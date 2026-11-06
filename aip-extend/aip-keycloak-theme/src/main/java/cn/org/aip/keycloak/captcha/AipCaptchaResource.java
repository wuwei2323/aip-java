package cn.org.aip.keycloak.captcha;

import com.anji.captcha.model.common.ResponseModel;
import com.anji.captcha.model.vo.CaptchaVO;
import com.anji.captcha.service.CaptchaService;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.managers.AuthenticationSessionManager;
import org.keycloak.services.managers.ClientSessionCode;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 绑定真实登录会话的同源滑块验证码接口。
 *
 * <p>挑战经分布式单次入口消费后才进入 AJ 坐标校验，成功后签发绑定相同认证
 * 上下文的一次性登录票据。</p>
 *
 * @author AIP
 */
@Produces(MediaType.APPLICATION_JSON)
public class AipCaptchaResource {

    private static final Logger LOG = Logger.getLogger(AipCaptchaResource.class);
    private static final Pattern TAB_ID_PATTERN = Pattern.compile("^[A-Za-z0-9._:-]{1,64}$");
    private static final int MAX_BODY_LENGTH = 4096;
    private static final int MAX_TOKEN_LENGTH = 128;
    private static final int MAX_POINT_JSON_LENGTH = 1024;
    private static final int MAX_CLIENT_ID_LENGTH = 128;
    private static final String RESULT_OK = "0000";
    private static final String CODE_COORDINATE_ERROR = "6111";

    private final KeycloakSession session;
    private final AuthenticationSessionManager authenticationSessions;
    private final AipCaptchaRateLimiter rateLimiter;

    public AipCaptchaResource(KeycloakSession session) {
        this(session, new AuthenticationSessionManager(session), AipCaptchaRateLimiter.INSTANCE);
    }

    AipCaptchaResource(KeycloakSession session, AuthenticationSessionManager authenticationSessions,
                       AipCaptchaRateLimiter rateLimiter) {
        this.session = session;
        this.authenticationSessions = authenticationSessions;
        this.rateLimiter = rateLimiter;
    }

    /** 获取挑战；只有来自真实且未过期认证会话的请求才能生成图片。 */
    @GET
    @Path("get")
    public Response get(@QueryParam("captchaType") String captchaType,
                        @QueryParam("tabId") String tabId,
                        @QueryParam("clientId") String clientId) {
        if (!AipCaptchaConstants.CAPTCHA_TYPE.equals(captchaType)
            || !validTabId(tabId) || !validClientId(clientId)) {
            return error(Response.Status.BAD_REQUEST, "captchaRequestInvalid");
        }
        boolean acquired = false;
        try {
            if (!rateLimiter.allowGetByIp(remoteAddress())) {
                return error(Response.Status.TOO_MANY_REQUESTS, "captchaRateLimited");
            }
            AipCaptchaAuthenticationContext context = authenticationContext(clientId, tabId);
            if (context == null) {
                return error(Response.Status.BAD_REQUEST, "captchaRequestInvalid");
            }
            if (!rateLimiter.allowGet(session, context)) {
                return error(Response.Status.TOO_MANY_REQUESTS, "captchaRateLimited");
            }
            acquired = AipCaptchaEngine.tryAcquireImagePermit();
            if (!acquired) {
                return error(Response.Status.TOO_MANY_REQUESTS, "captchaBusy");
            }
            CaptchaService service = AipCaptchaEngine.getService();
            if (service == null) {
                return error(Response.Status.SERVICE_UNAVAILABLE, "captchaUnavailable");
            }
            CaptchaVO request = new CaptchaVO();
            request.setCaptchaType(AipCaptchaConstants.CAPTCHA_TYPE);
            ResponseModel result = AipCaptchaSessionContext.call(session, () -> service.get(request));
            if (!RESULT_OK.equals(result.getRepCode())) {
                LOG.warnf("AJ-Captcha get failed, repCode=%s", result.getRepCode());
                return error(Response.Status.INTERNAL_SERVER_ERROR, "captchaUnavailable");
            }
            CaptchaVO challenge = (CaptchaVO) result.getRepData();
            AipCaptchaChallengeService.register(session, context, challenge.getToken());
            return ok(challenge);
        } catch (Exception e) {
            LOG.error("Failed to generate CAPTCHA", e);
            return error(Response.Status.INTERNAL_SERVER_ERROR, "captchaUnavailable");
        } finally {
            if (acquired) {
                AipCaptchaEngine.releaseImagePermit();
            }
        }
    }

    /** 原子消费挑战绑定，校验坐标后签发单次登录票据。 */
    @POST
    @Path("check")
    public Response check(String body,
                          @QueryParam("tabId") String tabId,
                          @QueryParam("clientId") String clientId) {
        if (!validTabId(tabId) || !validClientId(clientId) || body == null || body.length() > MAX_BODY_LENGTH) {
            return error(Response.Status.BAD_REQUEST, "captchaRequestInvalid");
        }
        try {
            if (!rateLimiter.allowCheckByIp(remoteAddress())) {
                return error(Response.Status.TOO_MANY_REQUESTS, "captchaRateLimited");
            }
            AipCaptchaAuthenticationContext context = authenticationContext(clientId, tabId);
            if (context == null) {
                return error(Response.Status.BAD_REQUEST, "captchaRequestInvalid");
            }
            if (!rateLimiter.allowCheck(session, context)) {
                return error(Response.Status.TOO_MANY_REQUESTS, "captchaRateLimited");
            }
            CheckRequest request;
            try {
                request = CheckRequest.parse(body);
            } catch (Exception e) {
                return error(Response.Status.BAD_REQUEST, "captchaRequestInvalid");
            }
            if (!AipCaptchaConstants.CAPTCHA_TYPE.equals(request.captchaType)
                || !validToken(request.token) || !validPointJson(request.pointJson)) {
                return error(Response.Status.BAD_REQUEST, "captchaRequestInvalid");
            }
            CaptchaService service = AipCaptchaEngine.getService();
            if (service == null) {
                return error(Response.Status.SERVICE_UNAVAILABLE, "captchaUnavailable");
            }
            if (!AipCaptchaChallengeService.consume(session, context, request.token)) {
                // 伪造/重放/跨会话挑战不进入引擎，也不计入其他用户的坐标失败锁。
                return error(Response.Status.BAD_REQUEST, "captchaExpired");
            }
            CaptchaVO captchaVO = new CaptchaVO();
            captchaVO.setCaptchaType(AipCaptchaConstants.CAPTCHA_TYPE);
            captchaVO.setToken(request.token);
            captchaVO.setPointJson(request.pointJson);
            ResponseModel result = AipCaptchaSessionContext.call(session, () -> service.check(captchaVO));
            if (!RESULT_OK.equals(result.getRepCode())) {
                rateLimiter.recordFailure(session, context);
                String messageKey = CODE_COORDINATE_ERROR.equals(result.getRepCode()) ? "captchaFail" : "captchaExpired";
                return error(Response.Status.BAD_REQUEST, messageKey);
            }
            String ticket = AipCaptchaTicketService.issue(session, context);
            return ok(Map.of("captchaVerification", ticket, "result", true));
        } catch (Exception e) {
            LOG.error("Failed to verify CAPTCHA", e);
            return error(Response.Status.INTERNAL_SERVER_ERROR, "captchaUnavailable");
        }
    }

    private AipCaptchaAuthenticationContext authenticationContext(String clientId, String tabId) {
        RealmModel realm = session.getContext().getRealm();
        if (realm == null || !realm.isEnabled()) {
            return null;
        }
        ClientModel client = realm.getClientByClientId(clientId);
        if (client == null || !client.isEnabled()) {
            return null;
        }
        // 复用 Keycloak 的 Cookie 校验/集群路由处理，不消费登录表单的 active_code。
        AuthenticationSessionModel auth = authenticationSessions.getCurrentAuthenticationSession(realm, client, tabId);
        AipCaptchaAuthenticationContext context = AipCaptchaAuthenticationContext.from(auth);
        if (context == null || !realm.getId().equals(context.realmId())
            || !clientId.equals(context.clientId()) || !tabId.equals(context.tabId())) {
            return null;
        }
        return new ClientSessionCode<>(session, realm, auth).isActionActive(ClientSessionCode.ActionType.LOGIN)
            ? context : null;
    }

    private Response ok(Object data) {
        Map<String, Object> envelope = new LinkedHashMap<>(3);
        envelope.put("code", 200);
        envelope.put("msg", "");
        envelope.put("data", data);
        return json(Response.Status.OK, envelope);
    }

    private Response error(Response.Status status, String messageKey) {
        Map<String, Object> envelope = new LinkedHashMap<>(3);
        envelope.put("code", status.getStatusCode());
        envelope.put("msg", messageKey);
        envelope.put("data", null);
        return json(status, envelope);
    }

    private Response json(Response.Status status, Map<String, Object> envelope) {
        String body;
        try {
            body = JsonSerialization.writeValueAsString(envelope);
        } catch (IOException e) {
            status = Response.Status.INTERNAL_SERVER_ERROR;
            body = "{\"code\":500,\"msg\":\"captchaUnavailable\",\"data\":null}";
        }
        return Response.status(status).type(MediaType.APPLICATION_JSON)
            .header("Cache-Control", "no-store, no-cache, must-revalidate")
            .header("Pragma", "no-cache").header("X-Content-Type-Options", "nosniff").entity(body).build();
    }

    private String remoteAddress() {
        return session.getContext().getConnection().getRemoteAddr();
    }

    private static boolean validTabId(String tabId) {
        return tabId != null && TAB_ID_PATTERN.matcher(tabId).matches();
    }

    private static boolean validClientId(String clientId) {
        return clientId != null && !clientId.isBlank() && clientId.length() <= MAX_CLIENT_ID_LENGTH;
    }

    private static boolean validToken(String token) {
        return token != null && !token.isBlank() && token.length() <= MAX_TOKEN_LENGTH;
    }

    private static boolean validPointJson(String pointJson) {
        return pointJson != null && !pointJson.isBlank() && pointJson.length() <= MAX_POINT_JSON_LENGTH;
    }

    /** AJ-Captcha 校验请求体，拒绝非字符串字段。 */
    static final class CheckRequest {
        private String captchaType;
        private String token;
        private String pointJson;

        static CheckRequest parse(String body) throws IOException {
            Map<?, ?> raw = JsonSerialization.readValue(body, Map.class);
            CheckRequest request = new CheckRequest();
            request.captchaType = asString(raw.get("captchaType"));
            request.token = asString(raw.get("token"));
            request.pointJson = asString(raw.get("pointJson"));
            return request;
        }

        private static String asString(Object value) {
            return value instanceof String text ? text : null;
        }
    }
}

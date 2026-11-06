package cn.org.aip.keycloak.captcha;

import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.authenticators.browser.UsernamePasswordForm;
import org.keycloak.events.Errors;
import org.keycloak.sessions.AuthenticationSessionModel;

/**
 * 在标准用户名密码表单提交前强制校验滑块验证码一次性票据。
 *
 * <p>表单渲染沿用 Keycloak 标准登录页；只有票据消费成功后才进入密码校验。
 * 直接构造 HTTP 请求跳过登录页时没有有效票据，因此无法绕过验证码。</p>
 *
 * <p>票据绑定 realmId、公开 clientId 与认证会话 tabId；比对时必须使用
 * {@code getClient().getClientId()}（公开 clientId），不能使用内部 UUID
 * {@code getClient().getId()}。</p>
 *
 * @author AIP
 */
public class AipCaptchaUsernamePasswordForm extends UsernamePasswordForm {

    private static final Logger LOG = Logger.getLogger(AipCaptchaUsernamePasswordForm.class);

    @Override
    public void action(AuthenticationFlowContext context) {
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();

        // 取消登录不要求验证码
        if (formData.containsKey("cancel")) {
            super.action(context);
            return;
        }

        AuthenticationSessionModel authSession = context.getAuthenticationSession();
        String realmId = authSession == null || authSession.getRealm() == null
            ? null : authSession.getRealm().getId();
        String clientId = authSession == null || authSession.getClient() == null
            ? null : authSession.getClient().getClientId();
        String tabId = authSession == null ? null : authSession.getTabId();
        String ticket = formData.getFirst(AipCaptchaConstants.FORM_FIELD);

        AipCaptchaAuthenticationContext binding = AipCaptchaAuthenticationContext.from(authSession);
        boolean verified = AipCaptchaTicketService.consume(context.getSession(), binding, ticket);

        if (!verified) {
            context.getEvent().detail("captcha", "invalid");
            context.getEvent().error(Errors.INVALID_USER_CREDENTIALS);
            LOG.warnf("CAPTCHA ticket rejected for realm=%s, client=%s, tab=%s",
                realmId, clientId, tabId);

            Response challenge = context.form()
                .setAttribute("usernameHidden", false)
                .setFormData(formData)
                .setError("captchaVerifyFail")
                .createLoginUsernamePassword();
            context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS, challenge);
            return;
        }

        super.action(context);
    }
}

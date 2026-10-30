package cn.org.aip.web.service;

import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nimbusds.jwt.JWTClaimsSet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import cn.org.aip.common.core.constant.Constants;
import cn.org.aip.common.core.constant.SystemConstants;
import cn.org.aip.common.core.constant.TenantConstants;
import cn.org.aip.common.core.domain.model.LoginUser;
import cn.org.aip.common.core.exception.ServiceException;
import cn.org.aip.common.core.utils.ServletUtils;
import cn.org.aip.common.core.utils.StringUtils;
import cn.org.aip.common.satoken.utils.LoginHelper;
import cn.org.aip.common.sso.config.KeycloakProperties;
import cn.org.aip.common.sso.core.KeycloakJwtVerifier;
import cn.org.aip.common.sso.core.KeycloakTokenClient;
import cn.org.aip.common.sso.core.SsoStateStore;
import cn.org.aip.common.tenant.helper.TenantHelper;
import cn.org.aip.system.domain.SysUser;
import cn.org.aip.system.domain.vo.SysClientVo;
import cn.org.aip.system.domain.vo.SysUserVo;
import cn.org.aip.system.mapper.SysUserMapper;
import cn.org.aip.system.service.ISysClientService;
import cn.org.aip.web.domain.bo.SsoChangePasswordBo;
import cn.org.aip.web.domain.vo.LoginVo;
import cn.org.aip.web.domain.vo.SsoAuthorizeVo;
import cn.org.aip.web.domain.vo.SsoLogoutVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * SSO 登录与 OIDC 流程业务处理
 *
 * @author Michelle.Chung
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SsoLoginService {

    public static final String DEFAULT_PC_CLIENT_ID = "e5cd7e4891bf95d1d19206ce24a7b32e";

    private final KeycloakProperties keycloakProperties;
    private final KeycloakJwtVerifier keycloakJwtVerifier;
    private final KeycloakTokenClient keycloakTokenClient;
    private final SsoStateStore ssoStateStore;
    private final PortalAppProvider portalAppProvider;
    private final SysLoginService sysLoginService;
    private final ISysClientService sysClientService;
    private final SysUserMapper userMapper;

    /**
     * 生成 Keycloak 授权登录 URL (支持 PKCE)
     * 固定使用门户 tenant 000000 的配置
     *
     * @param redirect 登录成功后的前端重定向相对路径
     * @return 授权地址响应
     */
    public SsoAuthorizeVo getAuthorizeUrl(String redirect) {
        String safeRedirect = sanitizeRedirect(redirect);
        PortalAppProvider.PortalAppConfig portalConfig = portalAppProvider.getPortalAppConfig();

        // 1. 生成 PKCE code_verifier (32 字节随机数) 与 code_challenge (S256)
        byte[] verifierBytes = RandomUtil.randomBytes(32);
        String codeVerifier = Base64.encodeUrlSafe(verifierBytes);
        byte[] digest = SecureUtil.sha256().digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
        String codeChallenge = Base64.encodeUrlSafe(digest);

        // 2. 生成 state 与 nonce
        String state = IdUtil.fastSimpleUUID();
        String nonce = IdUtil.fastSimpleUUID();

        // 3. 构建回调地址与前端地址
        String callbackUrl = resolveCallbackUrl();
        String frontendBaseUrl = resolveFrontendBaseUrl();

        // 存入 Redis (TTL 5分钟)
        SsoStateStore.StateData stateData = SsoStateStore.StateData.builder()
            .tenantId(portalConfig.getTenantId())
            .oidcClientId(portalConfig.getAppId())
            .redirect(safeRedirect)
            .nonce(nonce)
            .codeVerifier(codeVerifier)
            .callbackUrl(callbackUrl)
            .frontendBaseUrl(frontendBaseUrl)
            .createTime(System.currentTimeMillis())
            .build();
        ssoStateStore.saveState(state, stateData);

        // 4. 构建 Keycloak 授权 URL
        String normalizedScopes = normalizeScopes(portalConfig.getScopes());
        StringBuilder sb = new StringBuilder(keycloakProperties.getAuthUrl());
        sb.append("?response_type=code")
            .append("&client_id=").append(portalConfig.getAppId())
            .append("&redirect_uri=").append(URLUtil.encode(callbackUrl))
            .append("&scope=").append(URLUtil.encode(normalizedScopes))
            .append("&state=").append(state)
            .append("&nonce=").append(nonce)
            .append("&code_challenge=").append(codeChallenge)
            .append("&code_challenge_method=S256");

        return new SsoAuthorizeVo(sb.toString());
    }

    /**
     * 处理 Keycloak 回调
     */
    public void handleCallback(String code, String state, String error, String errorDescription, HttpServletResponse response) throws IOException {
        // 1. 优先尝试消费 state 获取当时发起授权的前端与回调地址
        SsoStateStore.StateData stateData = StringUtils.isNotBlank(state) ? ssoStateStore.consumeState(state) : null;
        String frontendBase = stateData != null && StringUtils.isNotBlank(stateData.getFrontendBaseUrl())
            ? stateData.getFrontendBaseUrl()
            : resolveFrontendBaseUrl();

        if (StringUtils.isNotBlank(error)) {
            log.warn("SSO callback received error from Keycloak: {} - {}", error, errorDescription);
            String target = frontendBase + "/login?error=" + URLUtil.encode(error);
            if (StringUtils.isNotBlank(errorDescription)) {
                target += "&error_description=" + URLUtil.encode(errorDescription);
            }
            response.sendRedirect(target);
            return;
        }

        if (StringUtils.isBlank(code) || StringUtils.isBlank(state)) {
            log.warn("SSO callback missing code or state");
            response.sendRedirect(frontendBase + "/login?error=invalid_request");
            return;
        }

        if (stateData == null) {
            log.warn("SSO state expired or invalid: {}", state);
            response.sendRedirect(frontendBase + "/login?error=state_expired");
            return;
        }

        PortalAppProvider.PortalAppConfig portalConfig = portalAppProvider.getPortalAppConfig();
        String callbackUrl = StringUtils.isNotBlank(stateData.getCallbackUrl())
            ? stateData.getCallbackUrl()
            : resolveCallbackUrl();

        // 2. 用 code + code_verifier 向 Keycloak 兑换 Token
        KeycloakTokenClient.OidcTokenResponse tokenResponse;
        try {
            tokenResponse = keycloakTokenClient.exchangeCode(
                code,
                stateData.getCodeVerifier(),
                callbackUrl,
                portalConfig.getAppId(),
                portalConfig.getClientSecret()
            );
        } catch (Exception e) {
            log.error("Failed to exchange code at Keycloak: {}", e.getMessage(), e);
            response.sendRedirect(frontendBase + "/login?error=token_exchange_failed");
            return;
        }

        // 3. 验签 ID Token
        JWTClaimsSet claims;
        try {
            claims = keycloakJwtVerifier.verifyToken(
                tokenResponse.getIdToken(),
                portalConfig.getAppId(),
                stateData.getNonce()
            );
        } catch (Exception e) {
            log.error("Failed to verify ID token: {}", e.getMessage(), e);
            response.sendRedirect(frontendBase + "/login?error=token_verification_failed");
            return;
        }

        // 4. 解析用户标识 (sub, employee_no, preferred_username, aip_user_id) 与会话标识 (sid)
        String sub = claims.getSubject();
        String preferredUsername = (String) claims.getClaim("preferred_username");
        String employeeNo = (String) claims.getClaim("employee_no");
        String aipUserId = (String) claims.getClaim("aip_user_id");
        String sid = (String) claims.getClaim("sid");

        SysUserVo user = findPlatformUser(sub, employeeNo, preferredUsername, aipUserId);
        if (user == null) {
            log.warn("Platform user not found for sub={}, employeeNo={}, username={}", sub, employeeNo, preferredUsername);
            response.sendRedirect(frontendBase + "/login?error=user_not_found");
            return;
        }

        if (SystemConstants.DISABLE.equals(user.getStatus())) {
            log.warn("Platform user is blocked: {}", user.getUserName());
            response.sendRedirect(frontendBase + "/login?error=user_blocked");
            return;
        }

        // 5. 检查首次登录强制改密
        if (Boolean.TRUE.equals(user.getPasswordResetRequired())) {
            String resetTicket = IdUtil.fastSimpleUUID();
            ssoStateStore.savePasswordResetTicket(
                resetTicket,
                new SsoStateStore.PasswordResetData(user.getUserId(), user.getUserName(), stateData.getTenantId(), stateData.getRedirect())
            );
            String resetPath = keycloakProperties.getFrontendChangePasswordPath();
            String redirectUrl = frontendBase + resetPath + "?ticket=" + resetTicket
                + "&redirect=" + URLUtil.encode(stateData.getRedirect());
            response.sendRedirect(redirectUrl);
            return;
        }

        // 6. 在 TenantHelper.dynamic 中构建 LoginUser 并签发 Sa-Token 会话
        LoginUser loginUser = TenantHelper.dynamic(stateData.getTenantId(), () -> sysLoginService.buildLoginUser(user));
        loginUser.setSsoSid(sid);
        loginUser.setSsoSub(sub);
        loginUser.setEmployeeNo(user.getEmployeeNo());
        loginUser.setKeycloakUserId(sub);

        SysClientVo client = sysClientService.queryByClientId(DEFAULT_PC_CLIENT_ID);
        SaLoginParameter model = new SaLoginParameter();
        if (client != null) {
            loginUser.setClientKey(client.getClientKey());
            loginUser.setDeviceType(client.getDeviceType());
            model.setDeviceType(client.getDeviceType());
            model.setTimeout(client.getTimeout());
            model.setActiveTimeout(client.getActiveTimeout());
            model.setExtra(LoginHelper.CLIENT_KEY, client.getClientId());
        } else {
            loginUser.setDeviceType("pc");
            model.setDeviceType("pc");
            model.setExtra(LoginHelper.CLIENT_KEY, DEFAULT_PC_CLIENT_ID);
        }

        LoginHelper.login(loginUser, model);
        String tokenValue = StpUtil.getTokenValue();

        // 7. 绑定 SSO 会话映射
        ssoStateStore.bindSession(sid, sub, tokenValue);

        // 8. 生成一次性换票 ticket (Redis 60秒) 并 302 到前端
        String ticket = IdUtil.fastSimpleUUID();
        ssoStateStore.saveTicket(ticket, SsoStateStore.TicketData.builder()
            .tokenValue(tokenValue)
            .loginId(loginUser.getLoginId())
            .redirect(stateData.getRedirect())
            .userId(user.getUserId())
            .tenantId(stateData.getTenantId())
            .clientId(client != null ? client.getClientId() : DEFAULT_PC_CLIENT_ID)
            .expireIn(StpUtil.getTokenTimeout())
            .build());

        String callbackPage = keycloakProperties.getFrontendCallbackPath();
        String targetUrl = frontendBase + callbackPage + "?ticket=" + ticket
            + "&redirect=" + URLUtil.encode(stateData.getRedirect());
        response.sendRedirect(targetUrl);
    }

    /**
     * 前端凭 ticket 兑换平台 Token (单次有效)
     */
    public LoginVo exchangeTicket(String ticket) {
        SsoStateStore.TicketData ticketData = ssoStateStore.consumeTicket(ticket);
        if (ticketData == null) {
            throw new ServiceException("兑换凭据已失效或不存在");
        }

        LoginVo loginVo = new LoginVo();
        loginVo.setAccessToken(ticketData.getTokenValue());
        loginVo.setExpireIn(ticketData.getExpireIn());
        loginVo.setClientId(ticketData.getClientId());
        return loginVo;
    }

    /**
     * 校验当前平台会话及与 Keycloak 会话映射
     */
    public boolean checkSession() {
        if (!StpUtil.isLogin()) {
            return false;
        }
        String tokenValue = StpUtil.getTokenValue();
        boolean valid = ssoStateStore.hasSidMapping(tokenValue);
        if (!valid) {
            log.info("Token {} sid mapping lost, logging out", tokenValue);
            try {
                StpUtil.logout();
            } catch (Exception ignored) {
            }
            return false;
        }
        return true;
    }

    /**
     * 平台登出并获取 Keycloak 登出 URL
     */
    public SsoLogoutVo logout() {
        String tokenValue = null;
        try {
            tokenValue = StpUtil.getTokenValue();
        } catch (Exception ignored) {
        }

        String sid = LoginHelper.getSsoSid();
        if (StringUtils.isNotBlank(sid)) {
            ssoStateStore.removeSid(sid);
        }

        sysLoginService.logout();

        PortalAppProvider.PortalAppConfig portalConfig = portalAppProvider.getPortalAppConfig();
        String frontendBase = keycloakProperties.getFrontendBaseUrl();
        String logoutUrl = keycloakProperties.getEndSessionUrl()
            + "?post_logout_redirect_uri=" + URLUtil.encode(frontendBase + "/login")
            + "&client_id=" + portalConfig.getAppId();

        return new SsoLogoutVo(logoutUrl);
    }

    /**
     * 处理 Keycloak Back-Channel Logout
     */
    public void handleBackChannelLogout(String logoutToken) {
        if (StringUtils.isBlank(logoutToken)) {
            throw new ServiceException("logout_token 不能为空");
        }

        PortalAppProvider.PortalAppConfig portalConfig = portalAppProvider.getPortalAppConfig();
        JWTClaimsSet claims = keycloakJwtVerifier.verifyLogoutToken(logoutToken, portalConfig.getAppId());

        String sid = (String) claims.getClaim("sid");
        String sub = claims.getSubject();

        log.info("Processing Back-Channel Logout for sid={}, sub={}", sid, sub);

        if (StringUtils.isNotBlank(sid)) {
            Collection<String> tokens = ssoStateStore.getTokensBySid(sid);
            for (String token : tokens) {
                try {
                    StpUtil.logoutByTokenValue(token);
                } catch (Exception e) {
                    log.warn("Failed to logout token {}: {}", token, e.getMessage());
                }
            }
            ssoStateStore.removeSid(sid);
        }

        if (StringUtils.isNotBlank(sub)) {
            Collection<String> tokens = ssoStateStore.getTokensBySub(sub);
            for (String token : tokens) {
                try {
                    StpUtil.logoutByTokenValue(token);
                } catch (Exception e) {
                    log.warn("Failed to logout token {}: {}", token, e.getMessage());
                }
            }
            ssoStateStore.removeSub(sub);
        }
    }

    /**
     * 修改密码（首次强制改密或普通改密）
     */
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(SsoChangePasswordBo bo) {
        if (!StringUtils.equals(bo.getNewPassword(), bo.getConfirmPassword())) {
            throw new ServiceException("两次输入的密码不一致");
        }

        if (StringUtils.isNotBlank(bo.getTicket())) {
            // 首次登录强制改密流程
            SsoStateStore.PasswordResetData data = ssoStateStore.getPasswordResetTicket(bo.getTicket());
            if (data == null) {
                throw new ServiceException("改密凭据已失效，请重新登录");
            }
            SysUser user = userMapper.selectById(data.getUserId());
            if (user == null) {
                throw new ServiceException("用户不存在");
            }
            if (BCrypt.checkpw(bo.getNewPassword(), user.getPassword())) {
                throw new ServiceException("新密码不能与原密码相同");
            }

            SysUser update = new SysUser();
            update.setUserId(user.getUserId());
            update.setPassword(BCrypt.hashpw(bo.getNewPassword()));
            update.setPasswordResetRequired(false);
            userMapper.updateById(update);

            ssoStateStore.removePasswordResetTicket(bo.getTicket());
            log.info("User {} completed mandatory password reset", user.getUserName());
        } else {
            // 登录状态下普通修改密码
            Long userId = LoginHelper.getUserId();
            if (userId == null) {
                throw new ServiceException("未登录或登录已失效");
            }
            if (StringUtils.isBlank(bo.getOldPassword())) {
                throw new ServiceException("旧密码不能为空");
            }
            SysUser user = userMapper.selectById(userId);
            if (user == null) {
                throw new ServiceException("用户不存在");
            }
            if (!BCrypt.checkpw(bo.getOldPassword(), user.getPassword())) {
                throw new ServiceException("原密码错误");
            }
            if (BCrypt.checkpw(bo.getNewPassword(), user.getPassword())) {
                throw new ServiceException("新密码不能与原密码相同");
            }

            SysUser update = new SysUser();
            update.setUserId(userId);
            update.setPassword(BCrypt.hashpw(bo.getNewPassword()));
            update.setPasswordResetRequired(false);
            userMapper.updateById(update);
        }
    }

    private SysUserVo findPlatformUser(String sub, String employeeNo, String preferredUsername, String aipUserId) {
        // 1. 优先按 keycloak_user_id 匹配
        if (StringUtils.isNotBlank(sub)) {
            SysUserVo user = userMapper.selectVoOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getKeycloakUserId, sub));
            if (user != null) {
                return user;
            }
        }

        SysUserVo user = null;

        // 2. 尝试按 aip_user_id 主键匹配
        if (StringUtils.isNotBlank(aipUserId)) {
            try {
                user = userMapper.selectVoById(Long.parseLong(aipUserId));
            } catch (Exception ignored) {
            }
        }

        // 3. 尝试按 Keycloak 联邦用户标识 (f:<componentId>:<externalId>) 提取平台主键匹配
        if (user == null && StringUtils.isNotBlank(sub) && sub.startsWith("f:")) {
            try {
                String externalId = sub.substring(sub.lastIndexOf(':') + 1);
                user = userMapper.selectVoById(Long.parseLong(externalId));
            } catch (Exception ignored) {
            }
        }

        // 4. 按工号 employee_no 匹配
        if (user == null && StringUtils.isNotBlank(employeeNo)) {
            user = userMapper.selectVoOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getEmployeeNo, employeeNo));
        }

        // 5. 按 preferred_username 匹配
        if (user == null && StringUtils.isNotBlank(preferredUsername)) {
            user = userMapper.selectVoOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUserName, preferredUsername));
        }

        // 6. 若 sub 即 username
        if (user == null && StringUtils.isNotBlank(sub)) {
            user = userMapper.selectVoOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUserName, sub));
        }

        // 自动回填绑定 keycloak_user_id
        if (user != null && StringUtils.isBlank(user.getKeycloakUserId()) && StringUtils.isNotBlank(sub)) {
            try {
                SysUser update = new SysUser();
                update.setUserId(user.getUserId());
                update.setKeycloakUserId(sub);
                userMapper.updateById(update);
                user.setKeycloakUserId(sub);
            } catch (Exception e) {
                log.warn("Failed to bind keycloak_user_id to user {}: {}", user.getUserName(), e.getMessage());
            }
        }

        return user;
    }

    public void handleCallback(String code, String state, HttpServletResponse response) throws IOException {
        handleCallback(code, state, null, null, response);
    }

    private String normalizeScopes(String scopes) {
        if (StringUtils.isBlank(scopes)) {
            return "openid profile email";
        }
        return Arrays.stream(scopes.split("[,\\s]+"))
            .filter(StringUtils::isNotBlank)
            .collect(Collectors.joining(" "));
    }

    private String resolveFrontendBaseUrl() {
        try {
            HttpServletRequest request = ServletUtils.getRequest();
            if (request != null) {
                String origin = request.getHeader("Origin");
                if (StringUtils.isNotBlank(origin) && (origin.startsWith("http://") || origin.startsWith("https://"))) {
                    return origin;
                }
                String referer = request.getHeader("Referer");
                if (StringUtils.isNotBlank(referer) && (referer.startsWith("http://") || referer.startsWith("https://"))) {
                    URI uri = URI.create(referer);
                    String portStr = uri.getPort() != -1 ? ":" + uri.getPort() : "";
                    return uri.getScheme() + "://" + uri.getHost() + portStr;
                }
            }
        } catch (Exception ignored) {
        }
        return keycloakProperties.getFrontendBaseUrl();
    }

    private String resolveCallbackUrl() {
        if (StringUtils.isNotBlank(keycloakProperties.getCallbackUrl())) {
            return keycloakProperties.getCallbackUrl();
        }
        try {
            HttpServletRequest request = ServletUtils.getRequest();
            if (request != null) {
                String origin = request.getHeader("Origin");
                if (StringUtils.isBlank(origin)) {
                    origin = request.getHeader("Referer");
                }
                if (StringUtils.isNotBlank(origin) && (origin.startsWith("http://") || origin.startsWith("https://"))) {
                    URI uri = URI.create(origin);
                    String host = uri.getHost();
                    if (StringUtils.isNotBlank(host) && !"localhost".equalsIgnoreCase(host) && !"127.0.0.1".equals(host)) {
                        return uri.getScheme() + "://" + host + ":8080/auth/sso/callback";
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return keycloakProperties.getBackendBaseUrl() + "/auth/sso/callback";
    }

    private String sanitizeRedirect(String redirect) {
        if (StringUtils.isBlank(redirect) || !redirect.startsWith("/") || redirect.startsWith("//")) {
            return "/";
        }
        return redirect;
    }

}

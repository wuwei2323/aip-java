package cn.org.aip.web.service;

import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nimbusds.jwt.JWTClaimsSet;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import cn.org.aip.common.core.constant.SystemConstants;
import cn.org.aip.common.core.domain.model.LoginUser;
import cn.org.aip.common.core.exception.ServiceException;
import cn.org.aip.common.core.utils.MessageUtils;
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
import cn.org.aip.web.domain.bo.SsoExchangeBo;
import cn.org.aip.web.domain.vo.LoginVo;
import cn.org.aip.web.domain.vo.SsoLogoutVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.Collection;

/**
 * SSO 登录与 OIDC 流程业务处理 (纯 SPA 前端路由落地 + 后端 JSON 换票)
 *
 * @author Michelle.Chung
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SsoLoginService {

    private final KeycloakProperties keycloakProperties;
    private final KeycloakJwtVerifier keycloakJwtVerifier;
    private final KeycloakTokenClient keycloakTokenClient;
    private final SsoStateStore ssoStateStore;
    private final PortalAppProvider portalAppProvider;
    private final SysLoginService sysLoginService;
    private final ISysClientService sysClientService;
    private final SysUserMapper userMapper;

    /**
     * 前端凭 Keycloak 授权码 code 兑换平台 Sa-Token 令牌 (强制校验 PKCE 与 Nonce)
     *
     * @param bo 换票请求对象
     * @return 平台登录令牌结果或强制改密响应
     */
    public LoginVo exchangeCode(SsoExchangeBo bo) {
        PortalAppProvider.PortalAppConfig portalConfig = portalAppProvider.getPortalAppConfig();

        // 1. 直连 Keycloak 换取 Token (透传 code, codeVerifier, redirectUri)
        KeycloakTokenClient.OidcTokenResponse tokenResponse;
        try {
            tokenResponse = keycloakTokenClient.exchangeCode(
                bo.getCode(),
                bo.getCodeVerifier(),
                bo.getRedirectUri(),
                portalConfig.getAppId(),
                portalConfig.getClientSecret()
            );
        } catch (ServiceException se) {
            throw se;
        } catch (Exception e) {
            log.error("Failed to exchange code at Keycloak: {}", e.getMessage(), e);
            throw new ServiceException(MessageUtils.message("sso.code.exchange.error", e.getMessage()));
        }

        // 2. 验签 ID Token (防重放 Nonce 严格校验)
        JWTClaimsSet claims;
        try {
            claims = keycloakJwtVerifier.verifyToken(
                tokenResponse.getIdToken(),
                portalConfig.getAppId(),
                bo.getNonce()
            );
        } catch (ServiceException se) {
            throw se;
        } catch (Exception e) {
            log.error("Failed to verify ID token: {}", e.getMessage(), e);
            throw new ServiceException(MessageUtils.message("sso.token.verify.error", e.getMessage()));
        }

        // 3. 解析用户身份
        String sub = claims.getSubject();
        String preferredUsername = (String) claims.getClaim("preferred_username");
        String employeeNo = (String) claims.getClaim("employee_no");
        String aipUserId = (String) claims.getClaim("aip_user_id");
        String sid = (String) claims.getClaim("sid");

        SysUserVo user = findPlatformUser(sub, employeeNo, preferredUsername, aipUserId);
        if (user == null) {
            log.warn("Platform user not found for sub={}, employeeNo={}, username={}", sub, employeeNo, preferredUsername);
            throw new ServiceException(MessageUtils.message("sso.user.not.found"));
        }

        if (SystemConstants.DISABLE.equals(user.getStatus())) {
            log.warn("Platform user is blocked: {}", user.getUserName());
            throw new ServiceException(MessageUtils.message("sso.user.blocked"));
        }

        String tenantId = portalConfig.getTenantId();

        // 4. 首次登录强制改密判断 (平滑返回改密凭据，不签发会话，销毁刚才建立的 Keycloak Session 防止死锁免密直通)
        if (Boolean.TRUE.equals(user.getPasswordResetRequired())) {
            if (StringUtils.isNotBlank(sid)) {
                try {
                    keycloakTokenClient.deleteSession(sid);
                } catch (Exception e) {
                    log.warn("Failed to terminate Keycloak session {} during mandatory password reset: {}", sid, e.getMessage());
                }
            }
            String resetTicket = IdUtil.fastSimpleUUID();
            ssoStateStore.savePasswordResetTicket(
                resetTicket,
                new SsoStateStore.PasswordResetData(user.getUserId(), user.getUserName(), tenantId)
            );
            LoginVo loginVo = new LoginVo();
            loginVo.setPasswordResetRequired(true);
            loginVo.setResetTicket(resetTicket);
            return loginVo;
        }

        // 5. 构建 LoginUser 并签发 Sa-Token 会话
        LoginUser loginUser = TenantHelper.dynamic(tenantId, () -> sysLoginService.buildLoginUser(user));
        loginUser.setSsoSid(sid);
        loginUser.setSsoSub(sub);
        loginUser.setEmployeeNo(user.getEmployeeNo());
        loginUser.setKeycloakUserId(sub);

        SysClientVo client = loginWithClient(loginUser);
        String tokenValue = StpUtil.getTokenValue();
        String effectiveClientId = client != null ? client.getClientId() : resolveClientId();

        // 暂存 Keycloak id_token，供退出登录时向 Keycloak 传递 id_token_hint 完成免交互静默登出
        try {
            StpUtil.getTokenSession().set("sso_id_token", tokenResponse.getIdToken());
        } catch (Exception ignored) {
        }

        // 6. 绑定 SSO 会话映射 (用于 Back-Channel Logout 与焦点探活)
        ssoStateStore.bindSession(sid, sub, tokenValue);

        LoginVo loginVo = new LoginVo();
        loginVo.setAccessToken(tokenValue);
        loginVo.setExpireIn(StpUtil.getTokenTimeout());
        loginVo.setClientId(effectiveClientId);
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
        LoginUser user = LoginHelper.getLoginUser();
        if (user != null && StringUtils.isBlank(user.getSsoSid()) && StringUtils.isBlank(user.getSsoSub())) {
            return true;
        }
        boolean valid = user != null && ssoStateStore.hasSidMapping(tokenValue);
        if (!valid) {
            log.info("SSO session mapping lost, logging out");
            try {
                StpUtil.logout();
            } catch (Exception ignored) {
            }
            return false;
        }
        return true;
    }

    /**
     * 平台登出并获取 Keycloak 登出 URL (服务端精准销毁会话 + 客户端免确认登出)
     *
     * @param redirectUri 登录注销后的前端回调页面地址 (支持前端动态指定)
     * @return 登出结果
     */
    public SsoLogoutVo logout(String redirectUri) {
        String sid = LoginHelper.getSsoSid();
        String idToken = null;
        try {
            idToken = (String) StpUtil.getTokenSession().get("sso_id_token");
        } catch (Exception ignored) {
        }

        // 1. 先注销本地平台当前 Sa-Token 会话（此时当前 Token 仍有效，否则 LoginHelper 取会话会抛异常）
        sysLoginService.logout();

        // 2. 清理该 sid 绑定的其余关联 Token 并清理映射
        if (StringUtils.isNotBlank(sid)) {
            Collection<String> tokens = ssoStateStore.getTokensBySid(sid);
            for (String token : tokens) {
                try {
                    StpUtil.logoutByTokenValue(token);
                } catch (Exception ignored) {
                }
            }
            ssoStateStore.removeSid(sid);

            // 3. 调用 Keycloak Admin API 销毁服务端会话 (容错隔离，绝不阻断本地登出主流程)
            try {
                keycloakTokenClient.deleteSession(sid);
            } catch (Exception e) {
                log.warn("Failed to delete Keycloak session {}: {}", sid, e.getMessage());
            }
        }

        // 4. 安全过滤并校验 post_logout_redirect_uri (防范开放重定向攻击)
        String targetRedirect = sanitizeLogoutRedirect(redirectUri);

        // 5. 构建标准 OIDC RP-Initiated Logout URL (带 id_token_hint 清除浏览器端 Keycloak Cookie)
        PortalAppProvider.PortalAppConfig portalConfig = portalAppProvider.getPortalAppConfig();
        StringBuilder sb = new StringBuilder(keycloakProperties.getEndSessionUrl());
        boolean hasParam = false;
        if (StringUtils.isNotBlank(idToken)) {
            sb.append("?id_token_hint=").append(URLUtil.encode(idToken));
            hasParam = true;
        } else {
            sb.append("?client_id=").append(portalConfig.getAppId());
            hasParam = true;
        }
        if (StringUtils.isNotBlank(targetRedirect)) {
            sb.append(hasParam ? "&" : "?").append("post_logout_redirect_uri=").append(URLUtil.encode(targetRedirect));
        }

        return new SsoLogoutVo(sb.toString());
    }

    /**
     * 默认无参登出
     */
    public SsoLogoutVo logout() {
        return logout(null);
    }

    /**
     * 处理 Keycloak Back-Channel Logout
     */
    public void handleBackChannelLogout(String logoutToken) {
        if (StringUtils.isBlank(logoutToken)) {
            throw new ServiceException(MessageUtils.message("sso.logout.token.not.blank"));
        }

        PortalAppProvider.PortalAppConfig portalConfig = portalAppProvider.getPortalAppConfig();
        JWTClaimsSet claims = keycloakJwtVerifier.verifyLogoutToken(logoutToken, portalConfig.getAppId());
        if (ssoStateStore.isLogoutProcessed(portalConfig.getAppId(), claims.getJWTID())) {
            return;
        }

        String sid = (String) claims.getClaim("sid");
        String sub = claims.getSubject();

        log.info("Processing Back-Channel Logout for sid={}, sub={}", sid, sub);

        if (StringUtils.isNotBlank(sid)) {
            Collection<String> tokens = ssoStateStore.getTokensBySid(sid);
            for (String token : tokens) {
                try {
                    StpUtil.logoutByTokenValue(token);
                } catch (Exception e) {
                    log.warn("Failed to invalidate a local SSO session: {}", e.getClass().getSimpleName());
                    throw e;
                }
            }
            ssoStateStore.removeSid(sid);
        } else if (StringUtils.isNotBlank(sub)) {
            Collection<String> tokens = ssoStateStore.getTokensBySub(sub);
            for (String token : tokens) {
                try {
                    StpUtil.logoutByTokenValue(token);
                } catch (Exception e) {
                    log.warn("Failed to invalidate a local SSO session: {}", e.getClass().getSimpleName());
                    throw e;
                }
            }
            ssoStateStore.removeSub(sub);
        }
        ssoStateStore.markLogoutProcessed(portalConfig.getAppId(), claims.getJWTID());
    }

    /**
     * 修改密码（首次强制改密或普通改密）
     */
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(SsoChangePasswordBo bo) {
        if (!StringUtils.equals(bo.getNewPassword(), bo.getConfirmPassword())) {
            throw new ServiceException(MessageUtils.message("sso.password.confirm.mismatch"));
        }

        if (StringUtils.isNotBlank(bo.getTicket())) {
            // 首次登录强制改密流程 (sys_user 表全局排除租户隔离，直接读写)
            SsoStateStore.PasswordResetData data = ssoStateStore.getPasswordResetTicket(bo.getTicket());
            if (data == null) {
                throw new ServiceException(MessageUtils.message("sso.ticket.expired"));
            }
            SysUser user = userMapper.selectById(data.getUserId());
            if (user == null) {
                throw new ServiceException(MessageUtils.message("sso.user.not.found"));
            }
            if (BCrypt.checkpw(bo.getNewPassword(), user.getPassword())) {
                throw new ServiceException(MessageUtils.message("sso.password.same.as.old"));
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
                throw new ServiceException(MessageUtils.message("sso.session.invalid"));
            }
            if (StringUtils.isBlank(bo.getOldPassword())) {
                throw new ServiceException(MessageUtils.message("sso.old.password.not.blank"));
            }
            SysUser user = userMapper.selectById(userId);
            if (user == null) {
                throw new ServiceException(MessageUtils.message("sso.user.not.found"));
            }
            if (!BCrypt.checkpw(bo.getOldPassword(), user.getPassword())) {
                throw new ServiceException(MessageUtils.message("sso.old.password.error"));
            }
            if (BCrypt.checkpw(bo.getNewPassword(), user.getPassword())) {
                throw new ServiceException(MessageUtils.message("sso.password.same.as.old"));
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

    /**
     * 解析当前调用的客户端标识（优先取请求头/请求参数中的 clientid，未提供时平滑回退至配置中的 defaultClientId）
     */
    private String resolveClientId() {
        try {
            HttpServletRequest request = ServletUtils.getRequest();
            if (request != null) {
                String headerCid = request.getHeader(LoginHelper.CLIENT_KEY);
                if (StringUtils.isNotBlank(headerCid)) {
                    return headerCid.trim();
                }
                String paramCid = ServletUtils.getParameter(LoginHelper.CLIENT_KEY);
                if (StringUtils.isNotBlank(paramCid)) {
                    return paramCid.trim();
                }
            }
        } catch (Exception ignored) {
        }
        return keycloakProperties.getDefaultClientId();
    }

    /**
     * 根据客户端配置建立 Sa-Token 登录会话
     */
    private SysClientVo loginWithClient(LoginUser loginUser) {
        String clientId = resolveClientId();
        SysClientVo client = StringUtils.isNotBlank(clientId) ? sysClientService.queryByClientId(clientId) : null;
        SaLoginParameter model = new SaLoginParameter();
        if (client != null) {
            loginUser.setClientKey(client.getClientKey());
            loginUser.setDeviceType(client.getDeviceType());
            model.setDeviceType(client.getDeviceType());
            model.setTimeout(client.getTimeout());
            model.setActiveTimeout(client.getActiveTimeout());
            model.setExtra(LoginHelper.CLIENT_KEY, client.getClientId());
        } else {
            String fallbackId = StringUtils.isNotBlank(clientId) ? clientId : "default";
            loginUser.setDeviceType("pc");
            model.setDeviceType("pc");
            model.setExtra(LoginHelper.CLIENT_KEY, fallbackId);
        }

        LoginHelper.login(loginUser, model);
        return client;
    }

    /**
     * 安全过滤并校验 post_logout_redirect_uri (防范开放重定向攻击)
     */
    private String sanitizeLogoutRedirect(String redirectUri) {
        if (StringUtils.isBlank(redirectUri)) {
            return resolveFrontendLogoutUrl();
        }
        String frontendBase = normalizeBaseUrl(keycloakProperties.getFrontendBaseUrl());
        // 允许相对路径
        if (redirectUri.startsWith("/") && !redirectUri.startsWith("//")) {
            return frontendBase + redirectUri;
        }
        // 允许配置的 frontendBaseUrl
        if (redirectUri.startsWith(frontendBase + "/") || redirectUri.equals(frontendBase)) {
            return redirectUri;
        }
        // 允许当前请求同源 Origin
        try {
            HttpServletRequest request = ServletUtils.getRequest();
            if (request != null) {
                String origin = request.getHeader("Origin");
                if (StringUtils.isBlank(origin)) {
                    origin = request.getHeader("Referer");
                }
                if (StringUtils.isNotBlank(origin) && (origin.startsWith("http://") || origin.startsWith("https://"))) {
                    URI uri = URI.create(origin);
                    String portStr = uri.getPort() != -1 ? ":" + uri.getPort() : "";
                    String originBase = uri.getScheme() + "://" + uri.getHost() + portStr;
                    if (redirectUri.startsWith(originBase + "/") || redirectUri.equals(originBase)) {
                        return redirectUri;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        log.warn("Invalid post_logout_redirect_uri: {}, fallback to default", redirectUri);
        return resolveFrontendLogoutUrl();
    }

    /**
     * 动态根据当前发起请求的前端 Origin / Referer 补齐登出回调地址
     */
    private String resolveFrontendLogoutUrl() {
        try {
            HttpServletRequest request = ServletUtils.getRequest();
            if (request != null) {
                String origin = request.getHeader("Origin");
                if (StringUtils.isBlank(origin)) {
                    origin = request.getHeader("Referer");
                }
                if (StringUtils.isNotBlank(origin) && (origin.startsWith("http://") || origin.startsWith("https://"))) {
                    URI uri = URI.create(origin);
                    String portStr = uri.getPort() != -1 ? ":" + uri.getPort() : "";
                    return uri.getScheme() + "://" + uri.getHost() + portStr + "/login?logout=true";
                }
            }
        } catch (Exception ignored) {
        }
        return normalizeBaseUrl(keycloakProperties.getFrontendBaseUrl()) + "/login?logout=true";
    }

    private String normalizeBaseUrl(String url) {
        if (StringUtils.isBlank(url)) {
            return "";
        }
        String trimmed = url.trim();
        return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }

}

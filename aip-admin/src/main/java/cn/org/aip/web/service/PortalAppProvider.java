package cn.org.aip.web.service;

import cn.hutool.crypto.SecureUtil;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import cn.org.aip.common.core.constant.TenantConstants;
import cn.org.aip.common.core.utils.StringUtils;
import cn.org.aip.common.sso.config.KeycloakProperties;
import cn.org.aip.system.domain.vo.SysApplicationVo;
import cn.org.aip.system.service.ISysApplicationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * 门户应用 OIDC 客户端配置提供者
 * 固定加载门户 tenant 000000 的 OIDC Client 配置；不解析域名/appId
 *
 * @author Michelle.Chung
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PortalAppProvider {

    private final KeycloakProperties keycloakProperties;
    private final ISysApplicationService applicationService;

    @Value("${keycloak.client-secret-key:aip-client-secret-aes-32byte-key}")
    private String aesKey;

    /**
     * 获取门户应用的 OIDC 配置
     */
    public PortalAppConfig getPortalAppConfig() {
        SysApplicationVo app = null;
        try {
            app = applicationService.queryByTenantId(TenantConstants.DEFAULT_TENANT_ID);
        } catch (Exception e) {
            log.warn("Failed to query portal application from DB, falling back to properties: {}", e.getMessage());
        }

        String appId = (app != null && StringUtils.isNotBlank(app.getAppId()))
            ? app.getAppId() : keycloakProperties.getClientId();
        String tenantId = TenantConstants.DEFAULT_TENANT_ID;

        String clientSecret = keycloakProperties.getClientSecret();
        if (app != null && StringUtils.isNotBlank(app.getClientSecret())) {
            clientSecret = app.getClientSecret();
        } else if (app != null) {
            // 尝试通过 applicationService 或直接解密
            try {
                // 如果库中有密文则解密
                // app.getClientSecret() 在普通查询中未暴露，若有特殊解密逻辑可以在此实现
            } catch (Exception ignored) {
            }
        }

        String scopes = (app != null && StringUtils.isNotBlank(app.getScopes()))
            ? app.getScopes() : "openid profile email";
        String requirePkce = (app != null && StringUtils.isNotBlank(app.getRequirePkce()))
            ? app.getRequirePkce() : "1";

        return PortalAppConfig.builder()
            .appId(appId)
            .tenantId(tenantId)
            .clientSecret(clientSecret)
            .scopes(scopes)
            .requirePkce(requirePkce)
            .build();
    }

    @Data
    @Builder
    public static class PortalAppConfig {
        private String appId;
        private String tenantId;
        private String clientSecret;
        private String scopes;
        private String requirePkce;
    }

}

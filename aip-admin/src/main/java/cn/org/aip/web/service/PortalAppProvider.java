package cn.org.aip.web.service;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import cn.org.aip.common.core.constant.TenantConstants;
import cn.org.aip.common.core.utils.StringUtils;
import cn.org.aip.common.sso.config.KeycloakProperties;
import cn.org.aip.common.sso.constant.KeycloakConstants;
import cn.org.aip.system.domain.vo.SysApplicationVo;
import cn.org.aip.system.service.ISysApplicationService;
import org.springframework.stereotype.Component;

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

    /**
     * 获取门户应用的 OIDC 配置
     */
    public PortalAppConfig getPortalAppConfig() {
        SysApplicationVo app = null;
        try {
            app = applicationService.queryByAppId(TenantConstants.DEFAULT_TENANT_ID);
        } catch (Exception e) {
            log.warn("Failed to query portal application from DB, falling back to properties: {}", e.getMessage());
        }

        String appId = (app != null && StringUtils.isNotBlank(app.getAppId()))
            ? app.getAppId() : keycloakProperties.getClientId();
        String tenantId = TenantConstants.DEFAULT_TENANT_ID;

        String clientSecret = keycloakProperties.getClientSecret();
        try {
            String dbSecret = applicationService.getDecryptedSecret(appId);
            if (StringUtils.isNotBlank(dbSecret)) {
                clientSecret = dbSecret;
            }
        } catch (Exception e) {
            log.warn("Failed to get decrypted secret for portal app: {}", e.getMessage());
        }

        return PortalAppConfig.builder()
            .appId(appId)
            .tenantId(tenantId)
            .clientSecret(clientSecret)
            .scopes(KeycloakConstants.DEFAULT_SCOPES)
            .requirePkce("1")
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

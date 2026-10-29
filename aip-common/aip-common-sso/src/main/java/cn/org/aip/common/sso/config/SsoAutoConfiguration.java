package cn.org.aip.common.sso.config;

import cn.org.aip.common.sso.core.KeycloakAdminClient;
import cn.org.aip.common.sso.core.KeycloakJwtVerifier;
import cn.org.aip.common.sso.core.KeycloakTokenClient;
import cn.org.aip.common.sso.core.SsoStateStore;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * SSO 自动配置
 *
 * @author Michelle.Chung
 */
@AutoConfiguration
@EnableConfigurationProperties(KeycloakProperties.class)
public class SsoAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "keycloak", name = "enabled", havingValue = "true", matchIfMissing = true)
    public KeycloakJwtVerifier keycloakJwtVerifier(KeycloakProperties properties) {
        return new KeycloakJwtVerifier(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public SsoStateStore ssoStateStore() {
        return new SsoStateStore();
    }

    @Bean
    @ConditionalOnMissingBean
    public KeycloakTokenClient keycloakTokenClient(KeycloakProperties properties) {
        return new KeycloakTokenClient(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public KeycloakAdminClient keycloakAdminClient(KeycloakProperties properties, KeycloakTokenClient tokenClient) {
        return new KeycloakAdminClient(properties, tokenClient);
    }

}

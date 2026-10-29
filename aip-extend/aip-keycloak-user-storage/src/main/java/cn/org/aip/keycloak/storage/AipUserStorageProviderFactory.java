package cn.org.aip.keycloak.storage;

import org.keycloak.component.ComponentModel;
import org.keycloak.component.ComponentValidationException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.storage.UserStorageProviderFactory;

import java.util.List;

/**
 * Keycloak AIP 用户存储 Provider Factory
 * Provider ID: aip-user-storage
 *
 * @author Michelle.Chung
 */
public class AipUserStorageProviderFactory implements UserStorageProviderFactory<AipUserStorageProvider> {

    public static final String PROVIDER_ID = "aip-user-storage";

    protected static final List<ProviderConfigProperty> CONFIG_PROPERTIES;

    static {
        CONFIG_PROPERTIES = ProviderConfigurationBuilder.create()
            .property()
            .name(AipUserStorageConfig.CONFIG_API_URL)
            .label("集成平台内部接口URL")
            .helpText("集成平台内部用户查询与密码校验接口基地址，如 http://localhost:8080 或 http://host.docker.internal:8080")
            .type(ProviderConfigProperty.STRING_TYPE)
            .defaultValue(AipUserStorageConfig.DEFAULT_API_URL)
            .add()
            .property()
            .name(AipUserStorageConfig.CONFIG_SHARED_SECRET)
            .label("内部通信共享密钥")
            .helpText("用于 /internal/keycloak/** 接口鉴权的共享密钥 (X-Internal-Secret)")
            .type(ProviderConfigProperty.PASSWORD)
            .defaultValue(AipUserStorageConfig.DEFAULT_SHARED_SECRET)
            .add()
            .property()
            .name(AipUserStorageConfig.CONFIG_CONNECT_TIMEOUT)
            .label("连接超时(毫秒)")
            .helpText("调用平台内部接口的连接超时时间")
            .type(ProviderConfigProperty.STRING_TYPE)
            .defaultValue(String.valueOf(AipUserStorageConfig.DEFAULT_CONNECT_TIMEOUT))
            .add()
            .property()
            .name(AipUserStorageConfig.CONFIG_READ_TIMEOUT)
            .label("读取超时(毫秒)")
            .helpText("调用平台内部接口的读取超时时间")
            .type(ProviderConfigProperty.STRING_TYPE)
            .defaultValue(String.valueOf(AipUserStorageConfig.DEFAULT_READ_TIMEOUT))
            .add()
            .build();
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public AipUserStorageProvider create(KeycloakSession session, ComponentModel model) {
        AipUserStorageConfig config = new AipUserStorageConfig(model);
        AipApiClient apiClient = new AipApiClient(config);
        return new AipUserStorageProvider(session, model, apiClient);
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return CONFIG_PROPERTIES;
    }

    @Override
    public void validateConfiguration(KeycloakSession session, RealmModel realm, ComponentModel config) throws ComponentValidationException {
        String apiUrl = config.get(AipUserStorageConfig.CONFIG_API_URL);
        if (apiUrl == null || apiUrl.trim().isEmpty()) {
            throw new ComponentValidationException("集成平台内部接口URL不能为空");
        }
        String sharedSecret = config.get(AipUserStorageConfig.CONFIG_SHARED_SECRET);
        if (sharedSecret == null || sharedSecret.trim().isEmpty()) {
            throw new ComponentValidationException("内部通信共享密钥不能为空");
        }
    }

}

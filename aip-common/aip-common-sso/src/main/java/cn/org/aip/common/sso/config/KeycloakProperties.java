package cn.org.aip.common.sso.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Keycloak OIDC 配置属性
 *
 * @author Michelle.Chung
 */
@Data
@ConfigurationProperties(prefix = "keycloak")
public class KeycloakProperties {

    /**
     * 是否启用 SSO
     */
    private boolean enabled = true;

    /**
     * Keycloak 服务端基础地址，如 http://localhost:8080
     */
    private String serverUrl = "http://localhost:8080";

    /**
     * 认证 Realm 标识
     */
    private String realm = "aip";

    /**
     * 门户应用 OIDC 客户端ID (固定为 000000)
     */
    private String clientId = "000000";

    /**
     * 默认平台终端客户端 ID (对应 sys_client 中的 client_id，用于会话超时与防重放策略)
     */
    private String defaultClientId = "e5cd7e4891bf95d1d19206ce24a7b32e";

    /**
     * 门户应用 OIDC 客户端密钥
     */
    private String clientSecret;

    /**
     * 后端回调地址，如 http://localhost:8080/auth/sso/callback
     */
    private String callbackUrl;

    /**
     * 前端基础地址，如 http://localhost:81
     */
    private String frontendBaseUrl = "http://localhost:81";

    /**
     * 后端基础地址，如 http://localhost:8080
     */
    private String backendBaseUrl = "http://localhost:8080";

    /**
     * 前端回调相对路径，默认 /sso/callback
     */
    private String frontendCallbackPath = "/sso/callback";

    /**
     * 前端改密相对路径，默认 /sso/change-password
     */
    private String frontendChangePasswordPath = "/sso/change-password";

    /**
     * JWT 时钟偏差容忍秒数，默认 60 秒
     */
    private long clockSkewSeconds = 60;

    /**
     * Keycloak 管理台客户端ID (Service Account, 授予 realm-management 的 manage-clients 角色)
     */
    private String adminClientId = "aip-admin-svc";

    /**
     * Keycloak 管理台客户端密钥
     */
    private String adminClientSecret;

    /**
     * 共享内部密钥，用于平台与 Keycloak User Storage SPI / 内部接口通信
     */
    private String internalSecret = "aip-sso-internal-secret-token";

    /** Connect timeout for token and admin requests, in milliseconds (1..30000). */
    private int connectTimeoutMillis = 3000;

    /** Read timeout for token and admin requests, in milliseconds (1..30000). */
    private int readTimeoutMillis = 5000;

    /** @return bounded timeout; zero and negative values must not disable the limit */
    public int getConnectTimeoutMillis() {
        return Math.max(1, Math.min(connectTimeoutMillis, 30000));
    }

    /** @return bounded timeout; zero and negative values must not disable the limit */
    public int getReadTimeoutMillis() {
        return Math.max(1, Math.min(readTimeoutMillis, 30000));
    }

    /**
     * 获取 OIDC 发行者地址 (iss)
     */
    public String getIssuer() {
        return normalizeUrl(serverUrl) + "/realms/" + realm;
    }

    /**
     * 获取授权码端点
     */
    public String getAuthUrl() {
        return getIssuer() + "/protocol/openid-connect/auth";
    }

    /**
     * 获取令牌兑换端点
     */
    public String getTokenUrl() {
        return getIssuer() + "/protocol/openid-connect/token";
    }

    /**
     * 获取 JWKS 公钥端点
     */
    public String getJwksUrl() {
        return getIssuer() + "/protocol/openid-connect/certs";
    }

    /**
     * 获取登出端点
     */
    public String getEndSessionUrl() {
        return getIssuer() + "/protocol/openid-connect/logout";
    }

    /**
     * 获取用户信息端点
     */
    public String getUserInfoUrl() {
        return getIssuer() + "/protocol/openid-connect/userinfo";
    }

    /**
     * 获取 Keycloak Admin REST API 基础路径
     */
    public String getAdminApiBaseUrl() {
        return normalizeUrl(serverUrl) + "/admin/realms/" + realm;
    }

    private String normalizeUrl(String url) {
        if (url == null) {
            return "";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

}

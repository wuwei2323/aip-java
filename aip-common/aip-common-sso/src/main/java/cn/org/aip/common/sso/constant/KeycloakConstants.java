package cn.org.aip.common.sso.constant;

/**
 * Keycloak OIDC 全局固定配置常量
 *
 * @author Michelle.Chung
 */
public interface KeycloakConstants {

    /**
     * 统一客户端认证方式：client_secret_basic
     */
    String CLIENT_AUTH_METHOD_BASIC = "client_secret_basic";

    /**
     * 默认固定授权范围
     */
    String DEFAULT_SCOPES = "openid profile email";

    /**
     * 默认固定授权类型
     */
    String DEFAULT_GRANT_TYPES = "authorization_code,refresh_token";

    /**
     * PKCE 校验算法：S256
     */
    String PKCE_METHOD_S256 = "S256";

    /**
     * Keycloak PKCE challenge method attribute
     */
    String ATTR_PKCE_METHOD = "pkce.code.challenge.method";

    /**
     * Keycloak Back-Channel Logout 属性
     */
    String ATTR_BACKCHANNEL_LOGOUT_SESSION_REQUIRED = "backchannel.logout.session.required";

    /**
     * Keycloak Back-Channel Logout 撤销离线令牌属性
     */
    String ATTR_BACKCHANNEL_LOGOUT_REVOKE_OFFLINE = "backchannel.logout.revoke.offline.tokens";

}

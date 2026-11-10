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

    /** Post-logout redirect URI allow-list attribute. */
    String ATTR_POST_LOGOUT_REDIRECT_URIS = "post.logout.redirect.uris";

    /** Explicit denial, rather than Keycloak's fallback to login redirect URIs. */
    String POST_LOGOUT_REDIRECT_DENY = "-";

    /** Back-channel logout endpoint attribute. */
    String ATTR_BACKCHANNEL_LOGOUT_URL = "backchannel.logout.url";

    /** Configuration has been saved and is waiting for its synchronous attempt. */
    String SYNC_PENDING = "PENDING";

    /** The latest configuration sync completed. */
    String SYNC_SUCCESS = "SUCCESS";

    /** The latest configuration sync failed or is paused. */
    String SYNC_FAILED = "FAILED";

    /**
     * Keycloak Back-Channel Logout 属性
     */
    String ATTR_BACKCHANNEL_LOGOUT_SESSION_REQUIRED = "backchannel.logout.session.required";

    /**
     * Keycloak Back-Channel Logout 撤销离线令牌属性
     */
    String ATTR_BACKCHANNEL_LOGOUT_REVOKE_OFFLINE = "backchannel.logout.revoke.offline.tokens";

}

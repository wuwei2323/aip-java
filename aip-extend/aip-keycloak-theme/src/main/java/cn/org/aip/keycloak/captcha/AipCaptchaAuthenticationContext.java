package cn.org.aip.keycloak.captcha;

import org.keycloak.sessions.AuthenticationSessionModel;

import java.util.Map;

/**
 * 由 Keycloak 真实认证会话生成的验证码绑定上下文，不信任请求中声明的会话归属。
 *
 * @param realmId Realm 内部标识
 * @param rootSessionId 根认证会话标识
 * @param clientId 公开客户端标识
 * @param tabId 认证标签页标识
 * @author AIP
 */
public record AipCaptchaAuthenticationContext(String realmId, String rootSessionId,
                                              String clientId, String tabId) {

    /** 从已验证的认证会话生成绑定；不完整的会话拒绝使用。 */
    static AipCaptchaAuthenticationContext from(AuthenticationSessionModel session) {
        if (session == null || session.getRealm() == null || session.getClient() == null
            || session.getParentSession() == null) {
            return null;
        }
        String realm = session.getRealm().getId();
        String root = session.getParentSession().getId();
        String client = session.getClient().getClientId();
        String tab = session.getTabId();
        if (blank(realm) || blank(root) || blank(client) || blank(tab)) {
            return null;
        }
        return new AipCaptchaAuthenticationContext(realm, root, client, tab);
    }

    Map<String, String> notes() {
        return Map.of("realm", realmId, "root", rootSessionId, "client", clientId, "tab", tabId);
    }

    boolean matches(Map<String, String> notes) {
        return notes != null && realmId.equals(notes.get("realm"))
            && rootSessionId.equals(notes.get("root")) && clientId.equals(notes.get("client"))
            && tabId.equals(notes.get("tab"));
    }

    /** 长度前缀避免 clientId 中的分隔符导致限流键碰撞。 */
    String rateLimitKey() {
        return part(realmId) + part(rootSessionId) + part(clientId) + part(tabId);
    }

    private static String part(String value) {
        return value.length() + ":" + value;
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}

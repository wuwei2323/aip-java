package cn.org.aip.keycloak.captcha;

import org.keycloak.models.KeycloakSession;

import java.util.UUID;

/**
 * 一次性登录票据服务。
 *
 * <p>票据绑定 realm、根认证会话、公开 clientId 和 tabId，使用分布式原子 remove
 * 保证单次消费。只有挑战校验成功后才可签发，不保留未使用的挑战审计字段。</p>
 *
 * @author AIP
 */
public final class AipCaptchaTicketService {

    /** 签发 180 秒有效的不透明登录票据。 */
    public static String issue(KeycloakSession session, AipCaptchaAuthenticationContext context) {
        String ticket = UUID.randomUUID().toString();
        session.singleUseObjects().put(AipCaptchaConstants.TICKET_PREFIX + ticket,
            AipCaptchaConstants.TICKET_TTL_SECONDS, context.notes());
        return ticket;
    }

    /** 原子消费并校验绑定；上下文不匹配的票据也不能再次使用。 */
    public static boolean consume(KeycloakSession session, AipCaptchaAuthenticationContext context, String ticket) {
        if (context == null || !isUuid(ticket)) {
            return false;
        }
        return context.matches(session.singleUseObjects().remove(AipCaptchaConstants.TICKET_PREFIX + ticket));
    }

    private static boolean isUuid(String value) {
        if (value == null || value.length() != 36) {
            return false;
        }
        try {
            UUID.fromString(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private AipCaptchaTicketService() {
    }
}

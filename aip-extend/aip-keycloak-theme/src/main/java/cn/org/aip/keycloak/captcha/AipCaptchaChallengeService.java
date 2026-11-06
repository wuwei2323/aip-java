package cn.org.aip.keycloak.captcha;

import org.keycloak.models.KeycloakSession;

/**
 * 在 AJ-Captcha 坐标校验之前原子消费挑战绑定，保证一个挑战最多进入引擎一次。
 *
 * <p>AJ-Captcha 的 exists/get/delete 并非原子操作；这个分布式单次入口阻止并发
 * 重复出票，同时保留上游图片生成、坐标算法和缓存接口契约。</p>
 *
 * @author AIP
 */
public final class AipCaptchaChallengeService {

    /** 仅在图片生成成功后登记挑战；登记失败不能将图片返回给客户端。 */
    public static void register(KeycloakSession session, AipCaptchaAuthenticationContext context, String token) {
        session.singleUseObjects().put(AipCaptchaConstants.CHALLENGE_PREFIX + token,
            AipCaptchaConstants.CHALLENGE_TTL_SECONDS, context.notes());
    }

    /** 无论绑定是否匹配都消费记录；重放、过期和跨会话请求不进入 AJ 引擎。 */
    public static boolean consume(KeycloakSession session, AipCaptchaAuthenticationContext context, String token) {
        return context.matches(session.singleUseObjects().remove(AipCaptchaConstants.CHALLENGE_PREFIX + token));
    }

    private AipCaptchaChallengeService() {
    }
}

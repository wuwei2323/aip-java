package cn.org.aip.keycloak.captcha;

import com.anji.captcha.service.CaptchaCacheService;
import org.keycloak.models.KeycloakSession;

import java.util.Collections;
import java.util.Map;

/**
 * AJ-Captcha 缓存适配器，底层复用 Keycloak 的分布式单次对象缓存。
 *
 * <p>挑战答案与一次性票据均写入 {@code SingleUseObjectProvider}，
 * 在 Keycloak 集群部署时由 Infinispan 分布式缓存保证跨节点可见。</p>
 *
 * @author AIP
 */
public class KeycloakCaptchaCacheService implements CaptchaCacheService {

    private static final String VALUE_KEY = "v";

    @Override
    public void set(String key, String value, long expiresInSeconds) {
        KeycloakSession session = AipCaptchaSessionContext.current();
        long lifespan = Math.max(expiresInSeconds, 1L);
        session.singleUseObjects().put(key, lifespan, Collections.singletonMap(VALUE_KEY, value));
    }

    @Override
    public boolean exists(String key) {
        return AipCaptchaSessionContext.current().singleUseObjects().contains(key);
    }

    @Override
    public void delete(String key) {
        AipCaptchaSessionContext.current().singleUseObjects().remove(key);
    }

    @Override
    public String get(String key) {
        Map<String, String> data = AipCaptchaSessionContext.current().singleUseObjects().get(key);
        return data == null ? null : data.get(VALUE_KEY);
    }

    @Override
    public String type() {
        return AipCaptchaConstants.CACHE_TYPE;
    }
}

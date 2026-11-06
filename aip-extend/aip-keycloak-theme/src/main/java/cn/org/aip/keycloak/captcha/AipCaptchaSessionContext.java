package cn.org.aip.keycloak.captcha;

import org.keycloak.models.KeycloakSession;

import java.util.function.Supplier;

/**
 * 将当前请求的 {@link KeycloakSession} 绑定到线程上下文。
 *
 * <p>AJ-Captcha 的 {@code CaptchaCacheService} 由 {@code ServiceLoader} 以单例方式创建，
 * 无法注入请求级会话，因此所有调用 AJ-Captcha 引擎的代码都必须通过
 * {@link #call(KeycloakSession, Supplier)} 包裹，确保缓存适配器可以取得当前会话。</p>
 *
 * @author AIP
 */
public final class AipCaptchaSessionContext {

    private static final ThreadLocal<KeycloakSession> CURRENT = new ThreadLocal<>();

    /**
     * 在绑定会话的上下文中执行验证码操作，结束后必定解绑，避免线程复用导致串会话。
     *
     * @param session 当前请求的 Keycloak 会话
     * @param action  验证码操作
     * @param <T>     返回值类型
     * @return 操作结果
     */
    public static <T> T call(KeycloakSession session, Supplier<T> action) {
        CURRENT.set(session);
        try {
            return action.get();
        } finally {
            CURRENT.remove();
        }
    }

    /**
     * 获取当前线程绑定的会话。
     *
     * @return Keycloak 会话
     */
    static KeycloakSession current() {
        KeycloakSession session = CURRENT.get();
        if (session == null) {
            throw new IllegalStateException("No Keycloak session bound to the current thread");
        }
        return session;
    }

    private AipCaptchaSessionContext() {
    }
}

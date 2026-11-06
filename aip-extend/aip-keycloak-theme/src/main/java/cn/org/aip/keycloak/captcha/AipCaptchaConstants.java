package cn.org.aip.keycloak.captcha;

/**
 * 滑块验证码在 Keycloak 内的统一常量。
 *
 * @author AIP
 */
public final class AipCaptchaConstants {

    /** RealmResourceProvider 的路径片段：/realms/{realm}/aip-captcha */
    public static final String PROVIDER_ID = "aip-captcha";

    /** 自定义登录表单 Authenticator 的 provider id */
    public static final String AUTHENTICATOR_ID = "aip-username-password-form";

    /** 仅启用 AJ-Captcha 的拼图滑块模式 */
    public static final String CAPTCHA_TYPE = "blockPuzzle";

    /** 与 KeycloakCaptchaCacheService#type() 保持一致 */
    public static final String CACHE_TYPE = "keycloak";

    /** 登录表单中承载一次性票据的字段名 */
    public static final String FORM_FIELD = "captchaVerification";

    /** 挑战的单次会话绑定前缀，与 AJ-Captcha 答案缓存隔离 */
    public static final String CHALLENGE_PREFIX = "AIP:CAPTCHA:CHALLENGE:";

    /** 与 AJ-Captcha 1.3.0 挑战有效期保持一致（秒） */
    public static final int CHALLENGE_TTL_SECONDS = 120;

    /** 一次性登录票据在 Keycloak 缓存中的前缀 */
    public static final String TICKET_PREFIX = "AIP:CAPTCHA:TICKET:";

    /** 票据有效期（秒），覆盖用户拖动与提交表单的时间 */
    public static final int TICKET_TTL_SECONDS = 180;

    /** AJ-Captcha 原生底图宽度 */
    public static final int NATIVE_WIDTH = 310;

    /** AJ-Captcha 原生拼图块宽度 */
    public static final int NATIVE_PIECE_WIDTH = 47;

    /** 滑块最大拖动距离（原生坐标） */
    public static final int NATIVE_MAX_OFFSET = NATIVE_WIDTH - NATIVE_PIECE_WIDTH;

    private AipCaptchaConstants() {
    }
}

package cn.org.aip.keycloak.captcha;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resource.RealmResourceProviderFactory;

/**
 * 将滑块验证码接口挂载到 {@code /realms/{realm}/aip-captcha}。
 *
 * <p>接口与登录页同源，因此不再需要跨域配置或外部服务地址。</p>
 *
 * @author AIP
 */
public class AipCaptchaResourceProviderFactory implements RealmResourceProviderFactory {

    @Override
    public RealmResourceProvider create(KeycloakSession session) {
        return new AipCaptchaResourceProvider(session);
    }

    @Override
    public void init(Config.Scope config) {
        // 无需外部配置
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // 启动时预加载图片并初始化 AJ-Captcha 引擎
        AipCaptchaEngine.initialize();
    }

    @Override
    public void close() {
        // 无资源需要释放
    }

    @Override
    public String getId() {
        return AipCaptchaConstants.PROVIDER_ID;
    }
}

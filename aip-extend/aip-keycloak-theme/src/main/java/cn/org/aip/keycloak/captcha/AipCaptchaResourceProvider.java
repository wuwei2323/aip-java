package cn.org.aip.keycloak.captcha;

import org.keycloak.models.KeycloakSession;
import org.keycloak.services.resource.RealmResourceProvider;

/**
 * 滑块验证码 RealmResourceProvider。
 *
 * @author AIP
 */
public class AipCaptchaResourceProvider implements RealmResourceProvider {

    private final KeycloakSession session;

    public AipCaptchaResourceProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public Object getResource() {
        return new AipCaptchaResource(session);
    }

    @Override
    public void close() {
        // 会话由 Keycloak 管理，无需在此关闭
    }
}

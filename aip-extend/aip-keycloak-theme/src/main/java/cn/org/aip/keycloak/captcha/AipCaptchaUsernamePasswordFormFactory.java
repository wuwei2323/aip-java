package cn.org.aip.keycloak.captcha;

import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.Collections;
import java.util.List;

/**
 * 滑块验证码登录表单 Authenticator 工厂。
 *
 * <p>在浏览器认证流中以 {@code aip-username-password-form} 替换标准
 * {@code auth-username-password-form} 执行项即可启用服务端强制校验。</p>
 *
 * @author AIP
 */
public class AipCaptchaUsernamePasswordFormFactory implements AuthenticatorFactory {

    private static final AuthenticationExecutionModel.Requirement[] REQUIREMENT_CHOICES = {
        AuthenticationExecutionModel.Requirement.REQUIRED,
        AuthenticationExecutionModel.Requirement.ALTERNATIVE,
        AuthenticationExecutionModel.Requirement.DISABLED
    };

    @Override
    public Authenticator create(KeycloakSession session) {
        return new AipCaptchaUsernamePasswordForm();
    }

    @Override
    public void init(Config.Scope config) {
        // 无需外部配置
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // 无需初始化
    }

    @Override
    public void close() {
        // 无资源需要释放
    }

    @Override
    public String getId() {
        return AipCaptchaConstants.AUTHENTICATOR_ID;
    }

    @Override
    public String getReferenceCategory() {
        return "password";
    }

    @Override
    public boolean isConfigurable() {
        return false;
    }

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return REQUIREMENT_CHOICES;
    }

    @Override
    public String getDisplayType() {
        return "AIP Username Password Form with CAPTCHA";
    }

    @Override
    public String getHelpText() {
        return "Validates the username and password only after a one-time CAPTCHA ticket has been consumed.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return Collections.emptyList();
    }

    @Override
    public boolean isUserSetupAllowed() {
        return false;
    }
}

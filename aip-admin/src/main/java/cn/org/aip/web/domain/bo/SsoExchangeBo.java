package cn.org.aip.web.domain.bo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * SSO 授权码换取平台令牌业务对象
 *
 * @author Michelle.Chung
 */
@Data
public class SsoExchangeBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Keycloak 授权码
     */
    @NotBlank(message = "{sso.code.not.blank}")
    private String code;

    /**
     * PKCE 校验凭据
     */
    @NotBlank(message = "{sso.code.verifier.not.blank}")
    private String codeVerifier;

    /**
     * 发起登录时的回调地址 (必须与 Keycloak 授权请求中的 redirect_uri 严格一致)
     */
    @NotBlank(message = "{sso.redirect.uri.not.blank}")
    private String redirectUri;

    /**
     * 防重放随机字符串 (必须与 Keycloak 授权请求中的 nonce 严格一致)
     */
    @NotBlank(message = "{sso.nonce.not.blank}")
    private String nonce;

}

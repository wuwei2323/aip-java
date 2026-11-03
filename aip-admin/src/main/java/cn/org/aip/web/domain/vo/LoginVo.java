package cn.org.aip.web.domain.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 登录验证信息
 *
 * @author Michelle.Chung
 */
@Data
public class LoginVo {

    /**
     * 授权令牌
     */
    @JsonProperty("access_token")
    private String accessToken;

    /**
     * 刷新令牌
     */
    @JsonProperty("refresh_token")
    private String refreshToken;

    /**
     * 授权令牌 access_token 的有效期
     */
    @JsonProperty("expire_in")
    private Long expireIn;

    /**
     * 刷新令牌 refresh_token 的有效期
     */
    @JsonProperty("refresh_expire_in")
    private Long refreshExpireIn;

    /**
     * 应用id
     */
    @JsonProperty("client_id")
    private String clientId;

    /**
     * 令牌权限
     */
    @JsonProperty("scope")
    private String scope;

    /**
     * 用户 openid
     */
    @JsonProperty("openid")
    private String openid;

    /**
     * 首次登录是否需要修改密码
     */
    @JsonProperty("password_reset_required")
    private Boolean passwordResetRequired;

    /**
     * 首次改密临时凭据
     */
    @JsonProperty("reset_ticket")
    private String resetTicket;

}

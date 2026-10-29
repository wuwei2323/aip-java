package cn.org.aip.web.domain.bo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * Keycloak User Storage SPI 密码校验请求对象
 *
 * @author Michelle.Chung
 */
@Data
public class KeycloakVerifyPasswordBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户账号或员工工号
     */
    @NotBlank(message = "用户名不能为空")
    private String username;

    /**
     * 明文密码
     */
    @NotBlank(message = "密码不能为空")
    private String password;

}

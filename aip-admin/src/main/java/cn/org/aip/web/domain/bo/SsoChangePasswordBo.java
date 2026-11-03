package cn.org.aip.web.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * SSO 修改密码/强制重置密码业务对象
 *
 * @author Michelle.Chung
 */
@Data
public class SsoChangePasswordBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 首次改密凭据 (从 /sso/change-password?ticket=... 获取)
     */
    private String ticket;

    /**
     * 旧密码 (普通登录后改密必填，使用ticket改密可为空)
     */
    private String oldPassword;

    /**
     * 新密码
     */
    @NotBlank(message = "{sso.new.password.not.blank}")
    @Size(min = 6, max = 32, message = "{sso.new.password.length.valid}")
    private String newPassword;

    /**
     * 确认新密码
     */
    @NotBlank(message = "{sso.confirm.password.not.blank}")
    private String confirmPassword;

}

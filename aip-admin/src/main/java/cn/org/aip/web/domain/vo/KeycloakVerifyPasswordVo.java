package cn.org.aip.web.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * Keycloak User Storage SPI 密码校验响应对象
 *
 * @author Michelle.Chung
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KeycloakVerifyPasswordVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 密码校验是否通过
     */
    private boolean valid;

    /**
     * 平台用户ID
     */
    private Long userId;

    /**
     * 平台登录账号
     */
    private String username;

    /**
     * 员工工号
     */
    private String employeeNo;

    /**
     * 用户状态 (0正常 1停用)
     */
    private String status;

    /**
     * 是否需要首次登录强制改密
     */
    private Boolean passwordResetRequired;

    /**
     * 提示信息 (如错误原因或锁定提示)
     */
    private String message;

}

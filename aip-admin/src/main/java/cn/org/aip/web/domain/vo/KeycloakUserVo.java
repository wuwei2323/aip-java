package cn.org.aip.web.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Keycloak SPI 用户视图模型
 *
 * @author Michelle.Chung
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KeycloakUserVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户唯一标识 (对应 Keycloak sub / UserModel id，平台 userId 字符串)
     */
    private String id;

    /**
     * Keycloak 登录主用户名 (工号优先，无工号则为平台账号)
     */
    private String username;

    /**
     * 平台登录账号
     */
    private String userName;

    /**
     * 员工工号
     */
    private String employeeNo;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 姓名/昵称
     */
    private String nickName;

    /**
     * 手机号码
     */
    private String phonenumber;

    /**
     * 账号是否启用
     */
    private boolean enabled;

    /**
     * 所属部门ID
     */
    private Long deptId;

    /**
     * 所属部门名称
     */
    private String deptName;

    /**
     * 是否需要首次登录强制改密
     */
    private Boolean passwordResetRequired;

    /**
     * 扩展用户声明属性 (映射为 Keycloak User Attribute)
     */
    @Builder.Default
    private Map<String, List<String>> attributes = new HashMap<>();

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

}

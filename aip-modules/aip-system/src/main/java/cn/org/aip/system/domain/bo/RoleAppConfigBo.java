package cn.org.aip.system.domain.bo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 角色在应用下的配置业务对象
 *
 * @author Lion Li
 */
@Data
public class RoleAppConfigBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 应用编号
     */
    @NotBlank(message = "应用编号不能为空")
    private String tenantId;

    /**
     * 数据范围（1：全部数据权限 2：自定数据权限 3：本部门数据权限 4：本部门及以下数据权限 5：仅本人数据权限 6：部门及以下或本人数据权限）
     */
    private String dataScope;

    /**
     * 菜单树选择项是否关联显示
     */
    private Boolean menuCheckStrictly;

    /**
     * 部门树选择项是否关联显示
     */
    private Boolean deptCheckStrictly;

    /**
     * 菜单组
     */
    private List<Long> menuIds;

    /**
     * 部门组（自定数据权限）
     */
    private List<Long> deptIds;

}

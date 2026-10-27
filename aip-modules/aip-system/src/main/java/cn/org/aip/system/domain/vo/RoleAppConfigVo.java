package cn.org.aip.system.domain.vo;

import cn.hutool.core.lang.tree.Tree;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 角色在应用下的配置视图对象
 *
 * @author Lion Li
 */
@Data
public class RoleAppConfigVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 应用编号
     */
    private String tenantId;

    /**
     * 应用名称
     */
    private String tenantName;

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
     * 该角色在该应用下已关联的菜单ID列表
     */
    private List<Long> menuIds;

    /**
     * 该角色在该应用下自定数据权限已关联的部门ID列表
     */
    private List<Long> deptIds;

    /**
     * 该应用下可用的菜单下拉树
     */
    private List<Tree<Long>> menuTree;

}

package cn.org.aip.system.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import cn.org.aip.common.tenant.helper.TenantHelper;
import cn.org.aip.common.core.constant.SystemConstants;
import cn.org.aip.common.core.utils.StreamUtils;
import cn.org.aip.common.core.utils.StringUtils;
import cn.org.aip.common.mybatis.core.mapper.BaseMapperPlus;
import cn.org.aip.system.domain.SysMenu;
import cn.org.aip.system.domain.vo.SysMenuVo;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 菜单表 数据层
 *
 * @author Lion Li
 */
public interface SysMenuMapper extends BaseMapperPlus<SysMenu, SysMenuVo> {

    /**
     * 构建用户权限菜单 SQL
     *
     * <p>
     * 查询用户所属角色所拥有的菜单权限，用于权限判断、菜单加载等场景
     * </p>
     *
     * @param userId 用户ID
     * @return SQL 字符串，用于 inSql 条件
     */
    default String buildMenuByUserSql(Long userId, String tenantId) {
        String tenantFilter = StringUtils.isNotBlank(tenantId)
            ? " and tenant_id = '" + tenantId + "' "
            : "";
        return """
                select menu_id from sys_role_menu where role_id in (
                    select sur.role_id from sys_user_role sur
                        left join sys_role sr on sr.role_id = sur.role_id
                        where sur.user_id = %d and sr.status = '0'
                ) %s
            """.formatted(userId, tenantFilter);
    }

    default String buildMenuByUserSql(Long userId) {
        return buildMenuByUserSql(userId, TenantHelper.getTenantId());
    }

    default String buildMenuByRoleSql(Long roleId, String tenantId) {
        String tenantFilter = StringUtils.isNotBlank(tenantId)
            ? " and srm.tenant_id = '" + tenantId + "' "
            : "";
        return """
                select srm.menu_id from sys_role_menu srm
                    left join sys_role sr on sr.role_id = srm.role_id
                    where srm.role_id = %d and sr.status = '0' %s
            """.formatted(roleId, tenantFilter);
    }

    default String buildMenuByRoleSql(Long roleId) {
        return buildMenuByRoleSql(roleId, TenantHelper.getTenantId());
    }

    default String buildParentMenuByRoleSql(Long roleId, String tenantId) {
        String tenantFilter = StringUtils.isNotBlank(tenantId)
            ? " and srm.tenant_id = '" + tenantId + "' "
            : "";
        return """
                select parent_id from sys_menu where menu_id in (
                    select srm.menu_id from sys_role_menu srm
                        left join sys_role sr on sr.role_id = srm.role_id
                        where srm.role_id = %d and sr.status = '0' %s
                )
            """.formatted(roleId, tenantFilter);
    }

    default String buildParentMenuByRoleSql(Long roleId) {
        return buildParentMenuByRoleSql(roleId, TenantHelper.getTenantId());
    }

    /**
     * 根据用户ID查询权限
     *
     * @param userId 用户ID
     * @return 权限列表
     */
    default Set<String> selectMenuPermsByUserId(Long userId) {
        List<String> list = this.selectObjs(
            new LambdaQueryWrapper<SysMenu>()
                .select(SysMenu::getPerms)
                .inSql(SysMenu::getMenuId, this.buildMenuByUserSql(userId))
                .isNotNull(SysMenu::getPerms)
        );
        return new HashSet<>(StreamUtils.filter(list, StringUtils::isNotBlank));
    }

    /**
     * 根据角色ID查询权限
     *
     * @param roleId 角色ID
     * @return 权限列表
     */
    default Set<String> selectMenuPermsByRoleId(Long roleId) {
        List<String> list = this.selectObjs(
            new LambdaQueryWrapper<SysMenu>()
                .select(SysMenu::getPerms)
                .inSql(SysMenu::getMenuId, this.buildMenuByRoleSql(roleId))
                .isNotNull(SysMenu::getPerms)
        );
        return new HashSet<>(StreamUtils.filter(list, StringUtils::isNotBlank));
    }

    /**
     * 根据用户ID查询菜单
     *
     * @return 菜单列表
     */
    default List<SysMenu> selectMenuTreeAll() {
        LambdaQueryWrapper<SysMenu> lqw = new LambdaQueryWrapper<SysMenu>()
            .in(SysMenu::getMenuType, SystemConstants.TYPE_DIR, SystemConstants.TYPE_MENU)
            .eq(SysMenu::getStatus, SystemConstants.NORMAL)
            .orderByAsc(SysMenu::getParentId)
            .orderByAsc(SysMenu::getOrderNum);
        return this.selectList(lqw);
    }

    /**
     * 根据角色ID查询菜单树信息
     *
     * @param roleId            角色ID
     * @param menuCheckStrictly 菜单树选择项是否关联显示
     * @return 选中菜单列表
     */
    default List<Long> selectMenuListByRoleId(Long roleId, String tenantId, boolean menuCheckStrictly) {
        LambdaQueryWrapper<SysMenu> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(SysMenu::getMenuId)
            .inSql(SysMenu::getMenuId, buildMenuByRoleSql(roleId, tenantId))
            .orderByAsc(SysMenu::getParentId)
            .orderByAsc(SysMenu::getOrderNum);
        if (menuCheckStrictly) {
            wrapper.notInSql(SysMenu::getMenuId, this.buildParentMenuByRoleSql(roleId, tenantId));
        }
        return this.selectObjs(wrapper);
    }

    default List<Long> selectMenuListByRoleId(Long roleId, boolean menuCheckStrictly) {
        return selectMenuListByRoleId(roleId, TenantHelper.getTenantId(), menuCheckStrictly);
    }

}

package cn.org.aip.system.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import cn.org.aip.common.core.utils.StreamUtils;
import cn.org.aip.common.core.utils.StringUtils;
import cn.org.aip.common.tenant.helper.TenantHelper;
import cn.org.aip.common.mybatis.annotation.DataColumn;
import cn.org.aip.common.mybatis.annotation.DataPermission;
import cn.org.aip.common.mybatis.core.mapper.BaseMapperPlus;
import cn.org.aip.common.mybatis.helper.DataBaseHelper;
import cn.org.aip.system.domain.SysDept;
import cn.org.aip.system.domain.vo.SysDeptVo;

import java.util.List;

/**
 * 部门管理 数据层
 *
 * @author Lion Li
 */
public interface SysDeptMapper extends BaseMapperPlus<SysDept, SysDeptVo> {

    /**
     * 构建角色对应的部门 SQL 查询语句
     *
     * <p>该 SQL 用于查询某个角色关联的所有部门 ID，常用于数据权限控制</p>
     *
     * @param roleId 角色ID
     * @return 查询部门ID的 SQL 语句字符串
     */
    default String buildDeptByRoleSql(Long roleId, String tenantId) {
        String tenantFilter = StringUtils.isNotBlank(tenantId)
            ? " and srd.tenant_id = '" + tenantId + "' "
            : "";
        return """
                select srd.dept_id from sys_role_dept srd
                    left join sys_role sr on sr.role_id = srd.role_id
                    where srd.role_id = %d and sr.status = '0' %s
            """.formatted(roleId, tenantFilter);
    }

    default String buildDeptByRoleSql(Long roleId) {
        return buildDeptByRoleSql(roleId, TenantHelper.getTenantId());
    }

    default String buildParentDeptByRoleSql(Long roleId, String tenantId) {
        String tenantFilter = StringUtils.isNotBlank(tenantId)
            ? " and srd.tenant_id = '" + tenantId + "' "
            : "";
        return """
                select parent_id from sys_dept where dept_id in (
                    select srd.dept_id from sys_role_dept srd
                        left join sys_role sr on sr.role_id = srd.role_id
                        where srd.role_id = %d and sr.status = '0' %s
                )
            """.formatted(roleId, tenantFilter);
    }

    default String buildParentDeptByRoleSql(Long roleId) {
        return buildParentDeptByRoleSql(roleId, TenantHelper.getTenantId());
    }

    /**
     * 查询部门管理数据
     *
     * @param queryWrapper 查询条件
     * @return 部门信息集合
     */
    @DataPermission({
        @DataColumn(key = "deptName", value = "dept_id")
    })
    default List<SysDeptVo> selectDeptList(Wrapper<SysDept> queryWrapper) {
        return this.selectVoList(queryWrapper);
    }

    /**
     * 分页查询部门管理数据
     *
     * @param page         分页信息
     * @param queryWrapper 查询条件
     * @return 部门信息集合
     */
    @DataPermission({
        @DataColumn(key = "deptName", value = "dept_id"),
    })
    default Page<SysDeptVo> selectPageDeptList(Page<SysDept> page, Wrapper<SysDept> queryWrapper) {
        return this.selectVoPage(page, queryWrapper);
    }

    /**
     * 统计指定部门ID的部门数量
     *
     * @param deptId 部门ID
     * @return 该部门ID的部门数量
     */
    @DataPermission({
        @DataColumn(key = "deptName", value = "dept_id")
    })
    default long countDeptById(Long deptId) {
        return this.selectCount(new LambdaQueryWrapper<SysDept>().eq(SysDept::getDeptId, deptId));
    }

    /**
     * 根据父部门ID查询其所有子部门的列表
     *
     * @param parentId 父部门ID
     * @return 包含子部门的列表
     */
    default List<SysDept> selectListByParentId(Long parentId) {
        return this.selectList(new LambdaQueryWrapper<SysDept>()
            .select(SysDept::getDeptId)
            .apply(DataBaseHelper.findInSet(parentId, "ancestors")));
    }

    /**
     * 查询某个部门及其所有子部门ID（含自身）
     *
     * @param parentId 父部门ID
     * @return 部门ID集合
     */
    default List<Long> selectDeptAndChildById(Long parentId) {
        List<SysDept> deptList = this.selectListByParentId(parentId);
        List<Long> deptIds = StreamUtils.toList(deptList, SysDept::getDeptId);
        deptIds.add(parentId);
        return deptIds;
    }

    /**
     * 根据角色ID查询部门树信息
     *
     * @param roleId            角色ID
     * @param deptCheckStrictly 部门树选择项是否关联显示
     * @return 选中部门列表
     */
    default List<Long> selectDeptListByRoleId(Long roleId, String tenantId, boolean deptCheckStrictly) {
        LambdaQueryWrapper<SysDept> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(SysDept::getDeptId)
            .inSql(SysDept::getDeptId, this.buildDeptByRoleSql(roleId, tenantId))
            .orderByAsc(SysDept::getParentId)
            .orderByAsc(SysDept::getOrderNum);
        if (deptCheckStrictly) {
            wrapper.notInSql(SysDept::getDeptId, this.buildParentDeptByRoleSql(roleId, tenantId));
        }
        return this.selectObjs(wrapper);
    }

    default List<Long> selectDeptListByRoleId(Long roleId, boolean deptCheckStrictly) {
        return selectDeptListByRoleId(roleId, TenantHelper.getTenantId(), deptCheckStrictly);
    }

}

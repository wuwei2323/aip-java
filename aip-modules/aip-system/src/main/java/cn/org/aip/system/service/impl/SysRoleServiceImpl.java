package cn.org.aip.system.service.impl;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import cn.org.aip.common.core.constant.CacheNames;
import cn.org.aip.common.core.constant.SystemConstants;
import cn.org.aip.common.core.constant.TenantConstants;
import cn.org.aip.common.core.domain.model.LoginUser;
import cn.org.aip.common.core.exception.ServiceException;
import cn.org.aip.common.core.service.RoleService;
import cn.org.aip.common.core.utils.MapstructUtils;
import cn.org.aip.common.core.utils.StreamUtils;
import cn.org.aip.common.core.utils.StringUtils;
import cn.org.aip.common.mybatis.core.page.PageQuery;
import cn.org.aip.common.mybatis.core.page.TableDataInfo;
import cn.org.aip.common.satoken.utils.LoginHelper;
import cn.org.aip.common.tenant.helper.TenantHelper;
import cn.hutool.core.lang.tree.Tree;
import cn.org.aip.common.redis.utils.CacheUtils;
import cn.org.aip.system.domain.SysRole;
import cn.org.aip.system.domain.SysRoleDataScope;
import cn.org.aip.system.domain.SysRoleDept;
import cn.org.aip.system.domain.SysRoleMenu;
import cn.org.aip.system.domain.SysTenant;
import cn.org.aip.system.domain.SysUserRole;
import cn.org.aip.system.domain.bo.RoleAppConfigBo;
import cn.org.aip.system.domain.bo.RoleDictValueBo;
import cn.org.aip.system.domain.bo.SysMenuBo;
import cn.org.aip.system.domain.bo.SysRoleBo;
import cn.org.aip.system.domain.vo.RoleAppConfigVo;
import cn.org.aip.system.domain.vo.SysMenuVo;
import cn.org.aip.system.domain.vo.SysRoleVo;
import cn.org.aip.system.mapper.SysRoleDataScopeMapper;
import cn.org.aip.system.mapper.SysRoleDeptMapper;
import cn.org.aip.system.mapper.SysRoleMapper;
import cn.org.aip.system.mapper.SysRoleMenuMapper;
import cn.org.aip.system.mapper.SysTenantMapper;
import cn.org.aip.system.mapper.SysUserRoleMapper;
import cn.org.aip.system.service.ISysDeptService;
import cn.org.aip.system.service.ISysMenuService;
import cn.org.aip.system.service.ISysRoleDictService;
import cn.org.aip.system.service.ISysRoleService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 角色 业务层处理
 *
 * @author Lion Li
 */
@RequiredArgsConstructor
@Service
public class SysRoleServiceImpl implements ISysRoleService, RoleService {

    private final SysRoleMapper baseMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleDeptMapper roleDeptMapper;
    private final SysRoleDataScopeMapper roleDataScopeMapper;
    private final SysTenantMapper tenantMapper;
    private final ISysMenuService menuService;
    private final ISysDeptService deptService;
    private final ISysRoleDictService roleDictService;

    /**
     * 分页查询角色列表
     *
     * @param role      查询条件
     * @param pageQuery 分页参数
     * @return 角色分页列表
     */
    @Override
    public TableDataInfo<SysRoleVo> selectPageRoleList(SysRoleBo role, PageQuery pageQuery) {
        Page<SysRoleVo> page = baseMapper.selectPageRoleList(pageQuery.build(), this.buildQueryWrapper(role));
        return TableDataInfo.build(page);
    }

    /**
     * 根据条件查询角色数据
     *
     * @param role 角色信息
     * @return 角色数据集合信息
     */
    @Override
    public List<SysRoleVo> selectRoleList(SysRoleBo role) {
        return baseMapper.selectRoleList(this.buildQueryWrapper(role));
    }

    private Wrapper<SysRole> buildQueryWrapper(SysRoleBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<SysRole> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(ObjectUtil.isNotNull(bo.getRoleId()), SysRole::getRoleId, bo.getRoleId())
            .like(StringUtils.isNotBlank(bo.getRoleName()), SysRole::getRoleName, bo.getRoleName())
            .eq(StringUtils.isNotBlank(bo.getStatus()), SysRole::getStatus, bo.getStatus())
            .like(StringUtils.isNotBlank(bo.getRoleKey()), SysRole::getRoleKey, bo.getRoleKey())
            .between(params.get("beginTime") != null && params.get("endTime") != null,
                SysRole::getCreateTime, params.get("beginTime"), params.get("endTime"))
            .orderByAsc(SysRole::getRoleSort).orderByAsc(SysRole::getCreateTime);
        return wrapper;
    }

    /**
     * 根据用户ID查询角色
     *
     * @param userId 用户ID
     * @return 角色列表
     */
    @Override
    public List<SysRoleVo> selectRolesByUserId(Long userId) {
        List<SysRoleVo> roles = baseMapper.selectRolesByUserId(userId);
        if (CollUtil.isNotEmpty(roles)) {
            String tenantId = TenantHelper.getTenantId();
            if (StringUtils.isNotBlank(tenantId)) {
                List<Long> roleIds = StreamUtils.toList(roles, SysRoleVo::getRoleId);
                List<SysRoleDataScope> scopes = roleDataScopeMapper.selectList(new LambdaQueryWrapper<SysRoleDataScope>()
                    .in(SysRoleDataScope::getRoleId, roleIds)
                    .eq(SysRoleDataScope::getTenantId, tenantId));
                Map<Long, String> scopeMap = StreamUtils.toMap(scopes, SysRoleDataScope::getRoleId, SysRoleDataScope::getDataScope);
                for (SysRoleVo role : roles) {
                    if (scopeMap.containsKey(role.getRoleId()) && StringUtils.isNotBlank(scopeMap.get(role.getRoleId()))) {
                        role.setDataScope(scopeMap.get(role.getRoleId()));
                    }
                }
            }
        }
        return roles;
    }

    /**
     * 根据用户ID查询角色列表(包含被授权状态)
     *
     * @param userId 用户ID
     * @return 角色列表
     */
    @Override
    public List<SysRoleVo> selectRolesAuthByUserId(Long userId) {
        List<SysRoleVo> userRoles = baseMapper.selectRolesByUserId(userId);
        List<SysRoleVo> roles = selectRoleAll();
        // 使用HashSet提高查找效率
        Set<Long> userRoleIds = StreamUtils.toSet(userRoles, SysRoleVo::getRoleId);
        for (SysRoleVo role : roles) {
            if (userRoleIds.contains(role.getRoleId())) {
                role.setFlag(true);
            }
        }
        return roles;
    }

    /**
     * 根据用户ID查询权限
     *
     * @param userId 用户ID
     * @return 权限列表
     */
    @Override
    public Set<String> selectRolePermissionByUserId(Long userId) {
        List<SysRoleVo> perms = baseMapper.selectRolesByUserId(userId);
        Set<String> permsSet = new HashSet<>();
        for (SysRoleVo perm : perms) {
            if (ObjectUtil.isNotNull(perm)) {
                permsSet.addAll(StringUtils.splitList(perm.getRoleKey().trim()));
            }
        }
        return permsSet;
    }

    /**
     * 查询所有角色
     *
     * @return 角色列表
     */
    @Override
    public List<SysRoleVo> selectRoleAll() {
        return this.selectRoleList(new SysRoleBo());
    }

    /**
     * 根据用户ID获取角色选择框列表
     *
     * @param userId 用户ID
     * @return 选中角色ID列表
     */
    @Override
    public List<Long> selectRoleListByUserId(Long userId) {
        List<SysRoleVo> list = baseMapper.selectRolesByUserId(userId);
        return StreamUtils.toList(list, SysRoleVo::getRoleId);
    }

    /**
     * 通过角色ID查询角色
     *
     * @param roleId 角色ID
     * @return 角色对象信息
     */
    @Override
    public SysRoleVo selectRoleById(Long roleId) {
        return baseMapper.selectRoleById(roleId);
    }

    /**
     * 通过角色ID串查询角色
     *
     * @param roleIds 角色ID串
     * @return 角色列表信息
     */
    @Override
    public List<SysRoleVo> selectRoleByIds(List<Long> roleIds) {
        return baseMapper.selectRoleList(new LambdaQueryWrapper<SysRole>()
            .eq(SysRole::getStatus, SystemConstants.NORMAL)
            .in(CollUtil.isNotEmpty(roleIds), SysRole::getRoleId, roleIds));
    }

    /**
     * 校验角色名称是否唯一
     *
     * @param role 角色信息
     * @return 结果
     */
    @Override
    public boolean checkRoleNameUnique(SysRoleBo role) {
        return TenantHelper.ignore(() -> {
            boolean exist = baseMapper.exists(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleName, role.getRoleName())
                .ne(ObjectUtil.isNotNull(role.getRoleId()), SysRole::getRoleId, role.getRoleId()));
            return !exist;
        });
    }

    /**
     * 校验角色权限是否唯一
     *
     * @param role 角色信息
     * @return 结果
     */
    @Override
    public boolean checkRoleKeyUnique(SysRoleBo role) {
        if (StringUtils.isBlank(role.getRoleKey())) {
            return true;
        }
        return TenantHelper.ignore(() -> {
            boolean exist = baseMapper.exists(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleKey, role.getRoleKey())
                .ne(ObjectUtil.isNotNull(role.getRoleId()), SysRole::getRoleId, role.getRoleId()));
            return !exist;
        });
    }

    /**
     * 校验角色是否允许操作
     *
     * @param role 角色信息
     */
    @Override
    public void checkRoleAllowed(SysRoleBo role) {
        if (ObjectUtil.isNotNull(role.getRoleId()) && LoginHelper.isSuperAdmin(role.getRoleId())) {
            throw new ServiceException("不允许操作超级管理员角色");
        }
        if (StringUtils.isBlank(role.getRoleKey())) {
            return;
        }
        String[] keys = new String[]{TenantConstants.SUPER_ADMIN_ROLE_KEY, TenantConstants.TENANT_ADMIN_ROLE_KEY};
        // 新增不允许使用 管理员标识符
        if (ObjectUtil.isNull(role.getRoleId())
            && StringUtils.equalsAny(role.getRoleKey(), keys)) {
            throw new ServiceException("不允许使用系统内置管理员角色标识符!");
        }
        // 修改不允许修改 管理员标识符
        if (ObjectUtil.isNotNull(role.getRoleId())) {
            SysRole sysRole = baseMapper.selectById(role.getRoleId());
            // 如果标识符不相等 判断为修改了管理员标识符
            if (sysRole != null && !StringUtils.equals(sysRole.getRoleKey(), role.getRoleKey())) {
                if (StringUtils.equalsAny(sysRole.getRoleKey(), keys)) {
                    throw new ServiceException("不允许修改系统内置管理员角色标识符!");
                } else if (StringUtils.equalsAny(role.getRoleKey(), keys)) {
                    throw new ServiceException("不允许使用系统内置管理员角色标识符!");
                }
            }
        }
    }

    /**
     * 校验角色是否有数据权限
     *
     * @param roleId 角色id
     */
    @Override
    public void checkRoleDataScope(Long roleId) {
        if (ObjectUtil.isNull(roleId)) {
            return;
        }
        this.checkRoleDataScope(Collections.singletonList(roleId));
    }

    /**
     * 校验角色是否有数据权限
     *
     * @param roleIds 角色ID列表（支持传单个ID）
     */
    @Override
    public void checkRoleDataScope(List<Long> roleIds) {
        if (CollUtil.isEmpty(roleIds) || LoginHelper.isSuperAdmin()) {
            return;
        }
        long count = baseMapper.selectRoleCount(roleIds);
        if (count != roleIds.size()) {
            throw new ServiceException("没有权限访问部分角色数据！");
        }
    }

    /**
     * 通过角色ID查询角色使用数量
     *
     * @param roleId 角色ID
     * @return 结果
     */
    @Override
    public long countUserRoleByRoleId(Long roleId) {
        return userRoleMapper.selectCount(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getRoleId, roleId));
    }

    /**
     * 新增保存角色信息
     *
     * @param bo 角色信息
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertRole(SysRoleBo bo) {
        if (StringUtils.isBlank(bo.getRoleKey())) {
            bo.setRoleKey("role_" + cn.hutool.core.util.IdUtil.fastSimpleUUID());
        }
        SysRole role = MapstructUtils.convert(bo, SysRole.class);
        // 新增角色信息
        baseMapper.insert(role);
        bo.setRoleId(role.getRoleId());

        if (CollUtil.isNotEmpty(bo.getAppConfigs())) {
            return saveRoleAppConfigs(role.getRoleId(), bo.getAppConfigs(), bo.getDictValues());
        }

        String tenantId = StringUtils.defaultIfBlank(bo.getTenantId(), TenantConstants.DEFAULT_TENANT_ID);
        // 初始化 sys_role_data_scope
        SysRoleDataScope rds = new SysRoleDataScope();
        rds.setRoleId(role.getRoleId());
        rds.setTenantId(tenantId);
        rds.setDataScope(StringUtils.defaultIfBlank(bo.getDataScope(), "1"));
        rds.setMenuCheckStrictly(bo.getMenuCheckStrictly() != null ? bo.getMenuCheckStrictly() : true);
        rds.setDeptCheckStrictly(bo.getDeptCheckStrictly() != null ? bo.getDeptCheckStrictly() : true);
        roleDataScopeMapper.insert(rds);

        if (bo.getDeptIds() != null) {
            insertRoleDept(bo);
        }
        if (bo.getRoleDictIds() != null || bo.getDictValues() != null) {
            roleDictService.saveRoleDictConfig(role.getRoleId(), bo.getRoleDictIds(), bo.getDictValues());
        }
        return insertRoleMenu(bo);
    }

    /**
     * 修改保存角色信息
     *
     * @param bo 角色信息
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateRole(SysRoleBo bo) {
        SysRole oldRole = baseMapper.selectById(bo.getRoleId());
        if (oldRole != null && StringUtils.isBlank(bo.getRoleKey())) {
            bo.setRoleKey(oldRole.getRoleKey());
        }
        SysRole role = MapstructUtils.convert(bo, SysRole.class);

        if (SystemConstants.DISABLE.equals(role.getStatus()) && this.countUserRoleByRoleId(role.getRoleId()) > 0) {
            throw new ServiceException("角色已分配，不能禁用!");
        }
        // 修改角色信息
        baseMapper.updateById(role);

        if (CollUtil.isNotEmpty(bo.getAppConfigs())) {
            return saveRoleAppConfigs(role.getRoleId(), bo.getAppConfigs(), bo.getDictValues());
        }

        String tenantId = StringUtils.defaultIfBlank(bo.getTenantId(), TenantConstants.DEFAULT_TENANT_ID);
        // 删除角色与菜单关联
        roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>()
            .eq(SysRoleMenu::getRoleId, role.getRoleId())
            .eq(SysRoleMenu::getTenantId, tenantId));
        if (bo.getDeptIds() != null) {
            roleDeptMapper.delete(new LambdaQueryWrapper<SysRoleDept>()
                .eq(SysRoleDept::getRoleId, role.getRoleId())
                .eq(SysRoleDept::getTenantId, tenantId));
            insertRoleDept(bo);
        }
        if (bo.getRoleDictIds() != null || bo.getDictValues() != null) {
            roleDictService.saveRoleDictConfig(role.getRoleId(), bo.getRoleDictIds(), bo.getDictValues());
        }
        return insertRoleMenu(bo);
    }

    /**
     * 修改角色状态
     *
     * @param roleId 角色ID
     * @param status 角色状态
     * @return 结果
     */
    @Override
    public int updateRoleStatus(Long roleId, String status) {
        if (SystemConstants.DISABLE.equals(status) && this.countUserRoleByRoleId(roleId) > 0) {
            throw new ServiceException("角色已分配，不能禁用!");
        }
        return baseMapper.update(null,
            new LambdaUpdateWrapper<SysRole>()
                .set(SysRole::getStatus, status)
                .eq(SysRole::getRoleId, roleId));
    }

    /**
     * 修改数据权限信息
     *
     * @param bo 角色信息
     * @return 结果
     */
    @CacheEvict(cacheNames = CacheNames.SYS_ROLE_CUSTOM, allEntries = true)
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int authDataScope(SysRoleBo bo) {
        if (CollUtil.isNotEmpty(bo.getAppConfigs())) {
            return saveRoleAppConfigs(bo.getRoleId(), bo.getAppConfigs(), bo.getDictValues());
        }
        SysRole role = MapstructUtils.convert(bo, SysRole.class);
        // 修改角色信息
        baseMapper.updateById(role);

        String tenantId = StringUtils.defaultIfBlank(bo.getTenantId(), TenantConstants.DEFAULT_TENANT_ID);
        SysRoleDataScope existing = roleDataScopeMapper.selectOne(new LambdaQueryWrapper<SysRoleDataScope>()
            .eq(SysRoleDataScope::getRoleId, bo.getRoleId())
            .eq(SysRoleDataScope::getTenantId, tenantId));
        String dataScope = StringUtils.defaultIfBlank(bo.getDataScope(), "1");
        boolean deptStrictly = bo.getDeptCheckStrictly() != null ? bo.getDeptCheckStrictly() : true;
        if (existing != null) {
            existing.setDataScope(dataScope);
            existing.setDeptCheckStrictly(deptStrictly);
            roleDataScopeMapper.updateById(existing);
        } else {
            SysRoleDataScope add = new SysRoleDataScope();
            add.setRoleId(bo.getRoleId());
            add.setTenantId(tenantId);
            add.setDataScope(dataScope);
            add.setDeptCheckStrictly(deptStrictly);
            add.setMenuCheckStrictly(bo.getMenuCheckStrictly() != null ? bo.getMenuCheckStrictly() : true);
            roleDataScopeMapper.insert(add);
        }

        // 删除角色与部门关联
        roleDeptMapper.delete(new LambdaQueryWrapper<SysRoleDept>()
            .eq(SysRoleDept::getRoleId, role.getRoleId())
            .eq(SysRoleDept::getTenantId, tenantId));
        // 保存角色自定义字典配置值（未传字典关联且未传配置值时不做变更）
        if (bo.getRoleDictIds() != null || bo.getDictValues() != null) {
            roleDictService.saveRoleDictConfig(role.getRoleId(), bo.getRoleDictIds(), bo.getDictValues());
        }
        // 新增角色和部门信息（数据权限）
        return insertRoleDept(bo);
    }

    /**
     * 新增角色菜单信息
     *
     * @param role 角色对象
     */
    private int insertRoleMenu(SysRoleBo role) {
        int rows = 1;
        String tenantId = StringUtils.defaultIfBlank(role.getTenantId(), TenantConstants.DEFAULT_TENANT_ID);
        List<SysRoleMenu> list = new ArrayList<>();
        if (role.getMenuIds() != null) {
            for (Long menuId : role.getMenuIds()) {
                SysRoleMenu rm = new SysRoleMenu();
                rm.setRoleId(role.getRoleId());
                rm.setMenuId(menuId);
                rm.setTenantId(tenantId);
                list.add(rm);
            }
        }
        if (CollUtil.isNotEmpty(list)) {
            rows = roleMenuMapper.insertBatch(list) ? list.size() : 0;
        }
        return rows;
    }

    /**
     * 新增角色部门信息(数据权限)
     *
     * @param role 角色对象
     */
    private int insertRoleDept(SysRoleBo role) {
        int rows = 1;
        String tenantId = StringUtils.defaultIfBlank(role.getTenantId(), TenantConstants.DEFAULT_TENANT_ID);
        List<SysRoleDept> list = new ArrayList<>();
        if (role.getDeptIds() != null) {
            for (Long deptId : role.getDeptIds()) {
                SysRoleDept rd = new SysRoleDept();
                rd.setRoleId(role.getRoleId());
                rd.setDeptId(deptId);
                rd.setTenantId(tenantId);
                list.add(rd);
            }
        }
        if (CollUtil.isNotEmpty(list)) {
            rows = roleDeptMapper.insertBatch(list) ? list.size() : 0;
        }
        return rows;
    }

    /**
     * 通过角色ID删除角色
     *
     * @param roleId 角色ID
     * @return 结果
     */
    @CacheEvict(cacheNames = CacheNames.SYS_ROLE_CUSTOM, allEntries = true)
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteRoleById(Long roleId) {
        // 删除角色与菜单关联
        roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, roleId));
        // 删除角色与部门关联
        roleDeptMapper.delete(new LambdaQueryWrapper<SysRoleDept>().eq(SysRoleDept::getRoleId, roleId));
        // 删除角色数据权限配置
        roleDataScopeMapper.delete(new LambdaQueryWrapper<SysRoleDataScope>().eq(SysRoleDataScope::getRoleId, roleId));
        // 删除角色自定义字典配置值与字典关联
        roleDictService.deleteByRoleIds(List.of(roleId));
        return baseMapper.deleteById(roleId);
    }

    /**
     * 批量删除角色信息
     *
     * @param roleIds 需要删除的角色ID
     * @return 结果
     */
    @CacheEvict(cacheNames = CacheNames.SYS_ROLE_CUSTOM, allEntries = true)
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteRoleByIds(List<Long> roleIds) {
        this.checkRoleDataScope(roleIds);
        List<SysRole> roles = baseMapper.selectByIds(roleIds);
        for (SysRole role : roles) {
            checkRoleAllowed(BeanUtil.toBean(role, SysRoleBo.class));
            if (countUserRoleByRoleId(role.getRoleId()) > 0) {
                throw new ServiceException(String.format("%1$s已分配，不能删除!", role.getRoleName()));
            }
        }
        // 删除角色与菜单关联
        roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().in(SysRoleMenu::getRoleId, roleIds));
        // 删除角色与部门关联
        roleDeptMapper.delete(new LambdaQueryWrapper<SysRoleDept>().in(SysRoleDept::getRoleId, roleIds));
        // 删除角色数据权限配置
        roleDataScopeMapper.delete(new LambdaQueryWrapper<SysRoleDataScope>().in(SysRoleDataScope::getRoleId, roleIds));
        // 删除角色自定义字典配置值与字典关联
        roleDictService.deleteByRoleIds(roleIds);
        return baseMapper.deleteByIds(roleIds);
    }

    /**
     * 取消授权用户角色
     *
     * @param userRole 用户和角色关联信息
     * @return 结果
     */
    @Override
    public int deleteAuthUser(SysUserRole userRole) {
        if (LoginHelper.getUserId().equals(userRole.getUserId())) {
            throw new ServiceException("不允许修改当前用户角色!");
        }
        int rows = userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>()
            .eq(SysUserRole::getRoleId, userRole.getRoleId())
            .eq(SysUserRole::getUserId, userRole.getUserId()));
        if (rows > 0) {
            cleanOnlineUser(List.of(userRole.getUserId()));
        }
        return rows;
    }

    /**
     * 批量取消授权用户角色
     *
     * @param roleId  角色ID
     * @param userIds 需要取消授权的用户数据ID
     * @return 结果
     */
    @Override
    public int deleteAuthUsers(Long roleId, Long[] userIds) {
        List<Long> ids = List.of(userIds);
        if (ids.contains(LoginHelper.getUserId())) {
            throw new ServiceException("不允许修改当前用户角色!");
        }
        int rows = userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>()
            .eq(SysUserRole::getRoleId, roleId)
            .in(SysUserRole::getUserId, ids));
        if (rows > 0) {
            cleanOnlineUser(ids);
        }
        return rows;
    }

    /**
     * 批量选择授权用户角色
     *
     * @param roleId  角色ID
     * @param userIds 需要授权的用户数据ID
     * @return 结果
     */
    @Override
    public int insertAuthUsers(Long roleId, Long[] userIds) {
        // 新增用户与角色管理
        int rows = 1;
        List<Long> ids = List.of(userIds);
        if (ids.contains(LoginHelper.getUserId())) {
            throw new ServiceException("不允许修改当前用户角色!");
        }
        List<SysUserRole> list = StreamUtils.toList(ids, userId -> {
            SysUserRole ur = new SysUserRole();
            ur.setUserId(userId);
            ur.setRoleId(roleId);
            return ur;
        });
        if (CollUtil.isNotEmpty(list)) {
            rows = userRoleMapper.insertBatch(list) ? list.size() : 0;
        }
        if (rows > 0) {
            cleanOnlineUser(ids);
        }
        return rows;
    }

    /**
     * 根据角色ID清除该角色关联的所有在线用户的登录状态（踢出在线用户）
     *
     * <p>
     * 先判断角色是否绑定用户，若无绑定则直接返回
     * 然后遍历当前所有在线Token，查找拥有该角色的用户并强制登出
     * 注意：在线用户量过大时，操作可能导致 Redis 阻塞，需谨慎调用
     * </p>
     *
     * @param roleId 角色ID
     */
    @Override
    public void cleanOnlineUserByRole(Long roleId) {
        // 如果角色未绑定用户 直接返回
        Long num = userRoleMapper.selectCount(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getRoleId, roleId));
        if (num == 0) {
            return;
        }
        List<String> keys = StpUtil.searchTokenValue("", 0, -1, false);
        if (CollUtil.isEmpty(keys)) {
            return;
        }
        // 角色关联的在线用户量过大会导致redis阻塞卡顿 谨慎操作
        keys.parallelStream().forEach(key -> {
            String token = StringUtils.substringAfterLast(key, ":");
            // 如果已经过期则跳过
            if (StpUtil.stpLogic.getTokenActiveTimeoutByToken(token) < -1) {
                return;
            }
            LoginUser loginUser = LoginHelper.getLoginUser(token);
            if (ObjectUtil.isNull(loginUser) || CollUtil.isEmpty(loginUser.getRoles())) {
                return;
            }
            if (loginUser.getRoles().stream().anyMatch(r -> r.getRoleId().equals(roleId))) {
                try {
                    StpUtil.logoutByTokenValue(token);
                } catch (NotLoginException ignored) {
                }
            }
        });
    }

    /**
     * 根据用户ID列表清除对应在线用户的登录状态（踢出指定用户）
     *
     * <p>
     * 遍历当前所有在线Token，匹配用户ID列表中的用户，强制登出
     * 注意：在线用户量过大时，操作可能导致 Redis 阻塞，需谨慎调用
     * </p>
     *
     * @param userIds 需要清除的用户ID列表
     */
    @Override
    public void cleanOnlineUser(List<Long> userIds) {
        List<String> keys = StpUtil.searchTokenValue("", 0, -1, false);
        if (CollUtil.isEmpty(keys)) {
            return;
        }
        // 角色关联的在线用户量过大会导致redis阻塞卡顿 谨慎操作
        keys.parallelStream().forEach(key -> {
            String token = StringUtils.substringAfterLast(key, ":");
            // 如果已经过期则跳过
            if (StpUtil.stpLogic.getTokenActiveTimeoutByToken(token) < -1) {
                return;
            }
            LoginUser loginUser = LoginHelper.getLoginUser(token);
            if (ObjectUtil.isNull(loginUser)) {
                return;
            }
            if (userIds.contains(loginUser.getUserId())) {
                try {
                    StpUtil.logoutByTokenValue(token);
                } catch (NotLoginException ignored) {
                }
            }
        });
    }

    /**
     * 根据角色 ID 列表查询角色名称映射关系
     *
     * @param roleIds 角色 ID 列表
     * @return Map，其中 key 为角色 ID，value 为对应的角色名称
     */
    @Override
    public Map<Long, String> selectRoleNamesByIds(List<Long> roleIds) {
        if (CollUtil.isEmpty(roleIds)) {
            return Collections.emptyMap();
        }
        List<SysRole> list = baseMapper.selectList(
            new LambdaQueryWrapper<SysRole>()
                .select(SysRole::getRoleId, SysRole::getRoleName)
                .in(SysRole::getRoleId, roleIds)
        );
        return StreamUtils.toMap(list, SysRole::getRoleId, SysRole::getRoleName);
    }

    /**
     * 查询角色在各应用下的配置列表（菜单、数据权限等）
     *
     * @param roleId 角色ID
     * @return 各应用配置列表
     */
    @Override
    public List<RoleAppConfigVo> selectRoleAppConfigs(Long roleId) {
        if (ObjectUtil.isNull(roleId)) {
            return new ArrayList<>();
        }
        List<SysTenant> allTenants = TenantHelper.ignore(() -> tenantMapper.selectList(
            new LambdaQueryWrapper<SysTenant>()
                .select(SysTenant::getTenantId, SysTenant::getCompanyName, SysTenant::getPackageId)
                .eq(SysTenant::getStatus, SystemConstants.NORMAL)
                .orderByAsc(SysTenant::getId)
        ));

        List<SysRoleDataScope> existingDataScopes = roleDataScopeMapper.selectList(
            new LambdaQueryWrapper<SysRoleDataScope>().eq(SysRoleDataScope::getRoleId, roleId)
        );
        Map<String, SysRoleDataScope> dataScopeMap = StreamUtils.toMap(existingDataScopes, SysRoleDataScope::getTenantId, java.util.function.Function.identity());
        SysRole role = baseMapper.selectById(roleId);

        // 获取全量菜单
        List<SysMenuVo> allMenus = menuService.selectMenuList(LoginHelper.getUserId());
        List<Tree<Long>> fullMenuTree = menuService.buildMenuTreeSelect(allMenus);

        List<RoleAppConfigVo> result = new ArrayList<>();
        for (SysTenant tenant : allTenants) {
            String tenantId = tenant.getTenantId();
            RoleAppConfigVo vo = new RoleAppConfigVo();
            vo.setTenantId(tenantId);
            vo.setTenantName(tenant.getCompanyName());

            SysRoleDataScope rds = dataScopeMap.get(tenantId);
            String dataScope = (rds != null && StringUtils.isNotBlank(rds.getDataScope()))
                ? rds.getDataScope()
                : (role != null && StringUtils.isNotBlank(role.getDataScope()) ? role.getDataScope() : "1");
            Boolean menuStrictly = (rds != null && rds.getMenuCheckStrictly() != null)
                ? rds.getMenuCheckStrictly()
                : (role != null && role.getMenuCheckStrictly() != null ? role.getMenuCheckStrictly() : Boolean.TRUE);
            Boolean deptStrictly = (rds != null && rds.getDeptCheckStrictly() != null)
                ? rds.getDeptCheckStrictly()
                : (role != null && role.getDeptCheckStrictly() != null ? role.getDeptCheckStrictly() : Boolean.TRUE);

            vo.setDataScope(dataScope);
            vo.setMenuCheckStrictly(menuStrictly);
            vo.setDeptCheckStrictly(deptStrictly);

            // 当前应用下的已关联菜单ID与部门ID
            vo.setMenuIds(menuService.selectMenuListByRoleId(roleId, tenantId));
            vo.setDeptIds(deptService.selectDeptListByRoleId(roleId, tenantId));

            // 当前应用下的可用菜单树
            if (tenant.getPackageId() != null && tenant.getPackageId() > 0) {
                List<Long> packageMenuIds = menuService.selectMenuListByPackageId(tenant.getPackageId());
                List<SysMenuVo> packageMenus = allMenus.stream()
                    .filter(m -> packageMenuIds.contains(m.getMenuId()))
                    .toList();
                vo.setMenuTree(menuService.buildMenuTreeSelect(packageMenus));
            } else {
                vo.setMenuTree(fullMenuTree);
            }
            result.add(vo);
        }
        return result;
    }

    /**
     * 保存角色在各应用下的配置
     *
     * @param roleId     角色ID
     * @param appConfigs 各应用配置列表
     * @param dictValues 角色自定义字典值列表（可选）
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int saveRoleAppConfigs(Long roleId, List<RoleAppConfigBo> appConfigs, List<RoleDictValueBo> dictValues) {
        SysRole role = baseMapper.selectById(roleId);
        if (role == null) {
            throw new ServiceException("角色不存在");
        }
        if (CollUtil.isNotEmpty(appConfigs)) {
            for (RoleAppConfigBo cfg : appConfigs) {
                String tenantId = cfg.getTenantId();
                if (StringUtils.isBlank(tenantId)) {
                    continue;
                }

                // 1. 保存 sys_role_data_scope
                SysRoleDataScope existing = roleDataScopeMapper.selectOne(
                    new LambdaQueryWrapper<SysRoleDataScope>()
                        .eq(SysRoleDataScope::getRoleId, roleId)
                        .eq(SysRoleDataScope::getTenantId, tenantId)
                );
                String dataScope = StringUtils.defaultIfBlank(cfg.getDataScope(), "1");
                boolean menuStrictly = cfg.getMenuCheckStrictly() != null ? cfg.getMenuCheckStrictly() : true;
                boolean deptStrictly = cfg.getDeptCheckStrictly() != null ? cfg.getDeptCheckStrictly() : true;
                if (existing != null) {
                    existing.setDataScope(dataScope);
                    existing.setMenuCheckStrictly(menuStrictly);
                    existing.setDeptCheckStrictly(deptStrictly);
                    roleDataScopeMapper.updateById(existing);
                } else {
                    SysRoleDataScope add = new SysRoleDataScope();
                    add.setRoleId(roleId);
                    add.setTenantId(tenantId);
                    add.setDataScope(dataScope);
                    add.setMenuCheckStrictly(menuStrictly);
                    add.setDeptCheckStrictly(deptStrictly);
                    roleDataScopeMapper.insert(add);
                }

                // 2. 默认应用同步更新主表 sys_role
                if (TenantConstants.DEFAULT_TENANT_ID.equals(tenantId)) {
                    role.setDataScope(dataScope);
                    role.setMenuCheckStrictly(menuStrictly);
                    role.setDeptCheckStrictly(deptStrictly);
                    baseMapper.updateById(role);
                }

                // 3. 保存菜单关联 sys_role_menu
                if (cfg.getMenuIds() != null) {
                    roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>()
                        .eq(SysRoleMenu::getRoleId, roleId)
                        .eq(SysRoleMenu::getTenantId, tenantId));
                    List<SysRoleMenu> menuList = new ArrayList<>();
                    for (Long menuId : cfg.getMenuIds()) {
                        SysRoleMenu rm = new SysRoleMenu();
                        rm.setRoleId(roleId);
                        rm.setMenuId(menuId);
                        rm.setTenantId(tenantId);
                        menuList.add(rm);
                    }
                    if (CollUtil.isNotEmpty(menuList)) {
                        roleMenuMapper.insertBatch(menuList);
                    }
                }

                // 4. 保存部门关联 sys_role_dept
                if (cfg.getDeptIds() != null) {
                    roleDeptMapper.delete(new LambdaQueryWrapper<SysRoleDept>()
                        .eq(SysRoleDept::getRoleId, roleId)
                        .eq(SysRoleDept::getTenantId, tenantId));
                    List<SysRoleDept> deptList = new ArrayList<>();
                    for (Long deptId : cfg.getDeptIds()) {
                        SysRoleDept rd = new SysRoleDept();
                        rd.setRoleId(roleId);
                        rd.setDeptId(deptId);
                        rd.setTenantId(tenantId);
                        deptList.add(rd);
                    }
                    if (CollUtil.isNotEmpty(deptList)) {
                        roleDeptMapper.insertBatch(deptList);
                    }
                }
            }
        }

        // 5. 保存字典配置
        if (dictValues != null) {
            roleDictService.saveRoleDictConfig(roleId, null, dictValues);
        }

        // 6. 清理缓存与在线用户
        CacheUtils.clear(CacheNames.SYS_ROLE_CUSTOM);
        cleanOnlineUserByRole(roleId);
        return 1;
    }

}

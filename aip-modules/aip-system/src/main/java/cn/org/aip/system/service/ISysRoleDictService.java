package cn.org.aip.system.service;

import cn.org.aip.system.domain.bo.RoleDictValueBo;
import cn.org.aip.system.domain.vo.RoleDictConfigVo;

import java.util.List;

/**
 * 角色字典配置 业务层
 *
 * @author Lion Li
 */
public interface ISysRoleDictService {

    /**
     * 查询角色关联的角色类字典配置（按应用分组）
     *
     * @param roleId 角色ID
     * @return 按应用分组的字典配置列表
     */
    List<RoleDictConfigVo> selectRoleDictConfig(Long roleId);

    /**
     * 保存角色关联字典与字典配置值（按角色全量替换）
     *
     * @param roleId      角色ID
     * @param roleDictIds 角色关联的字典ID列表
     * @param values      配置值列表
     */
    void saveRoleDictConfig(Long roleId, List<Long> roleDictIds, List<RoleDictValueBo> values);

    /**
     * 保存角色字典配置值（按角色全量替换）
     *
     * @param roleId 角色ID
     * @param values 配置值列表
     */
    void saveRoleDictValues(Long roleId, List<RoleDictValueBo> values);

    /**
     * 删除字典相关的角色配置值与角色关联
     *
     * @param dictIds 字典ID列表
     */
    void deleteByDictIds(List<Long> dictIds);

    /**
     * 删除字典在指定应用下的角色配置值
     *
     * @param dictId    字典ID
     * @param tenantIds 应用编号列表
     */
    void deleteByDictIdAndTenantIds(Long dictId, List<String> tenantIds);

    /**
     * 删除字典在指定角色下的配置值
     *
     * @param dictId  字典ID
     * @param roleIds 角色ID列表
     */
    void deleteByDictIdAndRoleIds(Long dictId, List<Long> roleIds);

    /**
     * 删除角色相关的配置值与字典关联
     *
     * @param roleIds 角色ID列表
     */
    void deleteByRoleIds(List<Long> roleIds);

}

package cn.org.aip.system.mapper;

import org.apache.ibatis.annotations.Param;
import cn.org.aip.common.mybatis.core.mapper.BaseMapperPlus;
import cn.org.aip.system.domain.SysRoleDictValue;
import cn.org.aip.system.domain.vo.RoleDictConfigItemVo;

import java.util.List;

/**
 * 角色字典配置值 数据层
 *
 * @author Lion Li
 */
public interface SysRoleDictValueMapper extends BaseMapperPlus<SysRoleDictValue, SysRoleDictValue> {

    /**
     * 查询所有可选的角色类字典（按应用、字典排序）
     *
     * @param tenantId 应用编号，为空时查询全部应用
     * @return 角色类字典列表
     */
    List<RoleDictConfigItemVo> selectAllRoleDicts(@Param("tenantId") String tenantId);

    /**
     * 查询角色关联的角色类字典及当前配置值（按应用、字典排序）
     *
     * @param roleId   角色ID
     * @param tenantId 应用编号，为空时查询全部应用
     * @return 字典配置项列表
     */
    List<RoleDictConfigItemVo> selectRoleDictConfig(@Param("roleId") Long roleId, @Param("tenantId") String tenantId);

}

package cn.org.aip.common.core.service;

import java.util.Map;

/**
 * 通用 角色字典服务
 * <p>
 * 用于业务模块读取角色在指定应用下的自定义字典配置值
 *
 * @author Lion Li
 */
public interface RoleDictService {

    /**
     * 获取角色在指定应用下某个字典的配置值
     *
     * @param roleId   角色ID
     * @param tenantId 应用编号
     * @param dictType 字典类型
     * @return 配置值，不存在时返回 null
     */
    String getRoleDictValue(Long roleId, String tenantId, String dictType);

    /**
     * 获取角色在指定应用下的全部字典配置值
     *
     * @param roleId   角色ID
     * @param tenantId 应用编号
     * @return 字典类型为 key、配置值为 value 的 Map
     */
    Map<String, String> getRoleDictValues(Long roleId, String tenantId);

    /**
     * 获取角色在当前应用下的全部字典配置值
     *
     * @param roleId 角色ID
     * @return 字典类型为 key、配置值为 value 的 Map
     */
    Map<String, String> getRoleDictValues(Long roleId);

    /**
     * 获取用户在指定应用下的全部字典配置值
     * <p>
     * 用户拥有多个角色时按角色顺序合并，后面的角色覆盖前面的角色
     *
     * @param userId   用户ID
     * @param tenantId 应用编号
     * @return 字典类型为 key、配置值为 value 的 Map
     */
    Map<String, String> getRoleDictValuesByUserId(Long userId, String tenantId);

}

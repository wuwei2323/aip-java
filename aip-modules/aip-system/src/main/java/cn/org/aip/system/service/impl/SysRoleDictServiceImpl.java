package cn.org.aip.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import cn.org.aip.common.core.constant.SystemConstants;
import cn.org.aip.common.core.exception.ServiceException;
import cn.org.aip.common.core.service.RoleDictService;
import cn.org.aip.common.core.utils.StreamUtils;
import cn.org.aip.common.core.utils.StringUtils;
import cn.org.aip.common.json.utils.JsonUtils;
import cn.org.aip.common.core.constant.TenantConstants;
import cn.org.aip.common.tenant.helper.TenantHelper;
import cn.org.aip.system.domain.SysApplication;
import cn.org.aip.system.domain.SysDictTypeRole;
import cn.org.aip.system.domain.SysRoleDictValue;
import cn.org.aip.system.domain.bo.RoleDictValueBo;
import cn.org.aip.system.domain.dto.DictComponentDTO;
import cn.org.aip.system.domain.vo.RoleDictConfigItemVo;
import cn.org.aip.system.domain.vo.RoleDictConfigVo;
import cn.org.aip.system.domain.vo.SysDictDataVo;
import cn.org.aip.system.domain.vo.SysRoleVo;
import cn.org.aip.system.mapper.SysApplicationMapper;
import cn.org.aip.system.mapper.SysDictDataMapper;
import cn.org.aip.system.mapper.SysDictTypeRoleMapper;
import cn.org.aip.system.mapper.SysRoleDictValueMapper;
import cn.org.aip.system.mapper.SysRoleMapper;
import cn.org.aip.system.service.ISysRoleDictService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 角色字典配置 业务层处理
 *
 * @author Lion Li
 */
@RequiredArgsConstructor
@Service
public class SysRoleDictServiceImpl implements ISysRoleDictService, RoleDictService {

    private final SysRoleDictValueMapper baseMapper;
    private final SysDictTypeRoleMapper dictTypeRoleMapper;
    private final SysDictDataMapper dictDataMapper;
    private final SysRoleMapper roleMapper;
    private final SysApplicationMapper applicationMapper;

    /**
     * 查询角色关联的角色类字典配置（按应用分组）
     *
     * @param roleId 角色ID
     * @return 按应用分组的字典配置列表
     */
    @Override
    public List<RoleDictConfigVo> selectRoleDictConfig(Long roleId) {
        if (ObjectUtil.isNull(roleId)) {
            return new ArrayList<>();
        }
        return buildRoleDictConfig(roleId);
    }

    /**
     * 查询新增角色时可用的角色类字典模板（无已选值）
     *
     * @return 按应用分组的字典配置模板
     */
    @Override
    public List<RoleDictConfigVo> selectRoleDictTemplate() {
        return buildRoleDictConfig(null);
    }

    /**
     * 构建角色字典配置（roleId 为空时返回新增角色的空白模板）
     *
     * @param roleId 角色ID，可为空
     * @return 按应用分组的字典配置列表
     */
    private List<RoleDictConfigVo> buildRoleDictConfig(Long roleId) {
        // 1. 先查询所有正常状态的应用，初始化分组（确保所有应用都在顶部Tab中可选）
        List<SysApplication> allApps = TenantHelper.ignore(() -> applicationMapper.selectList(new LambdaQueryWrapper<SysApplication>()
            .select(SysApplication::getAppId, SysApplication::getAppName)
            .eq(SysApplication::getStatus, SystemConstants.NORMAL)
            .orderByDesc(SysApplication::getCreateTime)
            .orderByAsc(SysApplication::getId)));
        Map<String, RoleDictConfigVo> grouped = new LinkedHashMap<>();
        for (SysApplication app : allApps) {
            grouped.computeIfAbsent(app.getAppId(), tenantId -> {
                RoleDictConfigVo vo = new RoleDictConfigVo();
                vo.setTenantId(tenantId);
                vo.setTenantName(app.getAppName());
                vo.setCheckedDictIds(new ArrayList<>());
                vo.setAllDicts(new ArrayList<>());
                return vo;
            });
        }

        // 2. 查询当前角色关联的角色类字典（按应用、字典排序）
        List<RoleDictConfigItemVo> items = baseMapper.selectAllRoleDicts(roleId, null);
        if (CollUtil.isEmpty(items)) {
            return new ArrayList<>(grouped.values());
        }

        Set<Long> checkedDictIds = new HashSet<>();
        Map<String, SysRoleDictValue> valueMap = new HashMap<>();
        if (ObjectUtil.isNotNull(roleId)) {
            // 3. 查询当前角色已关联的字典ID列表
            List<SysDictTypeRole> roleRelations = dictTypeRoleMapper.selectList(
                new LambdaQueryWrapper<SysDictTypeRole>().eq(SysDictTypeRole::getRoleId, roleId));
            checkedDictIds = StreamUtils.toSet(roleRelations, SysDictTypeRole::getDictId);

            // 4. 查询当前角色已配置的值
            List<SysRoleDictValue> existingValues = baseMapper.selectList(
                new LambdaQueryWrapper<SysRoleDictValue>().eq(SysRoleDictValue::getRoleId, roleId));
            valueMap = StreamUtils.toMap(existingValues,
                v -> (StringUtils.isNotBlank(v.getAppId()) ? v.getAppId() : v.getTenantId()) + ":" + v.getDictId(), v -> v);
        }

        // 5. 从 sys_dict_data 加载各字典类型的选项数据
        Set<String> dictTypes = StreamUtils.toSet(items, RoleDictConfigItemVo::getDictType);
        Map<String, List<DictComponentDTO.Option>> optionsMap = new HashMap<>();
        for (String type : dictTypes) {
            List<SysDictDataVo> dataList = dictDataMapper.selectDictDataByType(type);
            if (CollUtil.isNotEmpty(dataList)) {
                List<DictComponentDTO.Option> opts = StreamUtils.toList(dataList, d -> {
                    DictComponentDTO.Option opt = new DictComponentDTO.Option();
                    opt.setLabel(d.getDictLabel());
                    opt.setValue(d.getDictValue());
                    return opt;
                });
                optionsMap.put(type, opts);
            }
        }

        // 6. 按应用填充字典项（Set 去重，彻底防止同一应用内字典重复）
        Set<String> seenTenantDict = new HashSet<>();
        for (RoleDictConfigItemVo item : items) {
            String appId = StringUtils.isNotBlank(item.getAppId()) ? item.getAppId() : item.getTenantId();
            String tenantDictKey = appId + ":" + item.getDictId();
            if (!seenTenantDict.add(tenantDictKey)) {
                continue;
            }

            RoleDictConfigVo group = grouped.computeIfAbsent(appId, tid -> {
                RoleDictConfigVo vo = new RoleDictConfigVo();
                vo.setAppId(appId);
                vo.setTenantId(appId);
                vo.setAppName(item.getTenantName());
                vo.setTenantName(item.getTenantName());
                vo.setCheckedDictIds(new ArrayList<>());
                vo.setAllDicts(new ArrayList<>());
                return vo;
            });

            DictComponentDTO component = parseComponent(item.getComponentInfo());
            if (component != null && optionsMap.containsKey(item.getDictType())) {
                component.setOptions(optionsMap.get(item.getDictType()));
            }

            RoleDictConfigVo.Item vo = new RoleDictConfigVo.Item();
            vo.setDictId(item.getDictId());
            vo.setDictType(item.getDictType());
            vo.setDictName(item.getDictName());
            vo.setComponent(component);
            String fieldKey = component == null ? null : component.getFieldKey();
            vo.setFieldKey(fieldKey);

            SysRoleDictValue existingVal = valueMap.get(tenantDictKey);
            if (existingVal != null) {
                vo.setFieldValue(existingVal.getFieldValue());
                if (StringUtils.isNotBlank(existingVal.getFieldKey())) {
                    vo.setFieldKey(existingVal.getFieldKey());
                }
            }

            if (checkedDictIds.contains(item.getDictId())) {
                group.getCheckedDictIds().add(item.getDictId());
            }
            group.getAllDicts().add(vo);
        }
        return new ArrayList<>(grouped.values());
    }

    /**
     * 保存角色关联字典与字典配置值（按角色全量替换）
     *
     * @param roleId      角色ID
     * @param roleDictIds 角色关联的字典ID列表（可选，为null时不更改角色字典关联关系）
     * @param values      配置值列表
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveRoleDictConfig(Long roleId, List<Long> roleDictIds, List<RoleDictValueBo> values) {
        if (ObjectUtil.isNull(roleId)) {
            throw new ServiceException("角色ID不能为空");
        }

        // 1. 全量更新角色字典配置值 (sys_role_dict_value)
        if (values != null) {
            baseMapper.delete(new LambdaQueryWrapper<SysRoleDictValue>().eq(SysRoleDictValue::getRoleId, roleId));
            if (CollUtil.isNotEmpty(values)) {
                // 同一角色、应用、字典保留最新一条有效配置记录
                Map<String, SysRoleDictValue> result = new LinkedHashMap<>();
                for (RoleDictValueBo value : values) {
                    if (value == null || value.getDictId() == null) {
                        continue;
                    }
                    String appId = StringUtils.isNotBlank(value.getAppId()) ? value.getAppId() : value.getTenantId();
                    if (StringUtils.isBlank(appId)) {
                        continue;
                    }
                    String fieldValue = StringUtils.trim(value.getFieldValue());
                    // 仅保存非空白配置值，空白值或空JSON数组视为未配置
                    if (StringUtils.isNotBlank(fieldValue) && !"[]".equals(fieldValue)) {
                        SysRoleDictValue entity = new SysRoleDictValue();
                        entity.setRoleId(roleId);
                        entity.setTenantId(TenantConstants.DEFAULT_TENANT_ID);
                        entity.setAppId(appId);
                        entity.setDictId(value.getDictId());
                        entity.setFieldKey(StringUtils.trim(value.getFieldKey()));
                        entity.setFieldValue(fieldValue);
                        result.put(appId + ":" + value.getDictId(), entity);
                    }
                }
                if (CollUtil.isNotEmpty(result)) {
                    baseMapper.insertBatch(result.values());
                }
            }
        }

        // 2. 仅在显式指定 roleDictIds 时同步角色与字典的关联关系 (sys_dict_type_role)
        if (roleDictIds != null) {
            dictTypeRoleMapper.delete(new LambdaQueryWrapper<SysDictTypeRole>().eq(SysDictTypeRole::getRoleId, roleId));
            if (CollUtil.isNotEmpty(roleDictIds)) {
                List<SysDictTypeRole> roleDicts = StreamUtils.toList(roleDictIds, dictId -> {
                    SysDictTypeRole r = new SysDictTypeRole();
                    r.setRoleId(roleId);
                    r.setDictId(dictId);
                    return r;
                });
                dictTypeRoleMapper.insertBatch(roleDicts);
            }
        }
    }

    /**
     * 保存角色字典配置值（兼容接口）
     *
     * @param roleId 角色ID
     * @param values 配置值列表
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveRoleDictValues(Long roleId, List<RoleDictValueBo> values) {
        saveRoleDictConfig(roleId, null, values);
    }

    /**
     * 删除字典相关的角色配置值与角色关联
     *
     * @param dictIds 字典ID列表
     */
    @Override
    public void deleteByDictIds(List<Long> dictIds) {
        if (CollUtil.isEmpty(dictIds)) {
            return;
        }
        baseMapper.delete(new LambdaQueryWrapper<SysRoleDictValue>().in(SysRoleDictValue::getDictId, dictIds));
        dictTypeRoleMapper.delete(new LambdaQueryWrapper<SysDictTypeRole>().in(SysDictTypeRole::getDictId, dictIds));
    }

    /**
     * 删除字典在指定应用下的角色配置值
     *
     * @param dictId    字典ID
     * @param tenantIds 应用编号列表
     */
    @Override
    public void deleteByDictIdAndTenantIds(Long dictId, List<String> tenantIds) {
        if (ObjectUtil.isNull(dictId) || CollUtil.isEmpty(tenantIds)) {
            return;
        }
        baseMapper.delete(new LambdaQueryWrapper<SysRoleDictValue>()
            .eq(SysRoleDictValue::getDictId, dictId)
            .in(SysRoleDictValue::getAppId, tenantIds));
    }

    /**
     * 删除字典在指定角色下的配置值
     *
     * @param dictId  字典ID
     * @param roleIds 角色ID列表
     */
    @Override
    public void deleteByDictIdAndRoleIds(Long dictId, List<Long> roleIds) {
        if (ObjectUtil.isNull(dictId) || CollUtil.isEmpty(roleIds)) {
            return;
        }
        baseMapper.delete(new LambdaQueryWrapper<SysRoleDictValue>()
            .eq(SysRoleDictValue::getDictId, dictId)
            .in(SysRoleDictValue::getRoleId, roleIds));
    }

    /**
     * 删除角色相关的配置值与字典关联
     *
     * @param roleIds 角色ID列表
     */
    @Override
    public void deleteByRoleIds(List<Long> roleIds) {
        if (CollUtil.isEmpty(roleIds)) {
            return;
        }
        baseMapper.delete(new LambdaQueryWrapper<SysRoleDictValue>().in(SysRoleDictValue::getRoleId, roleIds));
        dictTypeRoleMapper.delete(new LambdaQueryWrapper<SysDictTypeRole>().in(SysDictTypeRole::getRoleId, roleIds));
    }

    /**
     * 获取角色在指定应用下某个字典的配置值
     *
     * @param roleId   角色ID
     * @param tenantId 应用编号
     * @param dictType 字典类型
     * @return 配置值，不存在时返回 null
     */
    @Override
    public String getRoleDictValue(Long roleId, String tenantId, String dictType) {
        if (StringUtils.isBlank(dictType)) {
            return null;
        }
        return getRoleDictValues(roleId, tenantId).get(dictType);
    }

    /**
     * 获取角色在指定应用下的全部字典配置值
     *
     * @param roleId   角色ID
     * @param tenantId 应用编号
     * @return 字典类型为 key、配置值为 value 的 Map
     */
    @Override
    public Map<String, String> getRoleDictValues(Long roleId, String tenantId) {
        if (ObjectUtil.isNull(roleId) || StringUtils.isBlank(tenantId)) {
            return new HashMap<>();
        }
        List<RoleDictConfigItemVo> items = baseMapper.selectRoleDictConfig(roleId, tenantId);
        Map<String, String> result = new LinkedHashMap<>();
        for (RoleDictConfigItemVo item : items) {
            if (StringUtils.isNotBlank(item.getFieldValue())) {
                result.put(item.getDictType(), item.getFieldValue());
            }
        }
        return result;
    }

    /**
     * 获取角色在当前应用下的全部字典配置值
     *
     * @param roleId 角色ID
     * @return 字典类型为 key、配置值为 value 的 Map
     */
    @Override
    public Map<String, String> getRoleDictValues(Long roleId) {
        return getRoleDictValues(roleId, TenantHelper.getTenantId());
    }

    /**
     * 获取用户在指定应用下的全部字典配置值
     * <p>
     * 用户拥有多个角色时按角色顺序合并，后面的角色覆盖前面的角色
     *
     * @param userId   用户ID
     * @param tenantId 应用编号
     * @return 字典类型为 key、配置值为 value 的 Map
     */
    @Override
    public Map<String, String> getRoleDictValuesByUserId(Long userId, String tenantId) {
        if (ObjectUtil.isNull(userId) || StringUtils.isBlank(tenantId)) {
            return new HashMap<>();
        }
        List<SysRoleVo> roles = roleMapper.selectRolesByUserId(userId);
        if (CollUtil.isEmpty(roles)) {
            return new HashMap<>();
        }
        Map<String, String> result = new LinkedHashMap<>();
        for (SysRoleVo role : roles) {
            result.putAll(getRoleDictValues(role.getRoleId(), tenantId));
        }
        return result;
    }

    /**
     * 解析组件配置
     *
     * @param componentInfo 组件配置JSON
     * @return 组件配置，格式非法时返回 null
     */
    private DictComponentDTO parseComponent(String componentInfo) {
        if (!JsonUtils.isJsonObject(componentInfo)) {
            return null;
        }
        try {
            return JsonUtils.parseObject(componentInfo, DictComponentDTO.class);
        } catch (Exception e) {
            return null;
        }
    }

}

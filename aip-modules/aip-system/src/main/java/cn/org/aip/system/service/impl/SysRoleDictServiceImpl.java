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
import cn.org.aip.common.tenant.helper.TenantHelper;
import cn.org.aip.system.constant.DictConstants;
import cn.org.aip.system.domain.SysDictType;
import cn.org.aip.system.domain.SysDictTypeRole;
import cn.org.aip.system.domain.SysDictTypeTenant;
import cn.org.aip.system.domain.SysRoleDictValue;
import cn.org.aip.system.domain.SysTenant;
import cn.org.aip.system.domain.bo.RoleDictValueBo;
import cn.org.aip.system.domain.dto.DictComponentDTO;
import cn.org.aip.system.domain.vo.RoleDictConfigItemVo;
import cn.org.aip.system.domain.vo.RoleDictConfigVo;
import cn.org.aip.system.domain.vo.SysDictDataVo;
import cn.org.aip.system.domain.vo.SysRoleVo;
import cn.org.aip.system.mapper.SysDictDataMapper;
import cn.org.aip.system.mapper.SysDictTypeMapper;
import cn.org.aip.system.mapper.SysDictTypeRoleMapper;
import cn.org.aip.system.mapper.SysDictTypeTenantMapper;
import cn.org.aip.system.mapper.SysRoleDictValueMapper;
import cn.org.aip.system.mapper.SysRoleMapper;
import cn.org.aip.system.mapper.SysTenantMapper;
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
    private final SysDictTypeMapper dictTypeMapper;
    private final SysDictTypeTenantMapper dictTypeTenantMapper;
    private final SysDictTypeRoleMapper dictTypeRoleMapper;
    private final SysDictDataMapper dictDataMapper;
    private final SysRoleMapper roleMapper;
    private final SysTenantMapper tenantMapper;

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

        // 1. 先查询所有正常状态的应用，初始化分组（确保所有应用都在顶部Tab中可选）
        List<SysTenant> allTenants = TenantHelper.ignore(() -> tenantMapper.selectList(new LambdaQueryWrapper<SysTenant>()
            .select(SysTenant::getTenantId, SysTenant::getCompanyName)
            .eq(SysTenant::getStatus, SystemConstants.NORMAL)
            .orderByAsc(SysTenant::getId)));
        Map<String, RoleDictConfigVo> grouped = new LinkedHashMap<>();
        for (SysTenant tenant : allTenants) {
            grouped.computeIfAbsent(tenant.getTenantId(), tenantId -> {
                RoleDictConfigVo vo = new RoleDictConfigVo();
                vo.setTenantId(tenantId);
                vo.setTenantName(tenant.getCompanyName());
                vo.setCheckedDictIds(new ArrayList<>());
                vo.setAllDicts(new ArrayList<>());
                return vo;
            });
        }

        // 2. 查询全部可选的角色类字典（按应用、字典排序）
        List<RoleDictConfigItemVo> items = baseMapper.selectAllRoleDicts(null);
        if (CollUtil.isEmpty(items)) {
            return new ArrayList<>(grouped.values());
        }

        // 3. 查询当前角色已关联的字典ID列表
        List<SysDictTypeRole> roleRelations = dictTypeRoleMapper.selectList(
            new LambdaQueryWrapper<SysDictTypeRole>().eq(SysDictTypeRole::getRoleId, roleId));
        Set<Long> checkedDictIds = StreamUtils.toSet(roleRelations, SysDictTypeRole::getDictId);

        // 4. 查询当前角色已配置的值
        List<SysRoleDictValue> existingValues = baseMapper.selectList(
            new LambdaQueryWrapper<SysRoleDictValue>().eq(SysRoleDictValue::getRoleId, roleId));
        Map<String, SysRoleDictValue> valueMap = StreamUtils.toMap(existingValues,
            v -> v.getTenantId() + ":" + v.getDictId(), v -> v);

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
            String tenantDictKey = item.getTenantId() + ":" + item.getDictId();
            if (!seenTenantDict.add(tenantDictKey)) {
                continue;
            }

            RoleDictConfigVo group = grouped.computeIfAbsent(item.getTenantId(), tenantId -> {
                RoleDictConfigVo vo = new RoleDictConfigVo();
                vo.setTenantId(tenantId);
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
     * @param roleDictIds 角色关联的字典ID列表
     * @param values      配置值列表
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveRoleDictConfig(Long roleId, List<Long> roleDictIds, List<RoleDictValueBo> values) {
        if (ObjectUtil.isNull(roleId)) {
            throw new ServiceException("角色ID不能为空");
        }

        // 1. 全量更新角色关联的字典 (sys_dict_type_role) - 仅当传入 roleDictIds 时更新
        if (roleDictIds != null) {
            dictTypeRoleMapper.delete(new LambdaQueryWrapper<SysDictTypeRole>().eq(SysDictTypeRole::getRoleId, roleId));
            if (CollUtil.isNotEmpty(roleDictIds)) {
                List<SysDictTypeRole> roleDicts = roleDictIds.stream()
                    .filter(ObjectUtil::isNotNull)
                    .distinct()
                    .map(dictId -> {
                        SysDictTypeRole r = new SysDictTypeRole();
                        r.setRoleId(roleId);
                        r.setDictId(dictId);
                        return r;
                    }).collect(Collectors.toList());
                if (CollUtil.isNotEmpty(roleDicts)) {
                    dictTypeRoleMapper.insertBatch(roleDicts);
                }
            }
        }

        // 2. 计算允许配置值的字典ID范围
        Set<Long> allowedDictIds;
        if (roleDictIds != null) {
            allowedDictIds = roleDictIds.stream().filter(ObjectUtil::isNotNull).collect(Collectors.toSet());
        } else {
            // 未显式传入 roleDictIds 时，保留现有角色关联字典
            List<SysDictTypeRole> existingRoleDicts = dictTypeRoleMapper.selectList(
                new LambdaQueryWrapper<SysDictTypeRole>().eq(SysDictTypeRole::getRoleId, roleId));
            allowedDictIds = StreamUtils.toSet(existingRoleDicts, SysDictTypeRole::getDictId);
        }

        // 3. 全量更新角色字典配置值 (sys_role_dict_value)
        if (values != null) {
            baseMapper.delete(new LambdaQueryWrapper<SysRoleDictValue>().eq(SysRoleDictValue::getRoleId, roleId));
            if (CollUtil.isEmpty(values) || CollUtil.isEmpty(allowedDictIds)) {
                return;
            }

            // 只保存角色已勾选关联字典的配置值
            List<RoleDictValueBo> filteredValues = values.stream()
                .filter(v -> v != null && v.getDictId() != null && allowedDictIds.contains(v.getDictId()))
                .collect(Collectors.toList());
            if (CollUtil.isEmpty(filteredValues)) {
                return;
            }

            List<Long> dictIds = StreamUtils.toList(filteredValues, RoleDictValueBo::getDictId);
            Map<Long, SysDictType> dictMap = StreamUtils.toIdentityMap(
                dictTypeMapper.selectByIds(dictIds), SysDictType::getDictId);
            Set<String> tenantRelations = dictTypeTenantMapper.selectList(new LambdaQueryWrapper<SysDictTypeTenant>()
                    .in(SysDictTypeTenant::getDictId, dictIds))
                .stream().map(relation -> relation.getDictId() + ":" + relation.getTenantId()).collect(Collectors.toSet());

            // 同一应用同一字典只保留一条记录
            Map<String, SysRoleDictValue> result = new LinkedHashMap<>();
            for (RoleDictValueBo value : filteredValues) {
                SysDictType dict = dictMap.get(value.getDictId());
                if (ObjectUtil.isNull(dict)) {
                    throw new ServiceException("字典不存在：{}", value.getDictId());
                }
                if (!DictConstants.CATEGORY_ROLE.equals(dict.getCategoryType())) {
                    throw new ServiceException("字典「{}」不是角色类字典", dict.getDictName());
                }
                if (!tenantRelations.contains(value.getDictId() + ":" + value.getTenantId())) {
                    throw new ServiceException("字典「{}」未关联应用「{}」", dict.getDictName(), value.getTenantId());
                }
                DictComponentDTO component = parseComponent(dict.getComponentInfo());
                if (component == null) {
                    throw new ServiceException("字典「{}」未配置组件信息", dict.getDictName());
                }
                // 从字典数据加载 options 用于校验
                List<SysDictDataVo> dataList = dictDataMapper.selectDictDataByType(dict.getDictType());
                if (CollUtil.isNotEmpty(dataList)) {
                    component.setOptions(StreamUtils.toList(dataList, d -> {
                        DictComponentDTO.Option opt = new DictComponentDTO.Option();
                        opt.setLabel(d.getDictLabel());
                        opt.setValue(d.getDictValue());
                        return opt;
                    }));
                }
                String fieldKey = StringUtils.isNotBlank(value.getFieldKey())
                    ? value.getFieldKey() : component.getFieldKey();
                SysRoleDictValue entity = new SysRoleDictValue();
                entity.setRoleId(roleId);
                entity.setTenantId(value.getTenantId());
                entity.setDictId(value.getDictId());
                entity.setFieldKey(fieldKey);
                entity.setFieldValue(normalizeValue(dict.getDictName(), component, value.getFieldValue()));
                result.put(value.getTenantId() + ":" + value.getDictId(), entity);
            }
            if (CollUtil.isNotEmpty(result)) {
                baseMapper.insertBatch(result.values());
            }
        } else if (roleDictIds != null) {
            // 未传入 values 但修改了 roleDictIds 时，清理不再属于该角色的字典配置值
            if (CollUtil.isNotEmpty(allowedDictIds)) {
                baseMapper.delete(new LambdaQueryWrapper<SysRoleDictValue>()
                    .eq(SysRoleDictValue::getRoleId, roleId)
                    .notIn(SysRoleDictValue::getDictId, allowedDictIds));
            } else {
                baseMapper.delete(new LambdaQueryWrapper<SysRoleDictValue>()
                    .eq(SysRoleDictValue::getRoleId, roleId));
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
        List<Long> dictIds = CollUtil.isEmpty(values) ? Collections.emptyList() :
            StreamUtils.toList(values, RoleDictValueBo::getDictId);
        saveRoleDictConfig(roleId, dictIds, values);
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
            .in(SysRoleDictValue::getTenantId, tenantIds));
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

    /**
     * 校验并规范化配置值
     * <p>
     * 多选框的值统一存为 JSON 数组字符串
     *
     * @param dictName  字典名称（用于提示）
     * @param component 组件配置
     * @param rawValue  原始值
     * @return 规范化后的值
     */
    private String normalizeValue(String dictName, DictComponentDTO component, String rawValue) {
        String componentType = component.getComponentType();
        String title = StringUtils.blankToDefault(component.getTitle(), dictName);
        Set<String> optionValues = CollUtil.isEmpty(component.getOptions()) ? new HashSet<>() :
            StreamUtils.toSet(component.getOptions(), DictComponentDTO.Option::getValue);

        if (StringUtils.equals(componentType, DictConstants.ComponentType.CHECKBOX)) {
            List<String> selected = new ArrayList<>();
            if (StringUtils.isNotBlank(rawValue)) {
                if (JsonUtils.isJsonArray(rawValue)) {
                    selected.addAll(JsonUtils.parseArray(rawValue, String.class));
                } else {
                    selected.addAll(StringUtils.splitList(rawValue));
                }
            }
            selected = selected.stream()
                .filter(StringUtils::isNotBlank)
                .map(StringUtils::trim)
                .distinct()
                .collect(Collectors.toList());
            if (Boolean.TRUE.equals(component.getRequired()) && CollUtil.isEmpty(selected)) {
                throw new ServiceException("「{}」不能为空", title);
            }
            for (String value : selected) {
                if (!optionValues.contains(value)) {
                    throw new ServiceException("「{}」的选项值不合法：{}", title, value);
                }
            }
            return CollUtil.isEmpty(selected) ? null : JsonUtils.toJsonString(selected);
        }

        String value = StringUtils.trim(rawValue);
        if (StringUtils.isBlank(value)) {
            if (Boolean.TRUE.equals(component.getRequired())) {
                throw new ServiceException("「{}」不能为空", title);
            }
            return null;
        }
        boolean optionable = StringUtils.equalsAny(componentType,
            DictConstants.ComponentType.SELECT, DictConstants.ComponentType.RADIO);
        if (optionable && !optionValues.contains(value)) {
            throw new ServiceException("「{}」的选项值不合法：{}", title, value);
        }
        return value;
    }

}

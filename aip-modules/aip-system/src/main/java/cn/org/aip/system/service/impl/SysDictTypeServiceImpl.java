package cn.org.aip.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import cn.org.aip.common.core.constant.CacheNames;
import cn.org.aip.common.core.constant.SystemConstants;
import cn.org.aip.common.core.domain.dto.DictDataDTO;
import cn.org.aip.common.core.domain.dto.DictTypeDTO;
import cn.org.aip.common.core.exception.ServiceException;
import cn.org.aip.common.core.service.DictService;
import cn.org.aip.common.core.utils.MapstructUtils;
import cn.org.aip.common.core.utils.SpringUtils;
import cn.org.aip.common.core.utils.StreamUtils;
import cn.org.aip.common.core.utils.StringUtils;
import cn.org.aip.common.mybatis.core.page.PageQuery;
import cn.org.aip.common.mybatis.core.page.TableDataInfo;
import cn.org.aip.common.redis.utils.CacheUtils;
import cn.org.aip.common.tenant.helper.TenantHelper;
import cn.org.aip.system.domain.SysDictData;
import cn.org.aip.system.domain.SysDictType;
import cn.org.aip.system.domain.SysDictTypeTenant;
import cn.org.aip.system.domain.SysTenant;
import cn.org.aip.system.domain.bo.SysDictTypeBo;
import cn.org.aip.system.domain.vo.SysDictDataVo;
import cn.org.aip.system.domain.vo.SysDictTypeVo;
import cn.org.aip.system.mapper.SysDictDataMapper;
import cn.org.aip.system.mapper.SysDictTypeMapper;
import cn.org.aip.system.mapper.SysDictTypeTenantMapper;
import cn.org.aip.system.mapper.SysTenantMapper;
import cn.org.aip.system.service.ISysDictTypeService;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 字典 业务层处理
 *
 * @author Lion Li
 */
@RequiredArgsConstructor
@Service
public class SysDictTypeServiceImpl implements ISysDictTypeService, DictService {

    private final SysDictTypeMapper baseMapper;
    private final SysDictDataMapper dictDataMapper;
    private final SysDictTypeTenantMapper dictTypeTenantMapper;
    private final SysTenantMapper tenantMapper;

    /**
     * 分页查询字典类型列表
     *
     * @param dictType  查询条件
     * @param pageQuery 分页参数
     * @return 字典类型分页列表
     */
    @Override
    public TableDataInfo<SysDictTypeVo> selectPageDictTypeList(SysDictTypeBo dictType, PageQuery pageQuery) {
        LambdaQueryWrapper<SysDictType> lqw = buildQueryWrapper(dictType);
        Page<SysDictTypeVo> page = baseMapper.selectVoPage(pageQuery.build(), lqw);
        fillTenantInfo(page.getRecords());
        return TableDataInfo.build(page);
    }

    /**
     * 根据条件分页查询字典类型
     *
     * @param dictType 字典类型信息
     * @return 字典类型集合信息
     */
    @Override
    public List<SysDictTypeVo> selectDictTypeList(SysDictTypeBo dictType) {
        LambdaQueryWrapper<SysDictType> lqw = buildQueryWrapper(dictType);
        List<SysDictTypeVo> list = baseMapper.selectVoList(lqw);
        fillTenantInfo(list);
        return list;
    }

    private LambdaQueryWrapper<SysDictType> buildQueryWrapper(SysDictTypeBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<SysDictType> lqw = Wrappers.lambdaQuery();
        lqw.like(StringUtils.isNotBlank(bo.getDictName()), SysDictType::getDictName, bo.getDictName());
        lqw.like(StringUtils.isNotBlank(bo.getDictType()), SysDictType::getDictType, bo.getDictType());
        lqw.eq(StringUtils.isNotBlank(bo.getCategoryType()), SysDictType::getCategoryType, bo.getCategoryType());
        lqw.apply(StringUtils.isNotBlank(bo.getTenantId()),
            "dict_id IN (SELECT dict_id FROM sys_dict_type_tenant WHERE tenant_id = {0})", bo.getTenantId());
        lqw.between(params.get("beginTime") != null && params.get("endTime") != null,
            SysDictType::getCreateTime, params.get("beginTime"), params.get("endTime"));
        lqw.orderByAsc(SysDictType::getDictId);
        return lqw;
    }

    /**
     * 批量回填字典类型关联的应用编号与应用名称
     *
     * @param list 字典类型列表
     */
    private void fillTenantInfo(List<SysDictTypeVo> list) {
        if (CollUtil.isEmpty(list)) {
            return;
        }
        List<Long> dictIds = StreamUtils.toList(list, SysDictTypeVo::getDictId);
        List<SysDictTypeTenant> relations = dictTypeTenantMapper.selectList(
            new LambdaQueryWrapper<SysDictTypeTenant>().in(SysDictTypeTenant::getDictId, dictIds));
        if (CollUtil.isEmpty(relations)) {
            list.forEach(vo -> {
                vo.setTenantIds(new ArrayList<>());
                vo.setTenantNames(new ArrayList<>());
            });
            return;
        }
        Set<String> tenantIds = new HashSet<>(StreamUtils.toList(relations, SysDictTypeTenant::getTenantId));
        List<SysTenant> tenants = tenantMapper.selectList(new LambdaQueryWrapper<SysTenant>()
            .select(SysTenant::getTenantId, SysTenant::getCompanyName)
            .in(SysTenant::getTenantId, tenantIds));
        Map<String, String> tenantNameMap = StreamUtils.toMap(tenants, SysTenant::getTenantId, SysTenant::getCompanyName);
        Map<Long, List<SysDictTypeTenant>> relationMap = StreamUtils.groupByKey(relations, SysDictTypeTenant::getDictId);
        for (SysDictTypeVo vo : list) {
            List<SysDictTypeTenant> dictRelations = relationMap.get(vo.getDictId());
            if (CollUtil.isEmpty(dictRelations)) {
                vo.setTenantIds(new ArrayList<>());
                vo.setTenantNames(new ArrayList<>());
                continue;
            }
            List<String> ids = StreamUtils.toList(dictRelations, SysDictTypeTenant::getTenantId);
            vo.setTenantIds(ids);
            vo.setTenantNames(StreamUtils.toList(ids, id -> tenantNameMap.getOrDefault(id, id)));
        }
    }

    /**
     * 根据所有字典类型
     *
     * @return 字典类型集合信息
     */
    @Override
    public List<SysDictTypeVo> selectDictTypeAll() {
        return baseMapper.selectVoList();
    }

    /**
     * 根据字典类型查询字典数据
     *
     * @param dictType 字典类型
     * @return 字典数据集合信息
     */
    @Cacheable(cacheNames = CacheNames.SYS_DICT, key = "#dictType")
    @Override
    public List<SysDictDataVo> selectDictDataByType(String dictType) {
        List<SysDictDataVo> dictDatas = dictDataMapper.selectDictDataByType(dictType);
        return CollUtil.isNotEmpty(dictDatas) ? dictDatas : null;
    }

    /**
     * 根据字典类型ID查询信息
     *
     * @param dictId 字典类型ID
     * @return 字典类型
     */
    @Override
    public SysDictTypeVo selectDictTypeById(Long dictId) {
        SysDictTypeVo vo = baseMapper.selectVoById(dictId);
        if (vo != null) {
            fillTenantInfo(Collections.singletonList(vo));
        }
        return vo;
    }

    /**
     * 根据字典类型查询信息
     *
     * @param dictType 字典类型
     * @return 字典类型
     */
    @Cacheable(cacheNames = CacheNames.SYS_DICT_TYPE, key = "#dictType")
    @Override
    public SysDictTypeVo selectDictTypeByType(String dictType) {
        return baseMapper.selectVoOne(new LambdaQueryWrapper<SysDictType>().eq(SysDictType::getDictType, dictType));
    }

    /**
     * 批量删除字典类型信息
     *
     * @param dictIds 需要删除的字典ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDictTypeByIds(List<Long> dictIds) {
        List<SysDictType> list = baseMapper.selectByIds(dictIds);
        list.forEach(x -> {
            boolean assigned = dictDataMapper.exists(new LambdaQueryWrapper<SysDictData>()
                .eq(SysDictData::getDictType, x.getDictType()));
            if (assigned) {
                throw new ServiceException("{}已分配,不能删除", x.getDictName());
            }
        });
        baseMapper.deleteByIds(dictIds);
        dictTypeTenantMapper.delete(new LambdaQueryWrapper<SysDictTypeTenant>()
            .in(SysDictTypeTenant::getDictId, dictIds));
        list.forEach(x -> evictDictCache(x.getDictType()));
    }

    /**
     * 清除所有应用下的字典缓存
     *
     * @param dictType 字典类型
     */
    private void evictDictCache(String dictType) {
        List<String> tenantIds = tenantMapper.selectList(new LambdaQueryWrapper<SysTenant>()
                .select(SysTenant::getTenantId)
                .eq(SysTenant::getStatus, SystemConstants.NORMAL))
            .stream().map(SysTenant::getTenantId).collect(Collectors.toList());
        if (CollUtil.isEmpty(tenantIds)) {
            CacheUtils.evict(CacheNames.SYS_DICT, dictType);
            CacheUtils.evict(CacheNames.SYS_DICT_TYPE, dictType);
            return;
        }
        for (String tenantId : tenantIds) {
            TenantHelper.dynamic(tenantId, () -> {
                CacheUtils.evict(CacheNames.SYS_DICT, dictType);
                CacheUtils.evict(CacheNames.SYS_DICT_TYPE, dictType);
            });
        }
    }

    /**
     * 重置字典缓存数据
     */
    @Override
    public void resetDictCache() {
        List<String> tenantIds = tenantMapper.selectList(new LambdaQueryWrapper<SysTenant>()
                .select(SysTenant::getTenantId)
                .eq(SysTenant::getStatus, SystemConstants.NORMAL))
            .stream().map(SysTenant::getTenantId).collect(Collectors.toList());
        if (CollUtil.isEmpty(tenantIds)) {
            CacheUtils.clear(CacheNames.SYS_DICT);
            CacheUtils.clear(CacheNames.SYS_DICT_TYPE);
            return;
        }
        for (String tenantId : tenantIds) {
            TenantHelper.dynamic(tenantId, () -> {
                CacheUtils.clear(CacheNames.SYS_DICT);
                CacheUtils.clear(CacheNames.SYS_DICT_TYPE);
            });
        }
    }

    /**
     * 新增保存字典类型信息
     *
     * @param bo 字典类型信息
     * @return 结果
     */
    @CachePut(cacheNames = CacheNames.SYS_DICT, key = "#bo.dictType")
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<SysDictDataVo> insertDictType(SysDictTypeBo bo) {
        SysDictType dict = MapstructUtils.convert(bo, SysDictType.class);
        int row = baseMapper.insert(dict);
        if (row > 0) {
            saveDictTypeTenants(dict.getDictId(), bo.getTenantIds());
            // 清除其他应用可能存在的缓存穿透数据
            evictDictCache(dict.getDictType());
            // 新增 type 下无 data 数据 返回空防止缓存穿透
            return new ArrayList<>();
        }
        throw new ServiceException("操作失败");
    }

    /**
     * 修改保存字典类型信息
     *
     * @param bo 字典类型信息
     * @return 结果
     */
    @CachePut(cacheNames = CacheNames.SYS_DICT, key = "#bo.dictType")
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<SysDictDataVo> updateDictType(SysDictTypeBo bo) {
        SysDictType dict = MapstructUtils.convert(bo, SysDictType.class);
        SysDictType oldDict = baseMapper.selectById(dict.getDictId());
        dictDataMapper.update(null, new LambdaUpdateWrapper<SysDictData>()
            .set(SysDictData::getDictType, dict.getDictType())
            .eq(SysDictData::getDictType, oldDict.getDictType()));
        int row = baseMapper.updateById(dict);
        if (row > 0) {
            saveDictTypeTenants(dict.getDictId(), bo.getTenantIds());
            evictDictCache(oldDict.getDictType());
            evictDictCache(dict.getDictType());
            return dictDataMapper.selectDictDataByType(dict.getDictType());
        }
        throw new ServiceException("操作失败");
    }

    /**
     * 保存字典类型与应用关联关系
     *
     * @param dictId    字典主键
     * @param tenantIds 应用编号列表
     */
    private void saveDictTypeTenants(Long dictId, List<String> tenantIds) {
        dictTypeTenantMapper.delete(new LambdaQueryWrapper<SysDictTypeTenant>()
            .eq(SysDictTypeTenant::getDictId, dictId));
        if (CollUtil.isEmpty(tenantIds)) {
            return;
        }
        List<SysDictTypeTenant> list = tenantIds.stream()
            .filter(StringUtils::isNotBlank)
            .distinct()
            .map(tenantId -> {
                SysDictTypeTenant relation = new SysDictTypeTenant();
                relation.setDictId(dictId);
                relation.setTenantId(tenantId);
                return relation;
            })
            .collect(Collectors.toList());
        if (CollUtil.isNotEmpty(list)) {
            dictTypeTenantMapper.insertBatch(list);
        }
    }

    /**
     * 校验字典类型称是否唯一
     *
     * @param dictType 字典类型
     * @return 结果
     */
    @Override
    public boolean checkDictTypeUnique(SysDictTypeBo dictType) {
        boolean exist = baseMapper.exists(new LambdaQueryWrapper<SysDictType>()
            .eq(SysDictType::getDictType, dictType.getDictType())
            .ne(ObjectUtil.isNotNull(dictType.getDictId()), SysDictType::getDictId, dictType.getDictId()));
        return !exist;
    }

    /**
     * 根据字典类型和字典值获取字典标签
     *
     * @param dictType  字典类型
     * @param dictValue 字典值
     * @param separator 分隔符
     * @return 字典标签
     */
    @Override
    public String getDictLabel(String dictType, String dictValue, String separator) {
        List<SysDictDataVo> datas = SpringUtils.getAopProxy(this).selectDictDataByType(dictType);
        Map<String, String> map = StreamUtils.toMap(datas, SysDictDataVo::getDictValue, SysDictDataVo::getDictLabel);
        if (StringUtils.containsAny(dictValue, separator)) {
            return Arrays.stream(dictValue.split(separator))
                .map(v -> map.getOrDefault(v, StringUtils.EMPTY))
                .collect(Collectors.joining(separator));
        } else {
            return map.getOrDefault(dictValue, StringUtils.EMPTY);
        }
    }

    /**
     * 根据字典类型和字典标签获取字典值
     *
     * @param dictType  字典类型
     * @param dictLabel 字典标签
     * @param separator 分隔符
     * @return 字典值
     */
    @Override
    public String getDictValue(String dictType, String dictLabel, String separator) {
        List<SysDictDataVo> datas = SpringUtils.getAopProxy(this).selectDictDataByType(dictType);
        Map<String, String> map = StreamUtils.toMap(datas, SysDictDataVo::getDictLabel, SysDictDataVo::getDictValue);
        if (StringUtils.containsAny(dictLabel, separator)) {
            return Arrays.stream(dictLabel.split(separator))
                .map(l -> map.getOrDefault(l, StringUtils.EMPTY))
                .collect(Collectors.joining(separator));
        } else {
            return map.getOrDefault(dictLabel, StringUtils.EMPTY);
        }
    }

    /**
     * 获取字典下所有的字典值与标签
     *
     * @param dictType 字典类型
     * @return dictValue为key，dictLabel为值组成的Map
     */
    @Override
    public Map<String, String> getAllDictByDictType(String dictType) {
        List<SysDictDataVo> list = SpringUtils.getAopProxy(this).selectDictDataByType(dictType);
        // 保证顺序
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        for (SysDictDataVo vo : list) {
            map.put(vo.getDictValue(), vo.getDictLabel());
        }
        return map;
    }

    /**
     * 根据字典类型查询详细信息
     *
     * @param dictType 字典类型
     * @return 字典类型详细信息
     */
    @Override
    public DictTypeDTO getDictType(String dictType) {
        SysDictTypeVo vo = SpringUtils.getAopProxy(this).selectDictTypeByType(dictType);
        return BeanUtil.toBean(vo, DictTypeDTO.class);
    }

    /**
     * 根据字典类型查询字典数据列表
     *
     * @param dictType 字典类型
     * @return 字典数据列表
     */
    @Override
    public List<DictDataDTO> getDictData(String dictType) {
        List<SysDictDataVo> list = SpringUtils.getAopProxy(this).selectDictDataByType(dictType);
        return BeanUtil.copyToList(list, DictDataDTO.class);
    }

}

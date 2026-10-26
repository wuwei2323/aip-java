package cn.org.aip.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import cn.org.aip.common.core.constant.CacheNames;
import cn.org.aip.common.core.constant.SystemConstants;
import cn.org.aip.common.core.exception.ServiceException;
import cn.org.aip.common.core.utils.MapstructUtils;
import cn.org.aip.common.core.utils.StringUtils;
import cn.org.aip.common.mybatis.core.page.PageQuery;
import cn.org.aip.common.mybatis.core.page.TableDataInfo;
import cn.org.aip.common.redis.utils.CacheUtils;
import cn.org.aip.common.tenant.helper.TenantHelper;
import cn.org.aip.system.domain.SysDictData;
import cn.org.aip.system.domain.SysTenant;
import cn.org.aip.system.domain.bo.SysDictDataBo;
import cn.org.aip.system.domain.vo.SysDictDataVo;
import cn.org.aip.system.mapper.SysDictDataMapper;
import cn.org.aip.system.mapper.SysTenantMapper;
import cn.org.aip.system.service.ISysDictDataService;
import org.springframework.cache.annotation.CachePut;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 字典 业务层处理
 *
 * @author Lion Li
 */
@RequiredArgsConstructor
@Service
public class SysDictDataServiceImpl implements ISysDictDataService {

    private final SysDictDataMapper baseMapper;
    private final SysTenantMapper tenantMapper;

    /**
     * 分页查询字典数据列表
     *
     * @param dictData  查询条件
     * @param pageQuery 分页参数
     * @return 字典数据分页列表
     */
    @Override
    public TableDataInfo<SysDictDataVo> selectPageDictDataList(SysDictDataBo dictData, PageQuery pageQuery) {
        LambdaQueryWrapper<SysDictData> lqw = buildQueryWrapper(dictData);
        Page<SysDictDataVo> page = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(page);
    }

    /**
     * 根据条件分页查询字典数据
     *
     * @param dictData 字典数据信息
     * @return 字典数据集合信息
     */
    @Override
    public List<SysDictDataVo> selectDictDataList(SysDictDataBo dictData) {
        LambdaQueryWrapper<SysDictData> lqw = buildQueryWrapper(dictData);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<SysDictData> buildQueryWrapper(SysDictDataBo bo) {
        LambdaQueryWrapper<SysDictData> lqw = Wrappers.lambdaQuery();
        lqw.eq(bo.getDictSort() != null, SysDictData::getDictSort, bo.getDictSort());
        lqw.like(StringUtils.isNotBlank(bo.getDictLabel()), SysDictData::getDictLabel, bo.getDictLabel());
        lqw.eq(StringUtils.isNotBlank(bo.getDictType()), SysDictData::getDictType, bo.getDictType());
        lqw.eq(StringUtils.isNotBlank(bo.getLabelStyle()), SysDictData::getLabelStyle, bo.getLabelStyle());
        lqw.orderByAsc(SysDictData::getDictSort, SysDictData::getDictCode);
        return lqw;
    }

    /**
     * 根据字典类型和字典键值查询字典数据信息
     *
     * @param dictType  字典类型
     * @param dictValue 字典键值
     * @return 字典标签
     */
    @Override
    public String selectDictLabel(String dictType, String dictValue) {
        return baseMapper.selectOne(new LambdaQueryWrapper<SysDictData>()
                .select(SysDictData::getDictLabel)
                .eq(SysDictData::getDictType, dictType)
                .eq(SysDictData::getDictValue, dictValue))
            .getDictLabel();
    }

    /**
     * 根据字典数据ID查询信息
     *
     * @param dictCode 字典数据ID
     * @return 字典数据
     */
    @Override
    public SysDictDataVo selectDictDataById(Long dictCode) {
        return baseMapper.selectVoById(dictCode);
    }

    /**
     * 批量删除字典数据信息
     *
     * @param dictCodes 需要删除的字典数据ID
     */
    @Override
    public void deleteDictDataByIds(List<Long> dictCodes) {
        List<SysDictData> list = baseMapper.selectByIds(dictCodes);
        baseMapper.deleteByIds(dictCodes);
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
            return;
        }
        for (String tenantId : tenantIds) {
            TenantHelper.dynamic(tenantId, () -> CacheUtils.evict(CacheNames.SYS_DICT, dictType));
        }
    }

    /**
     * 新增保存字典数据信息
     *
     * @param bo 字典数据信息
     * @return 结果
     */
    @CachePut(cacheNames = CacheNames.SYS_DICT, key = "#bo.dictType")
    @Override
    public List<SysDictDataVo> insertDictData(SysDictDataBo bo) {
        SysDictData data = MapstructUtils.convert(bo, SysDictData.class);
        if (StringUtils.isBlank(data.getLabelStyle())) {
            data.setLabelStyle(SystemConstants.NORMAL);
        }
        int row = baseMapper.insert(data);
        if (row > 0) {
            evictDictCache(data.getDictType());
            return baseMapper.selectDictDataByType(data.getDictType());
        }
        throw new ServiceException("操作失败");
    }

    /**
     * 修改保存字典数据信息
     *
     * @param bo 字典数据信息
     * @return 结果
     */
    @CachePut(cacheNames = CacheNames.SYS_DICT, key = "#bo.dictType")
    @Override
    public List<SysDictDataVo> updateDictData(SysDictDataBo bo) {
        SysDictData data = MapstructUtils.convert(bo, SysDictData.class);
        int row = baseMapper.updateById(data);
        if (row > 0) {
            evictDictCache(data.getDictType());
            return baseMapper.selectDictDataByType(data.getDictType());
        }
        throw new ServiceException("操作失败");
    }

    /**
     * 校验字典键值是否唯一
     *
     * @param dict 字典数据
     * @return 结果
     */
    @Override
    public boolean checkDictDataUnique(SysDictDataBo dict) {
        boolean exist = baseMapper.exists(new LambdaQueryWrapper<SysDictData>()
            .eq(SysDictData::getDictType, dict.getDictType())
            .eq(SysDictData::getDictValue, dict.getDictValue())
            .ne(ObjectUtil.isNotNull(dict.getDictCode()), SysDictData::getDictCode, dict.getDictCode()));
        return !exist;
    }

}

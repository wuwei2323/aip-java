package cn.org.aip.system.mapper;

import cn.org.aip.common.core.constant.SystemConstants;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import cn.org.aip.common.mybatis.core.mapper.BaseMapperPlus;
import cn.org.aip.system.domain.SysDictData;
import cn.org.aip.system.domain.vo.SysDictDataVo;

import java.util.List;

/**
 * 字典表 数据层
 *
 * @author Lion Li
 */
public interface SysDictDataMapper extends BaseMapperPlus<SysDictData, SysDictDataVo> {

    /**
     * 根据字典类型查询字典数据列表（仅查询正常状态）
     *
     * @param dictType 字典类型
     * @return 符合条件的字典数据列表
     */
    default List<SysDictDataVo> selectDictDataByType(String dictType) {
        return selectVoList(
            new LambdaQueryWrapper<SysDictData>()
                .eq(SysDictData::getDictType, dictType)
                .eq(SysDictData::getLabelStyle, SystemConstants.NORMAL)
                .orderByAsc(SysDictData::getDictSort));
    }
}

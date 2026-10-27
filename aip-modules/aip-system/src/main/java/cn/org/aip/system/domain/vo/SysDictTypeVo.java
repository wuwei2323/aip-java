package cn.org.aip.system.domain.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import cn.org.aip.common.excel.annotation.ExcelDictFormat;
import cn.org.aip.common.excel.convert.ExcelDictConvert;
import cn.org.aip.system.domain.SysDictType;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;


/**
 * 字典类型视图对象 sys_dict_type
 *
 * @author Michelle.Chung
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = SysDictType.class)
public class SysDictTypeVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 字典主键
     */
    @ExcelProperty(value = "字典主键")
    private Long dictId;

    /**
     * 字典名称
     */
    @ExcelProperty(value = "字典名称")
    private String dictName;

    /**
     * 字典类型
     */
    @ExcelProperty(value = "字典类型")
    private String dictType;

    /**
     * 字典分类（system=系统 role=角色）
     */
    @ExcelProperty(value = "字典分类")
    private String categoryType;

    /**
     * 组件配置信息（JSON，角色字典使用）
     */
    private String componentInfo;

    /**
     * 关联应用编号列表
     */
    private List<String> tenantIds;

    /**
     * 关联应用名称列表
     */
    private List<String> tenantNames;

    /**
     * 备注
     */
    @ExcelProperty(value = "备注")
    private String remark;

    /**
     * 创建时间
     */
    @ExcelProperty(value = "创建时间")
    private Date createTime;

}

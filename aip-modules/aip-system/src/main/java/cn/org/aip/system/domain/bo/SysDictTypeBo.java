package cn.org.aip.system.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import cn.org.aip.common.core.constant.RegexConstants;
import cn.org.aip.common.mybatis.core.domain.BaseEntity;
import cn.org.aip.system.domain.SysDictType;

import java.util.List;

/**
 * 字典类型业务对象 sys_dict_type
 *
 * @author Michelle.Chung
 */

@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = SysDictType.class, reverseConvertGenerate = false)
public class SysDictTypeBo extends BaseEntity {

    /**
     * 字典主键
     */
    private Long dictId;

    /**
     * 字典名称
     */
    @NotBlank(message = "字典名称不能为空")
    @Size(min = 0, max = 100, message = "字典类型名称长度不能超过{max}个字符")
    private String dictName;

    /**
     * 字典类型
     */
    @NotBlank(message = "字典类型不能为空")
    @Size(min = 0, max = 100, message = "字典类型类型长度不能超过{max}个字符")
    @Pattern(regexp = RegexConstants.DICTIONARY_TYPE, message = "字典类型必须以字母开头，且只能为（小写字母，数字，下滑线）")
    private String dictType;

    /**
     * 字典分类（system=系统 role=角色）
     */
    @NotBlank(message = "字典分类不能为空")
    private String categoryType;

    /**
     * 关联角色ID列表（JSON数组）
     */
    private String roleIds;

    /**
     * 组件配置信息（JSON）
     */
    private String componentInfo;

    /**
     * 关联应用编号列表
     */
    @NotEmpty(message = "关联应用不能为空")
    private List<String> tenantIds;

    /**
     * 应用编号（查询条件）
     */
    private String tenantId;

    /**
     * 备注
     */
    private String remark;


}

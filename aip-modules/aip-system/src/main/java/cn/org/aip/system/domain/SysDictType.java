package cn.org.aip.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import cn.org.aip.common.mybatis.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 字典类型表 sys_dict_type
 *
 * @author Lion Li
 */

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_dict_type")
public class SysDictType extends BaseEntity {

    /**
     * 字典主键
     */
    @TableId(value = "dict_id")
    private Long dictId;

    /**
     * 字典名称
     */
    private String dictName;

    /**
     * 字典类型
     */
    private String dictType;

    /**
     * 字典分类（system=系统 role=角色）
     */
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
     * 备注
     */
    private String remark;

}

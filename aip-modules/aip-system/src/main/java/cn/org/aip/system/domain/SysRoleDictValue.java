package cn.org.aip.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import cn.org.aip.common.mybatis.core.domain.BaseEntity;

/**
 * 角色字典配置值 sys_role_dict_value
 *
 * @author Lion Li
 */

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role_dict_value")
public class SysRoleDictValue extends BaseEntity {

    /**
     * 主键
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 角色ID
     */
    private Long roleId;

    /**
     * 应用编号
     */
    private String tenantId;

    /**
     * 字典主键
     */
    private Long dictId;

    /**
     * 组件字段标识（取 component_info.fieldKey）
     */
    private String fieldKey;

    /**
     * 字段值（多选为JSON数组字符串）
     */
    private String fieldValue;

}

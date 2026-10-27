package cn.org.aip.system.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 角色字典配置项（联表查询结果）
 *
 * @author Lion Li
 */
@Data
public class RoleDictConfigItemVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 字典主键
     */
    private Long dictId;

    /**
     * 字典类型
     */
    private String dictType;

    /**
     * 字典名称
     */
    private String dictName;

    /**
     * 组件配置信息（JSON）
     */
    private String componentInfo;

    /**
     * 应用编号
     */
    private String tenantId;

    /**
     * 应用名称
     */
    private String tenantName;

    /**
     * 组件字段标识
     */
    private String fieldKey;

    /**
     * 字段值
     */
    private String fieldValue;

}

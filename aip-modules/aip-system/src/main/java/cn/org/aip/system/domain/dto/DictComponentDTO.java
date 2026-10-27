package cn.org.aip.system.domain.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 角色字典组件配置
 * <p>
 * 对应 sys_dict_type.component_info，当前一个字典仅配置一个组件
 *
 * @author Lion Li
 */
@Data
public class DictComponentDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 组件字段标识，作为 sys_role_dict_value.field_key
     */
    private String fieldKey;

    /**
     * 组件类型：textarea / select / radio / checkbox / datetime
     */
    private String componentType;

    /**
     * 组件标题
     */
    private String title;

    /**
     * 占位提示
     */
    private String placeholder;

    /**
     * 是否必填
     */
    private Boolean required;

    /**
     * 默认值
     */
    private Object defaultValue;

    /**
     * 选项列表（select / radio / checkbox 使用）
     */
    private List<Option> options;

    /**
     * 选项
     */
    @Data
    public static class Option implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 选项标签
         */
        private String label;

        /**
         * 选项值
         */
        private String value;

    }

}

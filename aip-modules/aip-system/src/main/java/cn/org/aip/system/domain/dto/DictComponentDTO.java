package cn.org.aip.system.domain.dto;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
     * 展示样式（radio / checkbox 使用，如 default / button）
     */
    private String displayStyle;

    /**
     * 最少选择项数（checkbox 使用）
     */
    private Integer minSelections;

    /**
     * 最多选择项数（checkbox 使用）
     */
    private Integer maxSelections;

    /**
     * 时间类型（datetime 使用，如 datetime / date 等）
     */
    private String dateType;

    /**
     * 显示格式（datetime 使用，如 yyyy-MM-dd HH:mm:ss）
     */
    private String format;

    /**
     * 其他扩展属性
     */
    private Map<String, Object> extra = new HashMap<>();

    @JsonAnyGetter
    public Map<String, Object> getExtra() {
        return extra;
    }

    @JsonAnySetter
    public void setExtra(String key, Object value) {
        if (extra == null) {
            extra = new HashMap<>();
        }
        extra.put(key, value);
    }

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

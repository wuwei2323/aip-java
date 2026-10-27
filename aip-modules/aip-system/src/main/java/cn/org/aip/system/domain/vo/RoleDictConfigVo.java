package cn.org.aip.system.domain.vo;

import cn.org.aip.system.domain.dto.DictComponentDTO;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 角色字典配置视图对象（按应用分组）
 *
 * @author Lion Li
 */
@Data
public class RoleDictConfigVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 应用编号
     */
    private String tenantId;

    /**
     * 应用名称
     */
    private String tenantName;

    /**
     * 该角色在该应用下已勾选关联的字典ID列表
     */
    private List<Long> checkedDictIds;

    /**
     * 该应用下全部可选的角色类字典列表（含从 sys_dict_data 动态组装的 options）
     */
    private List<Item> allDicts;

    /**
     * 兼容字段
     */
    public List<Item> getItems() {
        return allDicts;
    }

    /**
     * 字典配置项
     */
    @Data
    public static class Item implements Serializable {

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
         * 组件配置（含从字典数据组装出的 options）
         */
        private DictComponentDTO component;

        /**
         * 组件字段标识
         */
        private String fieldKey;

        /**
         * 当前角色配置的字段值
         */
        private String fieldValue;

    }

}

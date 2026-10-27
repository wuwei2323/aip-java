package cn.org.aip.system.constant;

/**
 * 字典常量
 *
 * @author Lion Li
 */
public interface DictConstants {

    /**
     * 字典分类（系统）
     */
    String CATEGORY_SYSTEM = "system";

    /**
     * 字典分类（角色）
     */
    String CATEGORY_ROLE = "role";

    /**
     * 角色字典组件类型
     */
    interface ComponentType {

        /**
         * 文本框
         */
        String TEXTAREA = "textarea";

        /**
         * 下拉选项
         */
        String SELECT = "select";

        /**
         * 单选框组
         */
        String RADIO = "radio";

        /**
         * 多选框组
         */
        String CHECKBOX = "checkbox";

        /**
         * 日期时间
         */
        String DATETIME = "datetime";

    }

}

package cn.org.aip.common.core.constant;

/**
 * 应用常量信息
 *
 * @author Lion Li
 */
public interface TenantConstants {

    /**
     * 超级管理员ID
     */
    Long SUPER_ADMIN_ID = 1L;

    /**
     * 超级管理员角色 roleKey
     */
    String SUPER_ADMIN_ROLE_KEY = "superadmin";

    /**
     * 应用管理员角色 roleKey
     */
    String TENANT_ADMIN_ROLE_KEY = "admin";

    /**
     * 应用管理员角色名称
     */
    String TENANT_ADMIN_ROLE_NAME = "管理员";

    /**
     * 默认应用ID
     */
    String DEFAULT_TENANT_ID = "000000";

}

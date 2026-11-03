package cn.org.aip.web.domain.vo;

import lombok.Data;

/**
 * 应用列表 (登录页与导航栏应用切换展示对象)
 *
 * @author Lion Li
 */
@Data
public class TenantListVo {

    /**
     * 应用编号
     */
    private String tenantId;

    /**
     * 应用名称
     */
    private String companyName;

    /**
     * 访问地址/域名
     */
    private String domain;

}

package cn.org.aip.web.domain.vo;

import lombok.Data;

import java.util.List;

/**
 * 登录应用对象
 *
 * @author Michelle.Chung
 */
@Data
public class LoginTenantVo {

    /**
     * 应用开关
     */
    private Boolean tenantEnabled;

    /**
     * 应用对象列表
     */
    private List<TenantListVo> voList;

}

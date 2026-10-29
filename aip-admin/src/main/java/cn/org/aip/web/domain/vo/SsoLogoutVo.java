package cn.org.aip.web.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * SSO 登出地址响应
 *
 * @author Michelle.Chung
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SsoLogoutVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Keycloak 登出跳转 URL
     */
    private String logoutUrl;

}

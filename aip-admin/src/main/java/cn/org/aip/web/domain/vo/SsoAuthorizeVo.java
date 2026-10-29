package cn.org.aip.web.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * SSO 授权地址响应
 *
 * @author Michelle.Chung
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SsoAuthorizeVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Keycloak 授权登录 URL
     */
    private String authorizeUrl;

}

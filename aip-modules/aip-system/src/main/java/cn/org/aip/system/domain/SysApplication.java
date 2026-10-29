package cn.org.aip.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import cn.org.aip.common.mybatis.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 应用注册对象 sys_application
 *
 * @author Michelle.Chung
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_application")
public class SysApplication extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id")
    private Long id;

    /**
     * OIDC客户端ID (client_id)
     */
    private String appId;

    /**
     * 应用名称
     */
    private String appName;

    /**
     * 应用/租户编号 (与 sys_tenant 1:1)
     */
    private String tenantId;

    /**
     * 客户端认证方式 (client_secret_basic / client_secret_post / none)
     */
    private String clientAuthMethod;

    /**
     * 客户端密钥 (AES加密存储)
     */
    private String clientSecretEnc;

    /**
     * 登录重定向URI列表 (JSON数组)
     */
    private String redirectUris;

    /**
     * 登出重定向URI列表 (JSON数组)
     */
    private String postLogoutRedirectUris;

    /**
     * 授权范围
     */
    private String scopes;

    /**
     * 授权类型
     */
    private String grantTypes;

    /**
     * 访问令牌有效期 (分钟)
     */
    private Integer accessTokenTtlMinutes;

    /**
     * 刷新令牌有效期 (天)
     */
    private Integer refreshTokenTtlDays;

    /**
     * 是否复用刷新令牌 (0否 1是)
     */
    private String reuseRefreshTokens;

    /**
     * 是否强制PKCE (0否 1是)
     */
    private String requirePkce;

    /**
     * 应用域名
     */
    private String domain;

    /**
     * 应用入口地址
     */
    private String entryUrl;

    /**
     * 企微配置标识
     */
    private String wecomConfigKey;

    /**
     * 用户绑定策略 (REJECT / AUTO_REGISTER / PROMPT_BIND)
     */
    private String bindPolicy;

    /**
     * 应用图标
     */
    private String icon;

    /**
     * 显示顺序
     */
    private Integer orderNum;

    /**
     * 状态 (0正常 1停用)
     */
    private String status;

    /**
     * 删除标志 (0代表存在 1代表删除)
     */
    @TableLogic
    private String delFlag;

}

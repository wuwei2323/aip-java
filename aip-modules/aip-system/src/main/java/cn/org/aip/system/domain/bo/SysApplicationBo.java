package cn.org.aip.system.domain.bo;

import cn.org.aip.common.core.validate.AddGroup;
import cn.org.aip.common.core.validate.EditGroup;
import cn.org.aip.common.mybatis.core.domain.BaseEntity;
import cn.org.aip.system.domain.SysApplication;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 应用注册业务对象 sys_application
 *
 * @author Michelle.Chung
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = SysApplication.class, reverseConvertGenerate = false)
public class SysApplicationBo extends BaseEntity {

    /**
     * 主键
     */
    @NotNull(message = "id不能为空", groups = { EditGroup.class })
    private Long id;

    /**
     * OIDC客户端ID (client_id)
     */
    @Size(max = 64, message = "客户端ID长度不能超过{max}个字符")
    private String appId;

    /**
     * 应用名称
     */
    @NotBlank(message = "应用名称不能为空", groups = { AddGroup.class, EditGroup.class })
    @Size(max = 100, message = "应用名称长度不能超过{max}个字符")
    private String appName;

    /**
     * 应用/租户编号
     */
    @NotBlank(message = "应用/租户编号不能为空", groups = { AddGroup.class, EditGroup.class })
    @Size(max = 20, message = "应用/租户编号长度不能超过{max}个字符")
    private String tenantId;

    /**
     * 客户端认证方式
     */
    private String clientAuthMethod;

    /**
     * 客户端密钥 (明文传入，仅用于新增或重置密钥，保存时加密)
     */
    private String clientSecret;

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
     * 用户绑定策略
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

}

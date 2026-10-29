package cn.org.aip.system.domain.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import cn.org.aip.common.excel.annotation.ExcelDictFormat;
import cn.org.aip.common.excel.convert.ExcelDictConvert;
import cn.org.aip.system.domain.SysApplication;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 应用注册视图对象 sys_application
 *
 * @author Michelle.Chung
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = SysApplication.class)
public class SysApplicationVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @ExcelProperty(value = "id")
    private Long id;

    /**
     * OIDC客户端ID (client_id)
     */
    @ExcelProperty(value = "客户端ID")
    private String appId;

    /**
     * 应用名称
     */
    @ExcelProperty(value = "应用名称")
    private String appName;

    /**
     * 应用/租户编号
     */
    @ExcelProperty(value = "应用编号")
    private String tenantId;

    /**
     * 客户端认证方式
     */
    @ExcelProperty(value = "客户端认证方式")
    private String clientAuthMethod;

    /**
     * 客户端密钥 (明文，仅在新增或重置密钥时返回一次)
     */
    private String clientSecret;

    /**
     * 登录重定向URI列表 (JSON数组)
     */
    @ExcelProperty(value = "重定向URI列表")
    private String redirectUris;

    /**
     * 登出重定向URI列表 (JSON数组)
     */
    @ExcelProperty(value = "登出重定向URI列表")
    private String postLogoutRedirectUris;

    /**
     * 授权范围
     */
    @ExcelProperty(value = "授权范围")
    private String scopes;

    /**
     * 授权类型
     */
    @ExcelProperty(value = "授权类型")
    private String grantTypes;

    /**
     * 访问令牌有效期 (分钟)
     */
    @ExcelProperty(value = "访问令牌有效期(分钟)")
    private Integer accessTokenTtlMinutes;

    /**
     * 刷新令牌有效期 (天)
     */
    @ExcelProperty(value = "刷新令牌有效期(天)")
    private Integer refreshTokenTtlDays;

    /**
     * 是否复用刷新令牌
     */
    private String reuseRefreshTokens;

    /**
     * 是否强制PKCE
     */
    private String requirePkce;

    /**
     * 应用域名
     */
    @ExcelProperty(value = "应用域名")
    private String domain;

    /**
     * 应用入口地址
     */
    @ExcelProperty(value = "入口地址")
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
    @ExcelProperty(value = "状态", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "0=正常,1=停用")
    private String status;

    /**
     * 创建时间
     */
    @ExcelProperty(value = "创建时间")
    private Date createTime;

}

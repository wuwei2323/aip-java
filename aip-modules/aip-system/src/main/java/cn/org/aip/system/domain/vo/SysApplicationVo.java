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
 * 应用管理视图对象 sys_application
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
     * 应用编码 / OIDC客户端ID (client_id)
     */
    @ExcelProperty(value = "应用编码")
    private String appId;

    /**
     * 应用名称
     */
    @ExcelProperty(value = "应用名称")
    private String appName;

    /**
     * 应用分类
     */
    @ExcelProperty(value = "应用分类")
    private String category;

    /**
     * 应用简介
     */
    @ExcelProperty(value = "应用简介")
    private String description;

    /**
     * 应用图标
     */
    @ExcelProperty(value = "应用图标")
    private String icon;

    /**
     * 访问地址 (入口地址)
     */
    @ExcelProperty(value = "访问地址")
    private String entryUrl;

    /**
     * 登录重定向URI列表 (JSON数组)
     */
    @ExcelProperty(value = "登录重定向URI列表")
    private String redirectUris;

    /**
     * 客户端密钥 (明文，仅在新增或重置密钥时返回一次)
     */
    private String clientSecret;

    /**
     * 状态 (0正常 1停用)
     */
    @ExcelProperty(value = "状态", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "0=正常,1=停用")
    private String status;

    /**
     * 创建时间 (接入时间)
     */
    @ExcelProperty(value = "接入时间")
    private Date createTime;

}

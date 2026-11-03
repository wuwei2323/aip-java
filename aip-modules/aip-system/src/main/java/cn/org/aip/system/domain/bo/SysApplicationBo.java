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
 * 应用管理业务对象 sys_application
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
     * 应用编码 / OIDC客户端ID (client_id)
     */
    @Size(max = 64, message = "应用编码长度不能超过{max}个字符")
    private String appId;

    /**
     * 应用名称
     */
    @NotBlank(message = "应用名称不能为空", groups = { AddGroup.class, EditGroup.class })
    @Size(max = 100, message = "应用名称长度不能超过{max}个字符")
    private String appName;

    /**
     * 应用分类
     */
    @NotBlank(message = "应用分类不能为空", groups = { AddGroup.class, EditGroup.class })
    @Size(max = 64, message = "应用分类长度不能超过{max}个字符")
    private String category;

    /**
     * 应用简介
     */
    @NotBlank(message = "应用简介不能为空", groups = { AddGroup.class, EditGroup.class })
    @Size(max = 500, message = "应用简介长度不能超过{max}个字符")
    private String description;

    /**
     * 应用图标
     */
    @NotBlank(message = "应用图标不能为空", groups = { AddGroup.class, EditGroup.class })
    @Size(max = 255, message = "应用图标长度不能超过{max}个字符")
    private String icon;

    /**
     * 访问地址 (入口地址)
     */
    @NotBlank(message = "访问地址不能为空", groups = { AddGroup.class, EditGroup.class })
    @Size(max = 500, message = "访问地址长度不能超过{max}个字符")
    private String entryUrl;

    /**
     * 登录重定向URI列表 (JSON数组)
     */
    @NotBlank(message = "登录回调地址不能为空", groups = { AddGroup.class, EditGroup.class })
    private String redirectUris;

    /**
     * 状态 (0正常 1停用)
     */
    private String status;

}

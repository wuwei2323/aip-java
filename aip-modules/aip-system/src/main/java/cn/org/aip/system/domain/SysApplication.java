package cn.org.aip.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import cn.org.aip.common.mybatis.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 应用管理对象 sys_application
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
     * 应用编码 / OIDC客户端ID (client_id)
     */
    private String appId;

    /**
     * 应用名称
     */
    private String appName;

    /**
     * 应用分类
     */
    private String category;

    /**
     * 应用简介
     */
    private String description;

    /**
     * 应用图标
     */
    private String icon;

    /**
     * 访问地址 (入口地址)
     */
    private String entryUrl;

    /**
     * 登录重定向URI列表 (JSON数组)
     */
    private String redirectUris;

    /**
     * 客户端密钥 (AES加密存储)
     */
    private String clientSecretEnc;

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

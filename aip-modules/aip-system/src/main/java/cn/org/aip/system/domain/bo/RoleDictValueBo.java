package cn.org.aip.system.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 角色字典配置值业务对象
 *
 * @author Lion Li
 */
@Data
public class RoleDictValueBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 应用编号
     */
    @NotBlank(message = "应用编号不能为空")
    private String tenantId;

    /**
     * 应用编号（兼容字段）
     */
    private String appId;

    /**
     * 字典主键
     */
    @NotNull(message = "字典ID不能为空")
    private Long dictId;

    /**
     * 组件字段标识
     */
    @Size(max = 64, message = "组件字段标识长度不能超过{max}个字符")
    private String fieldKey;

    /**
     * 字段值（多选为JSON数组字符串）
     */
    private String fieldValue;

    public String getTenantId() {
        return cn.org.aip.common.core.utils.StringUtils.isNotBlank(tenantId) ? tenantId : appId;
    }

    public String getAppId() {
        return cn.org.aip.common.core.utils.StringUtils.isNotBlank(appId) ? appId : tenantId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
        if (this.tenantId == null) {
            this.tenantId = appId;
        }
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
        if (this.appId == null) {
            this.appId = tenantId;
        }
    }

}

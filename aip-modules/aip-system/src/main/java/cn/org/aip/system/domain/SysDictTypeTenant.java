package cn.org.aip.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 字典类型与应用关联 sys_dict_type_tenant
 *
 * @author Lion Li
 */

@Data
@TableName("sys_dict_type_tenant")
public class SysDictTypeTenant implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 字典主键
     */
    @TableId(type = IdType.INPUT)
    private Long dictId;

    /**
     * 应用编号
     */
    private String tenantId;

}

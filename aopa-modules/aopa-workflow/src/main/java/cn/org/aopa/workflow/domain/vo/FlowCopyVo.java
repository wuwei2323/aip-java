package cn.org.aopa.workflow.domain.vo;

import lombok.Data;
import cn.org.aopa.common.translation.annotation.Translation;
import cn.org.aopa.common.translation.constant.TransConstant;

import java.io.Serial;
import java.io.Serializable;

/**
 * 抄送对象
 *
 * @author AprilWind
 */
@Data
public class FlowCopyVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 用户名称
     */
    @Translation(type = TransConstant.USER_ID_TO_NICKNAME, mapper = "userId")
    private String userName;

    public FlowCopyVo(Long userId) {
        this.userId = userId;
    }

}

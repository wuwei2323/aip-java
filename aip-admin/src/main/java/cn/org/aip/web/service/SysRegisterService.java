package cn.org.aip.web.service;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import cn.org.aip.common.core.constant.Constants;
import cn.org.aip.common.core.constant.GlobalConstants;
import cn.org.aip.common.core.domain.model.RegisterBody;
import cn.org.aip.common.core.enums.UserType;
import cn.org.aip.common.core.exception.user.CaptchaException;
import cn.org.aip.common.core.exception.user.CaptchaExpireException;
import cn.org.aip.common.core.exception.user.UserException;
import cn.org.aip.common.core.utils.MessageUtils;
import cn.org.aip.common.core.utils.ServletUtils;
import cn.org.aip.common.core.utils.SpringUtils;
import cn.org.aip.common.core.utils.StringUtils;
import cn.org.aip.common.log.event.LogininforEvent;
import cn.org.aip.common.redis.utils.RedisUtils;
import cn.org.aip.common.tenant.helper.TenantHelper;
// import cn.org.aip.common.web.config.properties.CaptchaProperties;
import cn.org.aip.system.domain.SysUser;
import cn.org.aip.system.domain.bo.SysUserBo;
import cn.org.aip.system.mapper.SysUserMapper;
import cn.org.aip.system.service.ISysUserService;
import org.springframework.stereotype.Service;

/**
 * 注册校验方法
 *
 * @author Lion Li
 */
@RequiredArgsConstructor
@Service
public class SysRegisterService {

    private final ISysUserService userService;
    private final SysUserMapper userMapper;
    // private final CaptchaProperties captchaProperties;

    /**
     * 注册
     */
    public void register(RegisterBody registerBody) {
        String tenantId = registerBody.getTenantId();
        String username = registerBody.getUsername();
        String password = registerBody.getPassword();
        // 校验用户类型是否存在
        String userType = UserType.getUserType(registerBody.getUserType()).getUserType();

        // boolean captchaEnabled = captchaProperties.getEnable();
        // // 验证码开关
        // if (captchaEnabled) {
        //     validateCaptcha(tenantId, username, registerBody.getCode(), registerBody.getUuid());
        // }
        SysUserBo sysUser = new SysUserBo();
        sysUser.setUserName(username);
        sysUser.setNickName(username);
        sysUser.setPassword(BCrypt.hashpw(password));
        sysUser.setUserType(userType);

        boolean exist = TenantHelper.ignore(() -> {
            return userMapper.exists(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUserName, sysUser.getUserName()));
        });
        if (exist) {
            throw new UserException("user.register.save.error", username);
        }
        boolean regFlag = userService.registerUser(sysUser, tenantId);
        if (!regFlag) {
            throw new UserException("user.register.error");
        }
        recordLogininfor(tenantId, username, Constants.REGISTER, MessageUtils.message("user.register.success"));
    }

    /**
     * 校验验证码
     *
     * @param username 用户名
     * @param code     验证码
     * @param uuid     唯一标识
     */
    public void validateCaptcha(String tenantId, String username, String code, String uuid) {
        String verifyKey = GlobalConstants.CAPTCHA_CODE_KEY + StringUtils.blankToDefault(uuid, "");
        String captcha = RedisUtils.getCacheObject(verifyKey);
        RedisUtils.deleteObject(verifyKey);
        if (captcha == null) {
            recordLogininfor(tenantId, username, Constants.LOGIN_FAIL, MessageUtils.message("user.jcaptcha.expire"));
            throw new CaptchaExpireException();
        }
        if (!StringUtils.equalsIgnoreCase(code, captcha)) {
            recordLogininfor(tenantId, username, Constants.LOGIN_FAIL, MessageUtils.message("user.jcaptcha.error"));
            throw new CaptchaException();
        }
    }

    /**
     * 记录登录信息
     *
     * @param tenantId 应用ID
     * @param username 用户名
     * @param status   状态
     * @param message  消息内容
     * @return
     */
    private void recordLogininfor(String tenantId, String username, String status, String message) {
        LogininforEvent logininforEvent = new LogininforEvent();
        logininforEvent.setTenantId(tenantId);
        logininforEvent.setUsername(username);
        logininforEvent.setStatus(status);
        logininforEvent.setMessage(message);
        logininforEvent.setRequest(ServletUtils.getRequest());
        SpringUtils.context().publishEvent(logininforEvent);
    }

}

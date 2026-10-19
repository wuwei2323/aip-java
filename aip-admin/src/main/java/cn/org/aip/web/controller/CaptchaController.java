package cn.org.aip.web.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import cn.hutool.core.util.RandomUtil;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import cn.org.aip.common.core.constant.Constants;
import cn.org.aip.common.core.constant.GlobalConstants;
import cn.org.aip.common.core.domain.R;
import cn.org.aip.common.core.exception.ServiceException;
import cn.org.aip.common.core.utils.SpringUtils;
import cn.org.aip.common.mail.config.properties.MailProperties;
import cn.org.aip.common.mail.utils.MailUtils;
import cn.org.aip.common.ratelimiter.annotation.RateLimiter;
import cn.org.aip.common.redis.utils.RedisUtils;
import cn.org.aip.common.web.config.properties.CaptchaProperties;
import org.dromara.sms4j.api.SmsBlend;
import org.dromara.sms4j.api.entity.SmsResponse;
import org.dromara.sms4j.core.factory.SmsFactory;
import cn.org.aip.web.domain.vo.CaptchaVo;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.LinkedHashMap;

/**
 * 验证码操作处理
 *
 * @author Lion Li
 */
@SaIgnore
@Slf4j
@Validated
@RequiredArgsConstructor
@RestController
public class CaptchaController {

    private final CaptchaProperties captchaProperties;
    private final MailProperties mailProperties;

    /**
     * 短信验证码
     *
     * @param phonenumber 用户手机号
     */
    @RateLimiter(key = "#phonenumber", time = 60, count = 1)
    @GetMapping("/resource/sms/code")
    public R<Void> smsCode(@NotBlank(message = "{user.phonenumber.not.blank}") String phonenumber) {
        String key = GlobalConstants.CAPTCHA_CODE_KEY + phonenumber;
        String code = RandomUtil.randomNumbers(4);
        RedisUtils.setCacheObject(key, code, Duration.ofMinutes(Constants.CAPTCHA_EXPIRATION));
        // 验证码模板id 自行处理 (查数据库或写死均可)
        String templateId = "";
        LinkedHashMap<String, String> map = new LinkedHashMap<>(1);
        map.put("code", code);
        SmsBlend smsBlend = SmsFactory.getSmsBlend("config1");
        SmsResponse smsResponse = smsBlend.sendMessage(phonenumber, templateId, map);
        if (!smsResponse.isSuccess()) {
            log.error("验证码短信发送异常 => {}", smsResponse);
            return R.fail(smsResponse.getData().toString());
        }
        return R.ok();
    }

    /**
     * 邮箱验证码
     *
     * @param email 邮箱
     */
    @GetMapping("/resource/email/code")
    public R<Void> emailCode(@NotBlank(message = "{user.email.not.blank}") String email) {
        if (!mailProperties.getEnabled()) {
            return R.fail("当前系统没有开启邮箱功能！");
        }
        SpringUtils.getAopProxy(this).emailCodeImpl(email);
        return R.ok();
    }

    /**
     * 邮箱验证码
     * 独立方法避免验证码关闭之后仍然走限流
     */
    @RateLimiter(key = "#email", time = 60, count = 1)
    public void emailCodeImpl(String email) {
        String key = GlobalConstants.CAPTCHA_CODE_KEY + email;
        String code = RandomUtil.randomNumbers(4);
        RedisUtils.setCacheObject(key, code, Duration.ofMinutes(Constants.CAPTCHA_EXPIRATION));
        try {
            MailUtils.sendText(email, "登录验证码", "您本次验证码为：" + code + "，有效性为" + Constants.CAPTCHA_EXPIRATION + "分钟，请尽快填写。");
        } catch (Exception e) {
            log.error("验证码短信发送异常 => {}", e.getMessage());
            throw new ServiceException(e.getMessage());
        }
    }

    /**
     * 生成验证码
     */
    @GetMapping("/auth/code")
    public R<CaptchaVo> getCode() {
        boolean captchaEnabled = captchaProperties.getEnable();
        if (!captchaEnabled) {
            CaptchaVo captchaVo = new CaptchaVo();
            captchaVo.setCaptchaEnabled(false);
            return R.ok(captchaVo);
        }
        // 返回滑动验证码配置
        CaptchaVo captchaVo = new CaptchaVo();
        captchaVo.setCaptchaEnabled(true);
        // 这里返回true表示使用滑动验证码，前端根据这个字段切换验证码类型
        captchaVo.setSlidingCaptcha(true);
        return R.ok(captchaVo);
    }

}

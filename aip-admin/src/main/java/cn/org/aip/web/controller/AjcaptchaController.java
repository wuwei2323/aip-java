package cn.org.aip.web.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import cn.org.aip.common.core.domain.R;

import com.anji.captcha.model.common.ResponseModel;
import com.anji.captcha.model.vo.CaptchaVO;
import com.anji.captcha.properties.AjCaptchaProperties;
import com.anji.captcha.service.CaptchaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * AJ-Captcha 验证码控制器
 * 用于处理滑块验证码的生成和验证
 */
@SaIgnore
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/captcha")
public class AjcaptchaController {

    private final CaptchaService captchaService;

    @Autowired
    private AjCaptchaProperties ajCaptchaProperties;

    /**
     * 获取验证码
     */
    @RequestMapping(value = "/get", method = {RequestMethod.GET, RequestMethod.POST})
    public R<?> get(CaptchaVO captchaVO) {
        log.info("获取验证码请求: captchaType={}", captchaVO.getCaptchaType());

        // 如果captchaType为null，设置默认值为blockPuzzle
        if (captchaVO.getCaptchaType() == null) {
            captchaVO.setCaptchaType("blockPuzzle");
            log.info("设置默认验证码类型为: blockPuzzle");
        }

        ResponseModel response = captchaService.get(captchaVO);
        log.info("验证码响应: repCode={}, repMsg={}", response.getRepCode(), response.getRepMsg());

        // 打印ResponseModel对象的所有属性
        try {
            java.lang.reflect.Field[] fields = response.getClass().getDeclaredFields();
            for (java.lang.reflect.Field field : fields) {
                field.setAccessible(true);
                log.info("ResponseModel.{} = {}", field.getName(), field.get(response));
            }
        } catch (Exception e) {
            log.error("打印ResponseModel属性失败: {}", e.getMessage());
        }

        if ("0000".equals(response.getRepCode())) {
            log.info("验证码生成成功，返回R.ok(response)");
            return R.ok(response.getRepData());
        } else {
            log.info("验证码生成失败，返回R.fail({}, {})");
            return R.fail(Integer.parseInt(response.getRepCode()), response.getRepMsg());
        }
    }

    /**
     * 校验验证码
     */
    @PostMapping("/check")
    public R<?> check(@RequestBody CaptchaVO captchaVO) {
        log.info("校验验证码请求: captchaType={}", captchaVO.getCaptchaType());
        ResponseModel response = captchaService.check(captchaVO);
        log.info("校验验证码响应: repCode={}, repMsg={}", response.getRepCode(), response.getRepMsg());

         if ("0000".equals(response.getRepCode())) {
            return R.ok(response.getRepData());
        } else {
            return R.fail(Integer.parseInt(response.getRepCode()), response.getRepMsg());
        }
    }

    /**
     * 验证码二次校验
     */
    @PostMapping("/verify")
    public R<?> verify(@RequestBody CaptchaVO captchaVO) {
        log.info("二次校验验证码请求: captchaType={}", captchaVO.getCaptchaType());
        ResponseModel response = captchaService.verification(captchaVO);
        log.info("二次校验验证码响应: repCode={}, repMsg={}", response.getRepCode(), response.getRepMsg());

        if ("0000".equals(response.getRepCode())) {
            return R.ok(response.getRepData());
        } else {
            return R.fail(Integer.parseInt(response.getRepCode()), response.getRepMsg());
        }
    }
}

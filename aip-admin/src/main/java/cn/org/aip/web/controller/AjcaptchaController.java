package cn.org.aip.web.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import cn.org.aip.common.core.domain.R;
import cn.org.aip.common.core.utils.StringUtils;
import com.anji.captcha.model.common.ResponseModel;
import com.anji.captcha.model.vo.CaptchaVO;
import com.anji.captcha.properties.AjCaptchaProperties;
import com.anji.captcha.service.CaptchaService;
import com.anji.captcha.service.impl.CaptchaServiceFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * AJ-Captcha 验证码控制器
 * 用于处理滑块拼图 (blockPuzzle) 与汉字点选 (clickWord) 验证码的生成和验证
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
     * 根据请求类型获取对应的 CaptchaService 实例
     */
    private CaptchaService getService(CaptchaVO captchaVO) {
        if (captchaVO != null && StringUtils.isNotBlank(captchaVO.getCaptchaType())) {
            CaptchaService service = CaptchaServiceFactory.instances.get(captchaVO.getCaptchaType());
            if (service != null) {
                return service;
            }
        }
        return captchaService;
    }

    /**
     * 获取验证码 (支持 GET 查询参数或 POST 请求体)
     */
    @RequestMapping(value = "/get", method = {RequestMethod.GET, RequestMethod.POST})
    public R<?> get(@RequestBody(required = false) CaptchaVO bodyVo,
                    @RequestParam(value = "captchaType", required = false) String captchaTypeParam) {
        CaptchaVO captchaVO = bodyVo != null ? bodyVo : new CaptchaVO();
        if (StringUtils.isBlank(captchaVO.getCaptchaType()) && StringUtils.isNotBlank(captchaTypeParam)) {
            captchaVO.setCaptchaType(captchaTypeParam);
        }

        // 如果captchaType为null，设置默认值为clickWord
        if (StringUtils.isBlank(captchaVO.getCaptchaType())) {
            String defaultType = (ajCaptchaProperties != null && ajCaptchaProperties.getType() != null)
                ? ajCaptchaProperties.getType().getCodeValue()
                : "clickWord";
            captchaVO.setCaptchaType(defaultType);
        }
        log.info("获取验证码请求: captchaType={}", captchaVO.getCaptchaType());

        ResponseModel response = getService(captchaVO).get(captchaVO);
        log.info("验证码响应: repCode={}, repMsg={}", response.getRepCode(), response.getRepMsg());

        if ("0000".equals(response.getRepCode())) {
            return R.ok(response.getRepData());
        } else {
            return R.fail(Integer.parseInt(response.getRepCode()), response.getRepMsg());
        }
    }

    /**
     * 校验验证码
     */
    @PostMapping("/check")
    public R<?> check(@RequestBody CaptchaVO captchaVO) {
        log.info("校验验证码请求: captchaType={}", captchaVO.getCaptchaType());
        ResponseModel response = getService(captchaVO).check(captchaVO);
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
        ResponseModel response = getService(captchaVO).verification(captchaVO);
        log.info("二次校验验证码响应: repCode={}, repMsg={}", response.getRepCode(), response.getRepMsg());

        if ("0000".equals(response.getRepCode())) {
            return R.ok(response.getRepData());
        } else {
            return R.fail(Integer.parseInt(response.getRepCode()), response.getRepMsg());
        }
    }
}

package cn.org.aip.web.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.codec.Base64;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.zhyd.oauth.model.AuthResponse;
import me.zhyd.oauth.model.AuthUser;
import me.zhyd.oauth.request.AuthRequest;
import me.zhyd.oauth.utils.AuthStateUtils;
import cn.org.aip.common.core.constant.SystemConstants;
import cn.org.aip.common.core.domain.R;
import cn.org.aip.common.core.domain.model.LoginBody;
import cn.org.aip.common.core.domain.model.RegisterBody;
import cn.org.aip.common.core.domain.model.SocialLoginBody;
import cn.org.aip.common.core.utils.*;
import cn.org.aip.common.encrypt.annotation.ApiEncrypt;
import cn.org.aip.common.json.utils.JsonUtils;
import cn.org.aip.common.ratelimiter.annotation.RateLimiter;
import cn.org.aip.common.ratelimiter.enums.LimitType;
import cn.org.aip.common.satoken.utils.LoginHelper;
import cn.org.aip.common.social.config.properties.SocialLoginConfigProperties;
import cn.org.aip.common.social.config.properties.SocialProperties;
import cn.org.aip.common.social.utils.SocialUtils;
import cn.org.aip.common.sse.dto.SseMessageDto;
import cn.org.aip.common.sse.utils.SseMessageUtils;
import cn.org.aip.common.tenant.helper.TenantHelper;
import cn.org.aip.system.domain.bo.SysTenantBo;
import cn.org.aip.system.domain.vo.SysClientVo;
import cn.org.aip.system.domain.vo.SysTenantVo;
import cn.org.aip.system.service.ISysClientService;
import cn.org.aip.system.service.ISysConfigService;
import cn.org.aip.system.service.ISysSocialService;
import cn.org.aip.system.service.ISysTenantService;
import cn.org.aip.web.domain.vo.LoginTenantVo;
import cn.org.aip.web.domain.vo.LoginVo;
import cn.org.aip.web.domain.vo.TenantListVo;
import cn.org.aip.web.service.IAuthStrategy;
import cn.org.aip.web.service.SysLoginService;
import cn.org.aip.web.service.SysRegisterService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 认证
 *
 * @author Lion Li
 */
@Slf4j
@SaIgnore
@RequiredArgsConstructor
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final SocialProperties socialProperties;
    private final SysLoginService loginService;
    private final SysRegisterService registerService;
    private final ISysConfigService configService;
    private final ISysTenantService tenantService;
    private final ISysSocialService socialUserService;
    private final ISysClientService clientService;
    private final ScheduledExecutorService scheduledExecutorService;


    /**
     * 登录方法
     *
     * @param body 登录信息
     * @return 结果
     */
    @ApiEncrypt
    @PostMapping("/login")
    public R<LoginVo> login(@RequestBody String body) {
        LoginBody loginBody = JsonUtils.parseObject(body, LoginBody.class);
        ValidatorUtils.validate(loginBody);
        // 授权类型和客户端id
        String clientId = loginBody.getClientId();
        String grantType = loginBody.getGrantType();
        SysClientVo client = clientService.queryByClientId(clientId);
        // 查询不到 client 或 client 内不包含 grantType
        if (ObjectUtil.isNull(client) || !StringUtils.contains(client.getGrantType(), grantType)) {
            log.info("客户端id: {} 认证类型：{} 异常!.", clientId, grantType);
            return R.fail(MessageUtils.message("auth.grant.type.error"));
        } else if (!SystemConstants.NORMAL.equals(client.getStatus())) {
            return R.fail(MessageUtils.message("auth.grant.type.blocked"));
        }
        // 校验租户
        loginService.checkTenant(loginBody.getTenantId());
        // 登录
        LoginVo loginVo = IAuthStrategy.login(body, client, grantType);

        Long userId = LoginHelper.getUserId();
        scheduledExecutorService.schedule(() -> {
            SseMessageDto dto = new SseMessageDto();
            dto.setMessage(DateUtils.getTodayHour(new Date()) + "好，欢迎登录应用集成平台");
            dto.setUserIds(List.of(userId));
            SseMessageUtils.publishMessage(dto);
        }, 5, TimeUnit.SECONDS);
        return R.ok(loginVo);
    }

    /**
     * 获取跳转URL
     *
     * @param source 登录来源
     * @return 结果
     */
    @GetMapping("/binding/{source}")
    public R<String> authBinding(@PathVariable("source") String source,
                                 @RequestParam String tenantId, @RequestParam String domain) {
        SocialLoginConfigProperties obj = socialProperties.getType().get(source);
        if (ObjectUtil.isNull(obj)) {
            return R.fail(source + "平台账号暂不支持");
        }
        AuthRequest authRequest = SocialUtils.getAuthRequest(source, socialProperties);
        Map<String, String> map = new HashMap<>();
        map.put("tenantId", tenantId);
        map.put("domain", domain);
        map.put("state", AuthStateUtils.createState());
        String authorizeUrl = authRequest.authorize(Base64.encode(JsonUtils.toJsonString(map), StandardCharsets.UTF_8));
        return R.ok("操作成功", authorizeUrl);
    }

    /**
     * 前端回调绑定授权(需要token)
     *
     * @param loginBody 请求体
     * @return 结果
     */
    @PostMapping("/social/callback")
    public R<Void> socialCallback(@RequestBody SocialLoginBody loginBody) {
        // 校验token
        StpUtil.checkLogin();
        // 获取第三方登录信息
        AuthResponse<AuthUser> response = SocialUtils.loginAuth(
            loginBody.getSource(), loginBody.getSocialCode(),
            loginBody.getSocialState(), socialProperties);
        AuthUser authUserData = response.getData();
        // 判断授权响应是否成功
        if (!response.ok()) {
            return R.fail(response.getMsg());
        }
        loginService.socialRegister(authUserData);
        return R.ok();
    }


    /**
     * 取消授权(需要token)
     *
     * @param socialId socialId
     */
    @DeleteMapping(value = "/unlock/{socialId}")
    public R<Void> unlockSocial(@PathVariable Long socialId) {
        // 校验token
        StpUtil.checkLogin();
        Boolean rows = socialUserService.deleteWithValidById(socialId);
        return rows ? R.ok() : R.fail("取消授权失败");
    }


    /**
     * 退出登录
     */
    @PostMapping("/logout")
    public R<Void> logout() {
        loginService.logout();
        return R.ok("退出成功");
    }

    /**
     * 用户注册
     */
    @ApiEncrypt
    @PostMapping("/register")
    public R<Void> register(@Validated @RequestBody RegisterBody user) {
        if (!configService.selectRegisterEnabled(user.getTenantId())) {
            return R.fail("当前系统没有开启注册功能！");
        }
        registerService.register(user);
        return R.ok();
    }

    /**
     * 登录页面租户下拉框
     *
     * @return 租户列表
     */
    @RateLimiter(time = 60, count = 20, limitType = LimitType.IP)
    @GetMapping("/tenant/list")
    public R<LoginTenantVo> tenantList(HttpServletRequest request) throws Exception {
        // 返回对象
        LoginTenantVo result = new LoginTenantVo();
        boolean enable = TenantHelper.isEnable();
        result.setTenantEnabled(enable);
        // 如果未开启租户这直接返回
        if (!enable) {
            return R.ok(result);
        }

        List<SysTenantVo> tenantList = tenantService.queryList(new SysTenantBo());
        List<TenantListVo> voList = MapstructUtils.convert(tenantList, TenantListVo.class);
        try {
            // 如果只超管返回所有租户
            if (LoginHelper.isSuperAdmin()) {
                result.setVoList(voList);
                return R.ok(result);
            }
        } catch (NotLoginException ignored) {
        }

        // 获取域名
        String host;
        String referer = request.getHeader("referer");
        if (StringUtils.isNotBlank(referer)) {
            // 这里从referer中取值是为了本地使用hosts添加虚拟域名，方便本地环境调试
            host = referer.split("//")[1].split("/")[0];
        } else {
            host = new URL(request.getRequestURL().toString()).getHost();
        }
        // 根据域名进行筛选
        List<TenantListVo> list = StreamUtils.filter(voList, vo ->
            StringUtils.equalsIgnoreCase(vo.getDomain(), host));
        result.setVoList(CollUtil.isNotEmpty(list) ? list : voList);
        return R.ok(result);
    }

}

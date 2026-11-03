package cn.org.aip.web.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import cn.hutool.core.util.URLUtil;
import cn.org.aip.common.core.constant.HttpStatus;
import cn.org.aip.common.core.domain.R;
import cn.org.aip.common.core.utils.MessageUtils;
import cn.org.aip.common.core.utils.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import cn.org.aip.web.domain.bo.SsoChangePasswordBo;
import cn.org.aip.web.domain.bo.SsoExchangeBo;
import cn.org.aip.web.domain.vo.LoginVo;
import cn.org.aip.web.domain.vo.SsoLogoutVo;
import cn.org.aip.web.service.SsoLoginService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * SSO 统一身份认证控制器 (纯 SPA 前端路由落地 + 后端 JSON 换票)
 *
 * @author Michelle.Chung
 */
@Slf4j
@SaIgnore
@Validated
@RestController
@RequestMapping("/auth/sso")
@RequiredArgsConstructor
public class SsoAuthController {

    private final SsoLoginService ssoLoginService;

    /**
     * 前端凭 Keycloak 授权码 code 兑换平台 Sa-Token 令牌 (支持 PKCE 与 Nonce 校验)
     */
    @PostMapping({"/callback"})
    public R<LoginVo> callback(@Validated @RequestBody SsoExchangeBo bo) {
        return R.ok(ssoLoginService.exchangeCode(bo));
    }

    /**
     * 校验平台会话与 Keycloak 会话映射 (前端焦点探活)
     */
    @GetMapping("/session")
    public R<Boolean> session() {
        boolean valid = ssoLoginService.checkSession();
        if (!valid) {
            return R.fail(HttpStatus.UNAUTHORIZED, MessageUtils.message("sso.session.invalid"));
        }
        return R.ok(true);
    }

    /**
     * 平台登出并获取 Keycloak 登出 URL (精准注销服务端与客户端会话)
     */
    @PostMapping("/logout")
    public R<SsoLogoutVo> logout(@RequestParam(value = "redirectUri", required = false) String redirectUri,
                                 @RequestBody(required = false) Map<String, String> body) {
        String targetRedirect = (body != null && body.containsKey("redirectUri")) ? body.get("redirectUri") : redirectUri;
        return R.ok(ssoLoginService.logout(targetRedirect));
    }

    /**
     * Keycloak Back-Channel Logout 服务端登出通知
     */
    @PostMapping("/backchannel-logout")
    public void backchannelLogout(@RequestParam(value = "logout_token", required = false) String logoutToken,
                                  @RequestBody(required = false) String requestBody) {
        String token = logoutToken;
        if (StringUtils.isBlank(token) && StringUtils.isNotBlank(requestBody) && requestBody.contains("logout_token=")) {
            token = URLUtil.decode(StringUtils.substringBetween(requestBody + "&", "logout_token=", "&"));
        }
        ssoLoginService.handleBackChannelLogout(token);
    }

    /**
     * 强制改密或平台改密接口
     */
    @PostMapping("/change-password")
    public R<Void> changePassword(@Validated @RequestBody SsoChangePasswordBo bo) {
        ssoLoginService.changePassword(bo);
        return R.ok();
    }

}

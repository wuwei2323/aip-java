package cn.org.aip.web.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import cn.org.aip.common.core.constant.HttpStatus;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import cn.org.aip.common.core.domain.R;
import cn.org.aip.web.domain.bo.SsoChangePasswordBo;
import cn.org.aip.web.domain.vo.LoginVo;
import cn.org.aip.web.domain.vo.SsoAuthorizeVo;
import cn.org.aip.web.domain.vo.SsoLogoutVo;
import cn.org.aip.web.service.SsoLoginService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

/**
 * SSO 统一身份认证控制器 (全链路 OIDC)
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
     * 获取 Keycloak 授权登录 URL (固定门户 tenant 000000)
     *
     * @param redirect 登录成功后的前端重定向地址
     */
    @GetMapping("/authorize")
    public R<SsoAuthorizeVo> authorize(@RequestParam(value = "redirect", required = false) String redirect) {
        return R.ok(ssoLoginService.getAuthorizeUrl(redirect));
    }

    /**
     * Keycloak 登录回调地址 (成功换票或失败 302 回调前端)
     */
    @GetMapping("/callback")
    public void callback(@RequestParam(value = "code", required = false) String code,
                         @RequestParam(value = "state", required = false) String state,
                         @RequestParam(value = "error", required = false) String error,
                         @RequestParam(value = "error_description", required = false) String errorDescription,
                         HttpServletResponse response) throws IOException {
        ssoLoginService.handleCallback(code, state, error, errorDescription, response);
    }

    /**
     * 前端凭 ticket 兑换平台 Sa-Token 令牌
     */
    @PostMapping("/exchange")
    public R<LoginVo> exchange(@RequestBody(required = false) Map<String, String> body,
                               @RequestParam(value = "ticket", required = false) String ticketParam) {
        String ticket = (body != null && body.containsKey("ticket")) ? body.get("ticket") : ticketParam;
        if (ticket == null || ticket.isBlank()) {
            return R.fail("换票 ticket 不能为空");
        }
        return R.ok(ssoLoginService.exchangeTicket(ticket));
    }

    /**
     * 校验平台会话与 Keycloak 会话映射 (前端焦点探活)
     */
    @GetMapping("/session")
    public R<Boolean> session() {
        boolean valid = ssoLoginService.checkSession();
        if (!valid) {
            return R.fail(HttpStatus.UNAUTHORIZED, "会话已失效");
        }
        return R.ok(true);
    }

    /**
     * 平台登出并获取 Keycloak 登出 URL
     */
    @PostMapping("/logout")
    public R<SsoLogoutVo> logout() {
        return R.ok(ssoLoginService.logout());
    }

    /**
     * Keycloak Back-Channel Logout 服务端登出通知
     */
    @PostMapping("/backchannel-logout")
    public void backchannelLogout(@RequestParam(value = "logout_token", required = false) String logoutToken,
                                  @RequestBody(required = false) String requestBody) {
        String token = logoutToken;
        if (token == null && requestBody != null && requestBody.startsWith("logout_token=")) {
            token = requestBody.substring("logout_token=".length());
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

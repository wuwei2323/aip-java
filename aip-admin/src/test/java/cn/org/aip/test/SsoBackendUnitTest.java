package cn.org.aip.test;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import cn.org.aip.common.core.domain.R;
import cn.org.aip.common.core.exception.ServiceException;
import cn.org.aip.common.json.utils.JsonUtils;
import cn.org.aip.common.sso.config.KeycloakProperties;
import cn.org.aip.common.sso.core.KeycloakJwtVerifier;
import cn.org.aip.common.sso.core.SsoStateStore;
import cn.org.aip.web.controller.AuthController;
import cn.org.aip.web.domain.vo.LoginVo;
import cn.org.aip.web.domain.vo.SsoAuthorizeVo;
import cn.org.aip.web.service.SsoLoginService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.*;

/**
 * 阶段二单测：SSO 后端与 Keycloak OIDC 客户端接入
 */
@SpringBootTest
@DisplayName("阶段二单测：SSO 后端与 Keycloak OIDC 接入")
public class SsoBackendUnitTest {

    @Autowired
    private KeycloakProperties keycloakProperties;

    @Autowired
    private SsoStateStore ssoStateStore;

    @Autowired
    private SsoLoginService ssoLoginService;

    @Autowired
    private AuthController authController;

    @Tag("dev")
    @DisplayName("测试 Keycloak JWT 签名、发行者、受众、时钟偏差与防重放 nonce 校验")
    @Test
    public void testJwtVerifierWithMockRsa() throws Exception {
        // 1. 生成内存 RSA 秘钥对与 JWKSource
        RSAKey rsaKey = new RSAKeyGenerator(2048)
            .keyID("test-key-id")
            .generate();
        JWKSet jwkSet = new JWKSet(rsaKey);
        JWKSource<SecurityContext> jwkSource = new ImmutableJWKSet<>(jwkSet);
        JWSSigner signer = new RSASSASigner(rsaKey);

        KeycloakJwtVerifier verifier = new KeycloakJwtVerifier(keycloakProperties, jwkSource);

        long now = System.currentTimeMillis();
        String validNonce = "nonce-xyz-" + now;
        String clientId = keycloakProperties.getClientId();
        String issuer = keycloakProperties.getIssuer();

        // 2. 构造正常 ID Token
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
            .issuer(issuer)
            .subject("kc-user-001")
            .audience(clientId)
            .claim("azp", clientId)
            .claim("nonce", validNonce)
            .claim("preferred_username", "admin")
            .claim("employee_no", "EMP001")
            .issueTime(new Date(now))
            .notBeforeTime(new Date(now - 1000))
            .expirationTime(new Date(now + 3600 * 1000))
            .build();

        SignedJWT signedJWT = new SignedJWT(
            new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(rsaKey.getKeyID()).build(),
            claimsSet
        );
        signedJWT.sign(signer);
        String tokenString = signedJWT.serialize();

        // 校验成功
        JWTClaimsSet verified = verifier.verifyToken(tokenString, clientId, validNonce);
        Assertions.assertEquals("kc-user-001", verified.getSubject());
        Assertions.assertEquals("admin", verified.getClaim("preferred_username"));

        // 3. 校验过期 Token
        JWTClaimsSet expiredClaims = new JWTClaimsSet.Builder(claimsSet)
            .expirationTime(new Date(now - 120 * 1000)) // 2分钟前过期，超过 60s 容忍
            .build();
        SignedJWT expiredJwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(rsaKey.getKeyID()).build(), expiredClaims);
        expiredJwt.sign(signer);
        Assertions.assertThrows(ServiceException.class, () -> {
            verifier.verifyToken(expiredJwt.serialize(), clientId, validNonce);
        });

        // 4. 校验错误受众 (aud)
        Assertions.assertThrows(ServiceException.class, () -> {
            verifier.verifyToken(tokenString, "other-client", validNonce);
        });

        // 5. 校验错误 Nonce
        Assertions.assertThrows(ServiceException.class, () -> {
            verifier.verifyToken(tokenString, clientId, "wrong-nonce");
        });
    }

    @Tag("dev")
    @DisplayName("测试 OIDC Back-Channel Logout Token 规范校验")
    @Test
    public void testLogoutTokenVerifier() throws Exception {
        RSAKey rsaKey = new RSAKeyGenerator(2048)
            .keyID("test-logout-key")
            .generate();
        JWKSet jwkSet = new JWKSet(rsaKey);
        JWKSource<SecurityContext> jwkSource = new ImmutableJWKSet<>(jwkSet);
        JWSSigner signer = new RSASSASigner(rsaKey);

        KeycloakJwtVerifier verifier = new KeycloakJwtVerifier(keycloakProperties, jwkSource);
        long now = System.currentTimeMillis();
        String clientId = keycloakProperties.getClientId();
        String issuer = keycloakProperties.getIssuer();

        Map<String, Object> events = new HashMap<>();
        events.put(KeycloakJwtVerifier.BACKCHANNEL_LOGOUT_EVENT, Collections.emptyMap());

        // 1. 合法 Logout Token
        JWTClaimsSet logoutClaims = new JWTClaimsSet.Builder()
            .issuer(issuer)
            .subject("kc-user-001")
            .audience(clientId)
            .claim("sid", "session-sid-001")
            .claim("events", events)
            .issueTime(new Date(now))
            .build();

        SignedJWT signedLogout = new SignedJWT(
            new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(rsaKey.getKeyID()).build(),
            logoutClaims
        );
        signedLogout.sign(signer);

        JWTClaimsSet verified = verifier.verifyLogoutToken(signedLogout.serialize(), clientId);
        Assertions.assertEquals("session-sid-001", verified.getClaim("sid"));
        Assertions.assertEquals("kc-user-001", verified.getSubject());

        // 2. 违背规范包含 nonce 的 Logout Token 必须拒绝
        JWTClaimsSet invalidNonceLogout = new JWTClaimsSet.Builder(logoutClaims)
            .claim("nonce", "forbidden-nonce")
            .build();
        SignedJWT invalidJwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(rsaKey.getKeyID()).build(), invalidNonceLogout);
        invalidJwt.sign(signer);

        Assertions.assertThrows(ServiceException.class, () -> {
            verifier.verifyLogoutToken(invalidJwt.serialize(), clientId);
        });

        // 3. 缺少 events 声明必须拒绝
        JWTClaimsSet noEventsLogout = new JWTClaimsSet.Builder()
            .issuer(issuer)
            .audience(clientId)
            .claim("sid", "session-sid-001")
            .build();
        SignedJWT noEventsJwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(rsaKey.getKeyID()).build(), noEventsLogout);
        noEventsJwt.sign(signer);

        Assertions.assertThrows(ServiceException.class, () -> {
            verifier.verifyLogoutToken(noEventsJwt.serialize(), clientId);
        });
    }

    @Tag("dev")
    @DisplayName("测试 SsoStateStore 单次有效性、过期与会话映射绑定")
    @Test
    public void testSsoStateStoreLifecycle() {
        String state = "test_state_" + System.currentTimeMillis();
        SsoStateStore.StateData stateData = SsoStateStore.StateData.builder()
            .tenantId("000000")
            .oidcClientId("000000")
            .redirect("/system/menu")
            .nonce("nonce-123")
            .codeVerifier("verifier-abc")
            .createTime(System.currentTimeMillis())
            .build();

        // 1. 保存 state 并消费
        ssoStateStore.saveState(state, stateData);
        SsoStateStore.StateData consumed = ssoStateStore.consumeState(state);
        Assertions.assertNotNull(consumed);
        Assertions.assertEquals("/system/menu", consumed.getRedirect());

        // 单次有效：再次读取应为空
        Assertions.assertNull(ssoStateStore.consumeState(state));

        // 2. Ticket 换票数据单次有效测试
        String ticket = "test_ticket_" + System.currentTimeMillis();
        SsoStateStore.TicketData ticketData = SsoStateStore.TicketData.builder()
            .tokenValue("token-val-123")
            .loginId("sys_user:1")
            .userId(1L)
            .tenantId("000000")
            .clientId("e5cd7e4891bf95d1d19206ce24a7b32e")
            .expireIn(1800L)
            .build();

        ssoStateStore.saveTicket(ticket, ticketData);
        SsoStateStore.TicketData consumedTicket = ssoStateStore.consumeTicket(ticket);
        Assertions.assertNotNull(consumedTicket);
        Assertions.assertEquals("token-val-123", consumedTicket.getTokenValue());

        // 再次兑换为空
        Assertions.assertNull(ssoStateStore.consumeTicket(ticket));

        // 3. Session 绑定与反查测试
        String sid = "sid-test-" + System.currentTimeMillis();
        String sub = "sub-test-" + System.currentTimeMillis();
        String token = "satoken-val-" + System.currentTimeMillis();

        ssoStateStore.bindSession(sid, sub, token);
        Assertions.assertTrue(ssoStateStore.getTokensBySid(sid).contains(token));
        Assertions.assertTrue(ssoStateStore.getTokensBySub(sub).contains(token));
        Assertions.assertTrue(ssoStateStore.hasSidMapping(token));

        // 清理 sid
        ssoStateStore.removeSid(sid);
        Assertions.assertFalse(ssoStateStore.hasSidMapping(token));
    }

    @Tag("dev")
    @DisplayName("测试 SSO 发起登录 URL 生成与 PKCE 参数")
    @Test
    public void testAuthorizeUrlGeneration() {
        SsoAuthorizeVo vo = ssoLoginService.getAuthorizeUrl("/system/user?id=1");
        Assertions.assertNotNull(vo);
        Assertions.assertNotNull(vo.getAuthorizeUrl());

        String url = vo.getAuthorizeUrl();
        // 校验包含 OIDC 必要参数
        Assertions.assertTrue(url.contains("response_type=code"));
        Assertions.assertTrue(url.contains("client_id=" + keycloakProperties.getClientId()));
        Assertions.assertTrue(url.contains("code_challenge="));
        Assertions.assertTrue(url.contains("code_challenge_method=S256"));
        Assertions.assertTrue(url.contains("state="));
        Assertions.assertTrue(url.contains("nonce="));
    }

    @Tag("dev")
    @DisplayName("测试本地密码登录默认关闭拦截")
    @Test
    public void testLocalLoginDisabledByDefault() {
        Map<String, String> body = new HashMap<>();
        body.put("clientId", "e5cd7e4891bf95d1d19206ce24a7b32e");
        body.put("grantType", "password");
        body.put("tenantId", "000000");
        body.put("username", "admin");
        body.put("password", "123456");

        R<LoginVo> response = authController.login(JsonUtils.toJsonString(body));
        Assertions.assertEquals(R.FAIL, response.getCode());
        Assertions.assertTrue(response.getMsg().contains("统一身份认证"));
    }

}

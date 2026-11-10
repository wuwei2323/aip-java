package cn.org.aip.test;

import cn.dev33.satoken.stp.StpUtil;
import cn.org.aip.common.core.domain.model.LoginUser;
import cn.org.aip.common.core.exception.ServiceException;
import cn.org.aip.common.redis.utils.RedisUtils;
import cn.org.aip.common.satoken.utils.LoginHelper;
import cn.org.aip.common.sso.config.KeycloakProperties;
import cn.org.aip.common.sso.core.KeycloakJwtVerifier;
import cn.org.aip.common.sso.core.SsoStateStore;
import cn.org.aip.web.service.PortalAppProvider;
import cn.org.aip.web.service.SsoLoginService;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/** Isolated signed-token, session-index and logout tests without Redis, database or Keycloak access. */
@Tag("dev")
class PortalSsoLogoutTest {
    private static RSAKey key;
    private static KeycloakJwtVerifier verifier;
    private static KeycloakProperties properties;

    @BeforeAll
    static void initialize() throws Exception {
        SsoUnitTestSupport.initialize();
        key = new RSAKeyGenerator(2048).keyID("portal-logout-test").generate();
        properties = new KeycloakProperties();
        properties.setServerUrl("https://identity.example");
        properties.setRealm("aip");
        verifier = new KeycloakJwtVerifier(properties, new ImmutableJWKSet<>(new JWKSet(key.toPublicJWK())));
    }

    private JWTClaimsSet.Builder claims() {
        return new JWTClaimsSet.Builder().issuer(properties.getIssuer()).audience("000000")
            .subject("user-A").claim("sid", "sid-A").jwtID("logout-1")
            .issueTime(new Date()).claim("events", Map.of(KeycloakJwtVerifier.BACKCHANNEL_LOGOUT_EVENT, Map.of()));
    }

    private String sign(JWTClaimsSet claims) throws Exception {
        SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256)
            .type(new JOSEObjectType("logout+jwt")).keyID(key.getKeyID()).build(), claims);
        jwt.sign(new RSASSASigner(key));
        return jwt.serialize();
    }

    @Test
    void validatesSignedLogoutAndRejectsInvalidIdentityAndEvent() throws Exception {
        assertEquals("sid-A", verifier.verifyLogoutToken(sign(claims().build()), "000000").getClaim("sid"));
        for (JWTClaimsSet invalid : new JWTClaimsSet[] {
            claims().issuer("https://foreign.example/realms/aip").build(),
            claims().audience("other-app").build(),
            claims().claim("nonce", "forbidden").build(),
            claims().claim("events", Map.of(KeycloakJwtVerifier.BACKCHANNEL_LOGOUT_EVENT, "not-object")).build(),
            claims().subject(null).claim("sid", null).build(),
            claims().jwtID(null).build(),
            claims().issueTime(null).build(),
            claims().issueTime(new Date(System.currentTimeMillis() - Duration.ofMinutes(10).toMillis())).build(),
            claims().issueTime(new Date(System.currentTimeMillis() + Duration.ofMinutes(10).toMillis())).build()
        }) {
            String token = sign(invalid);
            assertThrows(ServiceException.class, () -> verifier.verifyLogoutToken(token, "000000"));
        }
    }

    private SsoLoginService service(SsoStateStore store) {
        PortalAppProvider portal = mock(PortalAppProvider.class);
        when(portal.getPortalAppConfig()).thenReturn(PortalAppProvider.PortalAppConfig.builder().appId("000000").build());
        return new SsoLoginService(properties, verifier, null, store, portal, null, null, null);
    }

    @Test
    void sidLogoutDoesNotFallBackToAllSessionsForTheSameUser() throws Exception {
        SsoStateStore store = mock(SsoStateStore.class);
        when(store.getTokensBySid("sid-A")).thenReturn(java.util.List.of("token-A"));
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            service(store).handleBackChannelLogout(sign(claims().build()));
            stp.verify(() -> StpUtil.logoutByTokenValue("token-A"));
            verify(store, never()).getTokensBySub(anyString());
            verify(store, never()).removeSub(anyString());
            verify(store).removeSid("sid-A");
            verify(store).markLogoutProcessed("000000", "logout-1");
        }
    }

    @Test
    void repeatedCompletedLogoutDoesNotTouchNewSessions() throws Exception {
        SsoStateStore store = mock(SsoStateStore.class);
        when(store.isLogoutProcessed("000000", "logout-1")).thenReturn(true);
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            service(store).handleBackChannelLogout(sign(claims().build()));
            stp.verifyNoInteractions();
            verify(store, never()).getTokensBySid(anyString());
        }
    }

    @Test
    void failedLocalInvalidationRemainsRetryable() throws Exception {
        SsoStateStore store = mock(SsoStateStore.class);
        when(store.getTokensBySid("sid-A")).thenReturn(java.util.List.of("token-A"));
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(() -> StpUtil.logoutByTokenValue("token-A")).thenThrow(new IllegalStateException("offline"));
            String token = sign(claims().build());
            assertThrows(IllegalStateException.class, () -> service(store).handleBackChannelLogout(token));
            verify(store, never()).markLogoutProcessed(anyString(), anyString());
            verify(store, never()).removeSid(anyString());
        }
    }

    @Test
    void subjectOnlyLogoutRemainsAnExplicitAccountLevelNotification() throws Exception {
        SsoStateStore store = mock(SsoStateStore.class);
        when(store.getTokensBySub("user-A")).thenReturn(java.util.List.of("token-A"));
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            service(store).handleBackChannelLogout(sign(claims().claim("sid", null).build()));
            stp.verify(() -> StpUtil.logoutByTokenValue("token-A"));
            verify(store).removeSub("user-A");
        }
    }

    @Test
    void missingSsoMappingFailsClosedButNonSsoLoginIsUnaffected() {
        SsoStateStore store = mock(SsoStateStore.class);
        LoginUser user = new LoginUser();
        user.setSsoSid("sid-A");
        user.setSsoSub("user-A");
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class);
             MockedStatic<LoginHelper> helper = mockStatic(LoginHelper.class)) {
            stp.when(StpUtil::isLogin).thenReturn(true);
            stp.when(StpUtil::getTokenValue).thenReturn("token-A");
            helper.when(LoginHelper::getLoginUser).thenReturn(user);
            assertFalse(service(store).checkSession());
            stp.verify(StpUtil::logout);
            user.setSsoSid(null);
            user.setSsoSub(null);
            assertTrue(service(store).checkSession());
        }
    }

    @Test
    void clearingSidRemovesOnlyItsTokensAndPreservesOtherSessionIndexes() {
        Map<String, Object> cache = new HashMap<>();
        try (MockedStatic<RedisUtils> redis = mockStatic(RedisUtils.class)) {
            redis.when(() -> RedisUtils.getCacheObject(anyString())).thenAnswer(call -> cache.get(call.getArgument(0)));
            redis.when(() -> RedisUtils.setCacheObject(anyString(), any(), any(Duration.class)))
                .thenAnswer(call -> { cache.put(call.getArgument(0), call.getArgument(1)); return null; });
            redis.when(() -> RedisUtils.deleteObject(anyString()))
                .thenAnswer(call -> cache.remove(call.getArgument(0)) != null);
            SsoStateStore store = new SsoStateStore();
            store.bindSession("sid-A", "same-user", "token-A");
            store.bindSession("sid-B", "same-user", "token-B");
            assertTrue(store.hasSidMapping("token-A"));
            store.removeSid("sid-A");
            assertFalse(store.hasSidMapping("token-A"));
            assertTrue(store.hasSidMapping("token-B"));
            assertEquals(java.util.Set.of("token-B"), store.getTokensBySub("same-user"));
            assertFalse(store.hasSidMapping("unknown-token"));
        }
    }
}

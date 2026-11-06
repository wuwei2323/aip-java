package cn.org.aip.keycloak.captcha;

import com.anji.captcha.model.vo.PointVO;
import com.anji.captcha.util.AESUtil;
import com.anji.captcha.util.JsonUtil;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.keycloak.common.util.Time;
import org.keycloak.models.SingleUseObjectProvider;
import org.keycloak.util.JsonSerialization;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static cn.org.aip.keycloak.captcha.CaptchaTestSupport.CONTEXT;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/** 使用真实 AJ 引擎覆盖 Resource 安全入口及并发单次出票。 */
@Tag("dev")
class AipCaptchaResourceTest {

    @Test
    void normalRoundTripIssuesOneBoundTicketAndDoesNotCacheResponse() throws Exception {
        var f = new CaptchaTestSupport.Fixture();
        Response response = f.resource.get("blockPuzzle", "tab-1", "client-1");
        assertEquals(200, response.getStatus());
        assertTrue(response.getHeaderString("Cache-Control").contains("no-store"));
        Map<?, ?> data = (Map<?, ?>) envelope(response).get("data");
        String body = solvedBody(f, data, false);
        Response checked = f.resource.check(body, "tab-1", "client-1");
        assertEquals(200, checked.getStatus());
        String ticket = (String) ((Map<?, ?>) envelope(checked).get("data")).get("captchaVerification");
        assertTrue(AipCaptchaTicketService.consume(f.session, CONTEXT, ticket));
        assertFalse(AipCaptchaTicketService.consume(f.session, CONTEXT, ticket));
        assertEquals(400, f.resource.check(body, "tab-1", "client-1").getStatus());
    }

    @Test
    void missingCookieSessionOrUnknownTabCannotObtainOrCheckChallenges() {
        var f = new CaptchaTestSupport.Fixture();
        when(f.manager.getCurrentAuthenticationSession(f.realm, f.client, "tab-1")).thenReturn(null);
        assertEquals(400, f.resource.get("blockPuzzle", "tab-1", "client-1").getStatus());
        assertEquals(400, f.resource.check("{}", "tab-1", "client-1").getStatus());
        assertEquals(400, f.resource.get("blockPuzzle", "arbitrary-tab", "client-1").getStatus());
        verify(f.manager).getCurrentAuthenticationSession(f.realm, f.client, "arbitrary-tab");
    }

    @Test
    void expiredLoginAndDisabledClientAreRejected() {
        var f = new CaptchaTestSupport.Fixture();
        when(f.root.getTimestamp()).thenReturn(Time.currentTime() - 601);
        assertEquals(400, f.resource.get("blockPuzzle", "tab-1", "client-1").getStatus());
        assertEquals(400, f.resource.check("{}", "tab-1", "client-1").getStatus());
        when(f.root.getTimestamp()).thenReturn(Time.currentTime());
        when(f.client.isEnabled()).thenReturn(false);
        assertEquals(400, f.resource.get("blockPuzzle", "tab-1", "client-1").getStatus());
    }

    @Test
    void wrongCoordinatesConsumeChallengeAndCountTowardLock() throws Exception {
        var f = new CaptchaTestSupport.Fixture();
        for (int i = 0; i < 5; i++) {
            Map<?, ?> data = challenge(f);
            assertEquals(400, f.resource.check(solvedBody(f, data, true), "tab-1", "client-1").getStatus());
            assertFalse(f.store.contains(AipCaptchaConstants.CHALLENGE_PREFIX + data.get("token")));
        }
        assertEquals(429, f.resource.get("blockPuzzle", "tab-1", "client-1").getStatus());
    }

    @Test
    void expiredOrCrossRootChallengeCannotMintTicket() throws Exception {
        var f = new CaptchaTestSupport.Fixture();
        String body = solvedBody(f, challenge(f), false);
        f.clock.advance(120);
        assertEquals(400, f.resource.check(body, "tab-1", "client-1").getStatus());
        body = solvedBody(f, challenge(f), false);
        when(f.root.getId()).thenReturn("another-root");
        assertEquals(400, f.resource.check(body, "tab-1", "client-1").getStatus());
        when(f.root.getId()).thenReturn("root-1");
        assertEquals(400, f.resource.check(body, "tab-1", "client-1").getStatus());
    }

    @Test
    void forgedTokensDoNotTriggerCoordinateFailureLock() {
        var f = new CaptchaTestSupport.Fixture();
        String body = "{\"captchaType\":\"blockPuzzle\",\"token\":\"unknown\",\"pointJson\":\"unknown\"}";
        for (int i = 0; i < 5; i++) assertEquals(400, f.resource.check(body, "tab-1", "client-1").getStatus());
        assertTrue(f.limiter.allowGet(f.session, CONTEXT));
    }

    @Test
    void rotatingUntrustedTabsEventuallyHitIpLimitBeforeSessionLookup() {
        var f = new CaptchaTestSupport.Fixture();
        for (int i = 0; i < 120; i++) {
            assertEquals(400, f.resource.get("blockPuzzle", "fake-" + i, "client-1").getStatus());
        }
        assertEquals(429, f.resource.get("blockPuzzle", "fake-extra", "client-1").getStatus());
        verify(f.manager, never()).getCurrentAuthenticationSession(f.realm, f.client, "fake-extra");
    }

    @Test
    void cacheFailureCannotFailOpen() {
        var f = new CaptchaTestSupport.Fixture();
        when(f.session.singleUseObjects()).thenThrow(new IllegalStateException("test cache unavailable"));
        assertEquals(500, f.resource.get("blockPuzzle", "tab-1", "client-1").getStatus());
        assertEquals(500, f.resource.check("{}", "tab-1", "client-1").getStatus());
    }

    @Test
    void parallelChecksCannotEnterEngineTwice() throws Exception {
        var f = new CaptchaTestSupport.Fixture();
        Map<?, ?> data = challenge(f);
        String body = solvedBody(f, data, false);
        String bindingKey = AipCaptchaConstants.CHALLENGE_PREFIX + data.get("token");
        var claimed = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        SingleUseObjectProvider gated = mock(SingleUseObjectProvider.class, delegatesTo(f.store));
        when(gated.remove(anyString())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            Map<String, String> notes = f.store.remove(key);
            if (key.equals(bindingKey) && notes != null) {
                claimed.countDown();
                assertTrue(release.await(5, TimeUnit.SECONDS));
            }
            return notes;
        });
        when(f.session.singleUseObjects()).thenReturn(gated);
        var pool = Executors.newFixedThreadPool(2);
        try {
            var first = pool.submit(() -> f.resource.check(body, "tab-1", "client-1"));
            assertTrue(claimed.await(5, TimeUnit.SECONDS));
            var second = pool.submit(() -> f.resource.check(body, "tab-1", "client-1"));
            assertEquals(400, second.get(5, TimeUnit.SECONDS).getStatus());
            release.countDown();
            Response winner = first.get(5, TimeUnit.SECONDS);
            assertEquals(200, winner.getStatus());
            String ticket = (String) ((Map<?, ?>) envelope(winner).get("data")).get("captchaVerification");
            assertTrue(AipCaptchaTicketService.consume(f.session, CONTEXT, ticket));
        } finally {
            release.countDown();
            pool.shutdownNow();
        }
    }

    private static Map<?, ?> challenge(CaptchaTestSupport.Fixture f) throws Exception {
        Response response = f.resource.get("blockPuzzle", "tab-1", "client-1");
        assertEquals(200, response.getStatus());
        return (Map<?, ?>) envelope(response).get("data");
    }

    private static String solvedBody(CaptchaTestSupport.Fixture f, Map<?, ?> data, boolean wrong) throws Exception {
        String token = (String) data.get("token");
        PointVO target = JsonUtil.parseObject(f.store.get("RUNNING:CAPTCHA:" + token).get("v"), PointVO.class);
        String point = "{\"x\":" + (wrong ? 0 : target.getX()) + ",\"y\":5}";
        String encrypted = AESUtil.aesEncrypt(point, (String) data.get("secretKey"));
        return JsonSerialization.writeValueAsString(Map.of("captchaType", "blockPuzzle", "token", token, "pointJson", encrypted));
    }

    private static Map<?, ?> envelope(Response response) throws Exception {
        return JsonSerialization.readValue((String) response.getEntity(), Map.class);
    }
}

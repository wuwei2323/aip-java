package cn.org.aip.keycloak.captcha;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static cn.org.aip.keycloak.captcha.CaptchaTestSupport.CONTEXT;
import static org.junit.jupiter.api.Assertions.*;

/** IP/会话双层配额、共享锁、容量和过期清理的回归测试。 */
@Tag("dev")
class AipCaptchaRateLimiterTest {

    @Test
    void getAndCheckHaveSeparateSessionQuotas() {
        var f = new CaptchaTestSupport.Fixture();
        for (int i = 0; i < 20; i++) assertTrue(f.limiter.allowGet(f.session, CONTEXT));
        assertFalse(f.limiter.allowGet(f.session, CONTEXT));
        for (int i = 0; i < 10; i++) assertTrue(f.limiter.allowCheck(f.session, CONTEXT));
        assertFalse(f.limiter.allowCheck(f.session, CONTEXT));
        f.clock.advance(60);
        assertTrue(f.limiter.allowGet(f.session, CONTEXT));
        assertTrue(f.limiter.allowCheck(f.session, CONTEXT));
    }

    @Test
    void rotatingTabsCannotResetIpQuota() {
        var f = new CaptchaTestSupport.Fixture();
        for (int i = 0; i < 120; i++) {
            var tab = new AipCaptchaAuthenticationContext("realm-1", "root-1", "client-1", "tab-" + i);
            assertTrue(f.limiter.allowGetByIp("10.1.1.1"));
            assertTrue(f.limiter.allowGet(f.session, tab));
        }
        assertFalse(f.limiter.allowGetByIp("10.1.1.1"));
        assertTrue(f.limiter.allowGetByIp("10.1.1.2"));
        for (int i = 0; i < 60; i++) assertTrue(f.limiter.allowCheckByIp("10.1.1.1"));
        assertFalse(f.limiter.allowCheckByIp("10.1.1.1"));
    }

    @Test
    void sharedFailureLockAppliesToBothEndpointsOnOtherNode() {
        var f = new CaptchaTestSupport.Fixture();
        var nodeB = new AipCaptchaRateLimiter(f.clock, 100);
        for (int i = 0; i < 5; i++) f.limiter.recordFailure(f.session, CONTEXT);
        assertFalse(f.limiter.allowGet(f.session, CONTEXT));
        assertFalse(f.limiter.allowCheck(f.session, CONTEXT));
        assertFalse(nodeB.allowGet(f.session, CONTEXT));
        assertFalse(nodeB.allowCheck(f.session, CONTEXT));
        f.clock.advance(299);
        assertFalse(nodeB.allowGet(f.session, CONTEXT));
        f.clock.advance(1);
        assertTrue(f.limiter.allowGet(f.session, CONTEXT));
        assertTrue(nodeB.allowGet(f.session, CONTEXT));
        assertTrue(nodeB.allowCheck(f.session, CONTEXT));
    }

    @Test
    void isolatedFailuresFromOldWindowDoNotCauseLock() {
        var f = new CaptchaTestSupport.Fixture();
        for (int i = 0; i < 4; i++) f.limiter.recordFailure(f.session, CONTEXT);
        f.clock.advance(60);
        f.limiter.recordFailure(f.session, CONTEXT);
        assertTrue(f.limiter.allowCheck(f.session, CONTEXT));
    }

    @Test
    void expiredFailureBucketsCanBeReclaimedAtCapacity() {
        var f = new CaptchaTestSupport.Fixture();
        var limiter = new AipCaptchaRateLimiter(f.clock, 2);
        limiter.recordFailure(f.session, CONTEXT);
        assertTrue(limiter.allowGetByIp("old-ip"));
        f.clock.advance(61);
        assertTrue(limiter.allowGetByIp("new-ip-1"));
        assertTrue(limiter.allowGetByIp("new-ip-2"));
    }

    @Test
    void capacityDoesNotResetExistingQuotasOrLocks() {
        var f = new CaptchaTestSupport.Fixture();
        var limiter = new AipCaptchaRateLimiter(f.clock, 2);
        for (int i = 0; i < 120; i++) assertTrue(limiter.allowGetByIp("full-ip"));
        assertFalse(limiter.allowGetByIp("full-ip"));
        for (int i = 0; i < 5; i++) limiter.recordFailure(f.session, CONTEXT);
        for (int i = 0; i < 300; i++) assertFalse(limiter.allowGetByIp("new-ip-" + i));
        assertFalse(limiter.allowGetByIp("full-ip"));
        assertFalse(limiter.allowGet(f.session, CONTEXT));
        f.clock.advance(60);
        assertTrue(limiter.allowGetByIp("new-ip"));
        assertFalse(limiter.allowGet(f.session, CONTEXT));
    }

    @Test
    void concurrentRequestsCannotExceedIpQuota() throws Exception {
        var f = new CaptchaTestSupport.Fixture();
        var pool = Executors.newFixedThreadPool(8);
        try {
            var futures = new ArrayList<java.util.concurrent.Future<Boolean>>();
            for (int i = 0; i < 200; i++) futures.add(pool.submit(() -> f.limiter.allowGetByIp("shared-ip")));
            int successes = 0;
            for (var future : futures) if (future.get(5, TimeUnit.SECONDS)) successes++;
            assertEquals(120, successes);
        } finally {
            pool.shutdownNow();
        }
    }
}

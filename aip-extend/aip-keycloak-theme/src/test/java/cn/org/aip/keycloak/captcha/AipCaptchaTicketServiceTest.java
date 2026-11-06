package cn.org.aip.keycloak.captcha;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static cn.org.aip.keycloak.captcha.CaptchaTestSupport.CONTEXT;
import static org.junit.jupiter.api.Assertions.*;

/** 一次性票据与挑战的过期、上下文绑定及并发消费回归。 */
@Tag("dev")
class AipCaptchaTicketServiceTest {

    @Test
    void ticketIsSingleUse() {
        var fixture = new CaptchaTestSupport.Fixture();
        String ticket = AipCaptchaTicketService.issue(fixture.session, CONTEXT);
        assertTrue(AipCaptchaTicketService.consume(fixture.session, CONTEXT, ticket));
        assertFalse(AipCaptchaTicketService.consume(fixture.session, CONTEXT, ticket));
        assertFalse(AipCaptchaTicketService.consume(fixture.session, CONTEXT, "not-a-uuid"));
        assertFalse(AipCaptchaTicketService.consume(fixture.session, null, ticket));
    }

    @Test
    void ticketAndChallengeRejectAllMismatchedBindings() {
        var fixture = new CaptchaTestSupport.Fixture();
        List<AipCaptchaAuthenticationContext> invalid = List.of(
            new AipCaptchaAuthenticationContext("other", "root-1", "client-1", "tab-1"),
            new AipCaptchaAuthenticationContext("realm-1", "other", "client-1", "tab-1"),
            new AipCaptchaAuthenticationContext("realm-1", "root-1", "other", "tab-1"),
            new AipCaptchaAuthenticationContext("realm-1", "root-1", "client-1", "other"));
        for (var context : invalid) {
            String ticket = AipCaptchaTicketService.issue(fixture.session, CONTEXT);
            assertFalse(AipCaptchaTicketService.consume(fixture.session, context, ticket));
            assertFalse(AipCaptchaTicketService.consume(fixture.session, CONTEXT, ticket));
            AipCaptchaChallengeService.register(fixture.session, CONTEXT, "challenge");
            assertFalse(AipCaptchaChallengeService.consume(fixture.session, context, "challenge"));
            assertFalse(AipCaptchaChallengeService.consume(fixture.session, CONTEXT, "challenge"));
        }
    }

    @Test
    void challengeAndTicketRespectTheirOwnTtl() {
        var fixture = new CaptchaTestSupport.Fixture();
        String ticket = AipCaptchaTicketService.issue(fixture.session, CONTEXT);
        AipCaptchaChallengeService.register(fixture.session, CONTEXT, "challenge");
        fixture.clock.advance(120);
        assertFalse(AipCaptchaChallengeService.consume(fixture.session, CONTEXT, "challenge"));
        fixture.clock.advance(59);
        assertTrue(AipCaptchaTicketService.consume(fixture.session, CONTEXT, ticket));
        ticket = AipCaptchaTicketService.issue(fixture.session, CONTEXT);
        fixture.clock.advance(180);
        assertFalse(AipCaptchaTicketService.consume(fixture.session, CONTEXT, ticket));
    }

    @Test
    void concurrentTicketConsumptionHasExactlyOneWinner() throws Exception {
        var fixture = new CaptchaTestSupport.Fixture();
        String ticket = AipCaptchaTicketService.issue(fixture.session, CONTEXT);
        var pool = Executors.newFixedThreadPool(8);
        var ready = new CountDownLatch(8);
        var start = new CountDownLatch(1);
        try {
            var futures = new ArrayList<java.util.concurrent.Future<Boolean>>();
            for (int i = 0; i < 8; i++) {
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    assertTrue(start.await(5, TimeUnit.SECONDS));
                    return AipCaptchaTicketService.consume(fixture.session, CONTEXT, ticket);
                }));
            }
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();
            int successes = 0;
            for (var future : futures) if (future.get(5, TimeUnit.SECONDS)) successes++;
            assertEquals(1, successes);
        } finally {
            start.countDown();
            pool.shutdownNow();
        }
    }
}

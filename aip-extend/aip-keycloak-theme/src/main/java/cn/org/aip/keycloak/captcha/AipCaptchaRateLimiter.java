package cn.org.aip.keycloak.captcha;

import org.keycloak.models.KeycloakSession;

import java.time.Clock;
import java.util.HashMap;
import java.util.Map;

/**
 * 预认证接口的节点内固定窗口限流器，叠加独立 IP 总额度及分布式失败锁。
 *
 * <p>认证会话额度不随客户端 IP 变化；IP 总额度不随 tabId 变化。节点内状态的
 * 更新、过期和容量检查使用同一短临界区，分布式调用始终在临界区外执行。</p>
 *
 * @author AIP
 */
public final class AipCaptchaRateLimiter {

    private static final int GET_LIMIT_PER_WINDOW = 20;
    private static final int CHECK_LIMIT_PER_WINDOW = 10;
    private static final int IP_GET_LIMIT = 120;
    private static final int IP_CHECK_LIMIT = 60;
    private static final int MAX_CHECK_FAILURES = 5;
    private static final long WINDOW_MILLIS = 60_000L;
    private static final long LOCK_MILLIS = 300_000L;
    private static final int MAX_ENTRIES = 10_000;
    private static final String DISTRIBUTED_LOCK_PREFIX = "AIP:CAPTCHA:RL:LOCK:";

    static final AipCaptchaRateLimiter INSTANCE = new AipCaptchaRateLimiter(Clock.systemUTC(), MAX_ENTRIES);

    private final Map<BucketKey, WindowCounter> counters = new HashMap<>();
    private final Clock clock;
    private final int maxEntries;
    private int operations;

    /** 注入时钟和容量，便于无等待地验证时间窗口及独立节点。 */
    AipCaptchaRateLimiter(Clock clock, int maxEntries) {
        this.clock = clock;
        this.maxEntries = maxEntries;
    }

    /** 在认证会话查找前计入 IP 总量，避免创建新标签页绕过请求总额度。 */
    public boolean allowGetByIp(String clientIp) {
        return incrementAndCheck(new BucketKey("IP_GET", ip(clientIp)), IP_GET_LIMIT);
    }

    public boolean allowCheckByIp(String clientIp) {
        return incrementAndCheck(new BucketKey("IP_CHECK", ip(clientIp)), IP_CHECK_LIMIT);
    }

    /** GET/CHECK 都遵守共享失败锁；缓存故障由接口失败关闭。 */
    public boolean allowGet(KeycloakSession session, AipCaptchaAuthenticationContext context) {
        return !isLocked(session, context)
            && incrementAndCheck(new BucketKey("GET", context.rateLimitKey()), GET_LIMIT_PER_WINDOW);
    }

    public boolean allowCheck(KeycloakSession session, AipCaptchaAuthenticationContext context) {
        return !isLocked(session, context)
            && incrementAndCheck(new BucketKey("CHECK", context.rateLimitKey()), CHECK_LIMIT_PER_WINDOW);
    }

    /** 记录当前窗口内失败；达到阈值后写入跨节点共享的 300 秒锁。 */
    public void recordFailure(KeycloakSession session, AipCaptchaAuthenticationContext context) {
        boolean shouldLock;
        synchronized (this) {
            long now = clock.millis();
            WindowCounter counter = getOrCreate(new BucketKey("FAIL", context.rateLimitKey()), now);
            // 无法建立失败计数时直接锁定当前会话，而不是悄悄丢弃失败记录。
            shouldLock = counter == null;
            if (counter != null) {
                counter.resetWindowIfExpired(now);
                counter.count++;
                if (counter.count >= MAX_CHECK_FAILURES) {
                    counter.lockUntil = now + LOCK_MILLIS;
                    shouldLock = true;
                }
            }
        }
        if (shouldLock) {
            session.singleUseObjects().putIfAbsent(distributedLockKey(context), LOCK_MILLIS / 1000);
        }
    }

    private boolean isLocked(KeycloakSession session, AipCaptchaAuthenticationContext context) {
        synchronized (this) {
            WindowCounter counter = counters.get(new BucketKey("FAIL", context.rateLimitKey()));
            if (counter != null && counter.lockUntil > clock.millis()) {
                return true;
            }
        }
        return session.singleUseObjects().contains(distributedLockKey(context));
    }

    private synchronized boolean incrementAndCheck(BucketKey key, int limit) {
        long now = clock.millis();
        WindowCounter counter = getOrCreate(key, now);
        if (counter == null) {
            return false;
        }
        counter.resetWindowIfExpired(now);
        if (counter.count >= limit) {
            return false;
        }
        counter.count++;
        return true;
    }

    /** 调用方持有当前实例锁；容量满时只回收过期项，不清空有效状态。 */
    private WindowCounter getOrCreate(BucketKey key, long now) {
        if ((++operations & 255) == 0) {
            cleanup(now);
        }
        WindowCounter existing = counters.get(key);
        if (existing != null) {
            return existing;
        }
        if (counters.size() >= maxEntries) {
            cleanup(now);
            if (counters.size() >= maxEntries) {
                return null;
            }
        }
        WindowCounter created = new WindowCounter(now);
        counters.put(key, created);
        return created;
    }

    private void cleanup(long now) {
        counters.values().removeIf(counter -> now - counter.windowStart >= WINDOW_MILLIS
            && counter.lockUntil <= now);
    }

    private static String distributedLockKey(AipCaptchaAuthenticationContext context) {
        return DISTRIBUTED_LOCK_PREFIX + context.rateLimitKey();
    }

    private static String ip(String value) {
        return value == null || value.isBlank() ? "unknown" : value;
    }

    /** 不同用途与标识分别作为键字段，避免字符串拼接碰撞。 */
    private record BucketKey(String bucket, String identity) {
    }

    /** 只在限流器实例锁内访问的窗口状态。 */
    private static final class WindowCounter {
        private long windowStart;
        private int count;
        private long lockUntil;

        private WindowCounter(long now) {
            windowStart = now;
        }

        private void resetWindowIfExpired(long now) {
            if (now - windowStart >= WINDOW_MILLIS) {
                windowStart = now;
                count = 0;
            }
        }
    }
}

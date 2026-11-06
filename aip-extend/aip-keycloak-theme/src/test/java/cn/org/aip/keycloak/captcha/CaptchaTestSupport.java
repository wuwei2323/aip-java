package cn.org.aip.keycloak.captcha;

import org.keycloak.common.ClientConnection;
import org.keycloak.common.util.Time;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.SingleUseObjectProvider;
import org.keycloak.services.managers.AuthenticationSessionManager;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.sessions.RootAuthenticationSessionModel;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 共享测试夹具：可控时间、真正原子的带 TTL 缓存及真实认证上下文模型。 */
final class CaptchaTestSupport {
    static final AipCaptchaAuthenticationContext CONTEXT =
        new AipCaptchaAuthenticationContext("realm-1", "root-1", "client-1", "tab-1");

    /** 仅在测试推进时间，不依赖真实等待。 */
    static final class MutableClock extends Clock {
        private long millis = System.currentTimeMillis();

        void advance(long seconds) {
            millis += seconds * 1000;
        }

        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return Instant.ofEpochMilli(millis); }
        @Override public long millis() { return millis; }
    }

    /** 每次操作原子执行且尊重过期时间，可被两个模拟节点共享。 */
    static final class Store implements SingleUseObjectProvider {
        private final Clock clock;
        private final Map<String, Entry> values = new HashMap<>();

        Store(Clock clock) { this.clock = clock; }

        @Override
        public synchronized void put(String key, long lifespanSeconds, Map<String, String> notes) {
            values.put(key, new Entry(Map.copyOf(notes), clock.millis() + lifespanSeconds * 1000));
        }

        @Override
        public synchronized Map<String, String> get(String key) {
            Entry entry = values.get(key);
            if (entry != null && entry.expiresAt > clock.millis()) return entry.notes;
            values.remove(key);
            return null;
        }

        @Override
        public synchronized Map<String, String> remove(String key) {
            Entry entry = values.remove(key);
            return entry != null && entry.expiresAt > clock.millis() ? entry.notes : null;
        }

        @Override
        public synchronized boolean replace(String key, Map<String, String> notes) {
            if (get(key) == null) return false;
            values.put(key, new Entry(Map.copyOf(notes), values.get(key).expiresAt));
            return true;
        }

        @Override
        public synchronized boolean putIfAbsent(String key, long lifespanSeconds) {
            if (get(key) != null) return false;
            put(key, lifespanSeconds, Map.of());
            return true;
        }

        @Override public synchronized boolean contains(String key) { return get(key) != null; }
        @Override public void close() { }

        /** 单次缓存记录。 */
        private record Entry(Map<String, String> notes, long expiresAt) { }
    }

    /** 可切换 tab、根会话及客户端状态的 Keycloak 请求夹具。 */
    static final class Fixture {
        final MutableClock clock = new MutableClock();
        final Store store = new Store(clock);
        final KeycloakSession session = mock(KeycloakSession.class);
        final KeycloakContext keycloakContext = mock(KeycloakContext.class);
        final RealmModel realm = mock(RealmModel.class);
        final ClientModel client = mock(ClientModel.class);
        final RootAuthenticationSessionModel root = mock(RootAuthenticationSessionModel.class);
        final AuthenticationSessionModel auth = mock(AuthenticationSessionModel.class);
        final AuthenticationSessionManager manager = mock(AuthenticationSessionManager.class);
        final AipCaptchaRateLimiter limiter = new AipCaptchaRateLimiter(clock, 10_000);
        final AipCaptchaResource resource = new AipCaptchaResource(session, manager, limiter);

        Fixture() {
            when(session.singleUseObjects()).thenReturn(store);
            when(session.getContext()).thenReturn(keycloakContext);
            when(keycloakContext.getRealm()).thenReturn(realm);
            ClientConnection connection = mock(ClientConnection.class);
            when(keycloakContext.getConnection()).thenReturn(connection);
            when(connection.getRemoteAddr()).thenReturn("10.1.1.1");
            when(realm.getId()).thenReturn(CONTEXT.realmId());
            when(realm.isEnabled()).thenReturn(true);
            when(realm.getAccessCodeLifespanLogin()).thenReturn(600);
            when(realm.getClientByClientId(CONTEXT.clientId())).thenReturn(client);
            when(client.getClientId()).thenReturn(CONTEXT.clientId());
            when(client.isEnabled()).thenReturn(true);
            when(root.getId()).thenReturn(CONTEXT.rootSessionId());
            when(root.getTimestamp()).thenReturn(Time.currentTime());
            when(auth.getRealm()).thenReturn(realm);
            when(auth.getClient()).thenReturn(client);
            when(auth.getParentSession()).thenReturn(root);
            when(auth.getTabId()).thenReturn(CONTEXT.tabId());
            when(manager.getCurrentAuthenticationSession(realm, client, CONTEXT.tabId())).thenReturn(auth);
        }
    }

    private CaptchaTestSupport() { }
}

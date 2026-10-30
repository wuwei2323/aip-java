package cn.org.aip.common.sso.core;

import cn.org.aip.common.core.constant.GlobalConstants;
import cn.org.aip.common.core.utils.StringUtils;
import cn.org.aip.common.redis.utils.RedisUtils;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.Serial;
import java.io.Serializable;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * SSO 状态与会话存储组件（基于 Redis）
 *
 * @author Michelle.Chung
 */
@Slf4j
@Component
public class SsoStateStore {

    private static final String STATE_PREFIX = GlobalConstants.GLOBAL_REDIS_KEY + "sso:state:";
    private static final String TICKET_PREFIX = GlobalConstants.GLOBAL_REDIS_KEY + "sso:ticket:";
    private static final String SID_PREFIX = GlobalConstants.GLOBAL_REDIS_KEY + "sso:sid:";
    private static final String SUB_PREFIX = GlobalConstants.GLOBAL_REDIS_KEY + "sso:sub:";
    private static final String TOKEN_PREFIX = GlobalConstants.GLOBAL_REDIS_KEY + "sso:token:";
    private static final String PWD_RESET_PREFIX = GlobalConstants.GLOBAL_REDIS_KEY + "sso:pwd_reset:";

    private static final Duration STATE_TTL = Duration.ofMinutes(5);
    private static final Duration TICKET_TTL = Duration.ofSeconds(60);
    private static final Duration SESSION_TTL = Duration.ofDays(7);
    private static final Duration PWD_RESET_TTL = Duration.ofMinutes(10);

    /**
     * 保存授权发起状态 (state -> StateData)
     */
    public void saveState(String state, StateData data) {
        if (StringUtils.isBlank(state) || data == null) {
            return;
        }
        RedisUtils.setCacheObject(STATE_PREFIX + state, data, STATE_TTL);
    }

    /**
     * 消费授权状态（单次有效，读取后立即删除）
     */
    public StateData consumeState(String state) {
        if (StringUtils.isBlank(state)) {
            return null;
        }
        String key = STATE_PREFIX + state;
        StateData data = RedisUtils.getCacheObject(key);
        if (data != null) {
            RedisUtils.deleteObject(key);
        }
        return data;
    }

    /**
     * 保存前端换票 Ticket (ticket -> TicketData)
     */
    public void saveTicket(String ticket, TicketData data) {
        if (StringUtils.isBlank(ticket) || data == null) {
            return;
        }
        RedisUtils.setCacheObject(TICKET_PREFIX + ticket, data, TICKET_TTL);
    }

    /**
     * 消费换票 Ticket（单次有效，读取后立即删除）
     */
    public TicketData consumeTicket(String ticket) {
        if (StringUtils.isBlank(ticket)) {
            return null;
        }
        String key = TICKET_PREFIX + ticket;
        TicketData data = RedisUtils.getCacheObject(key);
        if (data != null) {
            RedisUtils.deleteObject(key);
        }
        return data;
    }

    /**
     * 保存首次强制改密 Ticket
     */
    public void savePasswordResetTicket(String ticket, PasswordResetData data) {
        if (StringUtils.isBlank(ticket) || data == null) {
            return;
        }
        RedisUtils.setCacheObject(PWD_RESET_PREFIX + ticket, data, PWD_RESET_TTL);
    }

    /**
     * 读取强制改密 Ticket
     */
    public PasswordResetData getPasswordResetTicket(String ticket) {
        if (StringUtils.isBlank(ticket)) {
            return null;
        }
        return RedisUtils.getCacheObject(PWD_RESET_PREFIX + ticket);
    }

    /**
     * 删除强制改密 Ticket
     */
    public void removePasswordResetTicket(String ticket) {
        if (StringUtils.isNotBlank(ticket)) {
            RedisUtils.deleteObject(PWD_RESET_PREFIX + ticket);
        }
    }

    /**
     * 绑定 Keycloak sid / sub 到 Sa-Token tokenValue
     */
    public void bindSession(String sid, String sub, String tokenValue) {
        if (StringUtils.isBlank(tokenValue)) {
            return;
        }
        if (StringUtils.isNotBlank(sid)) {
            String sidKey = SID_PREFIX + sid;
            Set<String> tokens = RedisUtils.getCacheObject(sidKey);
            if (tokens == null) {
                tokens = new HashSet<>();
            }
            tokens.add(tokenValue);
            RedisUtils.setCacheObject(sidKey, tokens, SESSION_TTL);

            // 反向记录 token -> sid
            RedisUtils.setCacheObject(TOKEN_PREFIX + tokenValue, sid, SESSION_TTL);
        }

        if (StringUtils.isNotBlank(sub)) {
            String subKey = SUB_PREFIX + sub;
            Set<String> tokens = RedisUtils.getCacheObject(subKey);
            if (tokens == null) {
                tokens = new HashSet<>();
            }
            tokens.add(tokenValue);
            RedisUtils.setCacheObject(subKey, tokens, SESSION_TTL);
        }
    }

    /**
     * 根据 Keycloak sid 获取关联的所有本地 token
     */
    public Collection<String> getTokensBySid(String sid) {
        if (StringUtils.isBlank(sid)) {
            return Collections.emptyList();
        }
        Set<String> tokens = RedisUtils.getCacheObject(SID_PREFIX + sid);
        return tokens != null ? tokens : Collections.emptyList();
    }

    /**
     * 根据 Keycloak sub 获取关联的所有本地 token
     */
    public Collection<String> getTokensBySub(String sub) {
        if (StringUtils.isBlank(sub)) {
            return Collections.emptyList();
        }
        Set<String> tokens = RedisUtils.getCacheObject(SUB_PREFIX + sub);
        return tokens != null ? tokens : Collections.emptyList();
    }

    /**
     * 删除 sid 关联映射
     */
    public void removeSid(String sid) {
        if (StringUtils.isNotBlank(sid)) {
            RedisUtils.deleteObject(SID_PREFIX + sid);
        }
    }

    /**
     * 删除 sub 关联映射
     */
    public void removeSub(String sub) {
        if (StringUtils.isNotBlank(sub)) {
            RedisUtils.deleteObject(SUB_PREFIX + sub);
        }
    }

    /**
     * 检查 token 对应的 Keycloak 会话映射是否存在
     */
    public boolean hasSidMapping(String tokenValue) {
        if (StringUtils.isBlank(tokenValue)) {
            return false;
        }
        String sid = RedisUtils.getCacheObject(TOKEN_PREFIX + tokenValue);
        if (StringUtils.isBlank(sid)) {
            return true; // 未启用或没有sid关联则不做强校验
        }
        Set<String> tokens = RedisUtils.getCacheObject(SID_PREFIX + sid);
        return tokens != null && tokens.contains(tokenValue);
    }

    /**
     * 授权中继状态数据
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StateData implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private String tenantId;
        private String oidcClientId;
        private String redirect;
        private String nonce;
        private String codeVerifier;
        private String callbackUrl;
        private String frontendBaseUrl;
        private Long createTime;
    }

    /**
     * 前端换票数据
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TicketData implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private String tokenValue;
        private String loginId;
        private String redirect;
        private Long userId;
        private String tenantId;
        private String clientId;
        private Long expireIn;
    }

    /**
     * 强制改密中继数据
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PasswordResetData implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private Long userId;
        private String username;
        private String tenantId;
        private String redirect;
    }

}

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
 * SSO 会话与强制改密凭证存储组件（基于 Redis）
 *
 * @author Michelle.Chung
 */
@Slf4j
@Component
public class SsoStateStore {

    private static final String SID_PREFIX = GlobalConstants.GLOBAL_REDIS_KEY + "sso:sid:";
    private static final String SUB_PREFIX = GlobalConstants.GLOBAL_REDIS_KEY + "sso:sub:";
    private static final String TOKEN_PREFIX = GlobalConstants.GLOBAL_REDIS_KEY + "sso:token:";
    private static final String TOKEN_SUB_PREFIX = GlobalConstants.GLOBAL_REDIS_KEY + "sso:token_sub:";
    private static final String PWD_RESET_PREFIX = GlobalConstants.GLOBAL_REDIS_KEY + "sso:pwd_reset:";
    private static final String LOGOUT_PREFIX = GlobalConstants.GLOBAL_REDIS_KEY + "sso:logout_done:";

    private static final Duration SESSION_TTL = Duration.ofDays(7);
    private static final Duration PWD_RESET_TTL = Duration.ofMinutes(10);

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
            RedisUtils.setCacheObject(TOKEN_SUB_PREFIX + tokenValue, sub, SESSION_TTL);
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
            for (String token : Set.copyOf(getTokensBySid(sid))) {
                unbindSession(token);
            }
            RedisUtils.deleteObject(SID_PREFIX + sid);
        }
    }

    /**
     * 删除 sub 关联映射
     */
    public void removeSub(String sub) {
        if (StringUtils.isNotBlank(sub)) {
            for (String token : Set.copyOf(getTokensBySub(sub))) {
                unbindSession(token);
            }
            RedisUtils.deleteObject(SUB_PREFIX + sub);
        }
    }

    /** 删除单个本地会话的正向和反向索引，不影响该用户的其他 SSO 会话。 */
    public void unbindSession(String tokenValue) {
        if (StringUtils.isBlank(tokenValue)) {
            return;
        }
        String sid = RedisUtils.getCacheObject(TOKEN_PREFIX + tokenValue);
        String sub = RedisUtils.getCacheObject(TOKEN_SUB_PREFIX + tokenValue);
        removeFromIndex(SID_PREFIX, sid, tokenValue);
        removeFromIndex(SUB_PREFIX, sub, tokenValue);
        RedisUtils.deleteObject(TOKEN_PREFIX + tokenValue);
        RedisUtils.deleteObject(TOKEN_SUB_PREFIX + tokenValue);
    }

    private void removeFromIndex(String prefix, String identity, String tokenValue) {
        if (StringUtils.isBlank(identity)) {
            return;
        }
        Set<String> tokens = RedisUtils.getCacheObject(prefix + identity);
        if (tokens != null) {
            Set<String> remaining = new HashSet<>(tokens);
            remaining.remove(tokenValue);
            if (remaining.isEmpty()) {
                RedisUtils.deleteObject(prefix + identity);
            } else {
                RedisUtils.setCacheObject(prefix + identity, remaining, SESSION_TTL);
            }
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
            return false;
        }
        Set<String> tokens = RedisUtils.getCacheObject(SID_PREFIX + sid);
        return tokens != null && tokens.contains(tokenValue);
    }

    /** 重复的已完成通知不能再次影响后来建立的本地会话。 */
    public boolean isLogoutProcessed(String audience, String jwtId) {
        return Boolean.TRUE.equals(RedisUtils.getCacheObject(LOGOUT_PREFIX + audience + ":" + jwtId));
    }

    public void markLogoutProcessed(String audience, String jwtId) {
        RedisUtils.setCacheObject(LOGOUT_PREFIX + audience + ":" + jwtId, true, Duration.ofMinutes(10));
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
    }

}

package cn.org.aip.common.ajcaptcha.spi;

import com.anji.captcha.service.CaptchaCacheService;
import cn.org.aip.common.redis.utils.RedisUtils;
import org.springframework.stereotype.Component;

/**
 * AJ-Captcha Redis缓存服务实现
 * 分布式环境下使用Redis存储验证码相关信息
 */
@Component
public class AjCaptchaCacheServiceRedisImpl implements CaptchaCacheService {

    @Override
    public void set(String key, String value, long expiresInSeconds) {
        RedisUtils.setCacheObject(key, value, java.time.Duration.ofSeconds(expiresInSeconds));
    }

    @Override
    public boolean exists(String key) {
        return RedisUtils.hasKey(key);
    }

    @Override
    public void delete(String key) {
        RedisUtils.deleteObject(key);
    }

    @Override
    public String get(String key) {
        return RedisUtils.getCacheObject(key);
    }

    @Override
    public Long increment(String key, long val) {
        // 使用Redis原子操作实现计数器功能
        String currentValue = get(key);
        Long newValue;
        if (currentValue == null || currentValue.isEmpty()) {
            // 如果键不存在，设置初始值并返回
            newValue = val;
            set(key, String.valueOf(newValue), 300); // 默认5分钟过期
        } else {
            // 如果键存在，递增并返回新值
            newValue = Long.parseLong(currentValue) + val;
            set(key, String.valueOf(newValue), 300); // 默认5分钟过期
        }
        return newValue;
    }

    @Override
    public String type() {
        return "redis";
    }
}
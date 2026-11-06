package cn.org.aip.keycloak.captcha;

import com.anji.captcha.model.common.ResponseModel;
import com.anji.captcha.model.vo.CaptchaVO;
import com.anji.captcha.model.vo.PointVO;
import com.anji.captcha.service.CaptchaCacheService;
import com.anji.captcha.service.CaptchaService;
import com.anji.captcha.service.impl.CaptchaServiceFactory;
import com.anji.captcha.util.AESUtil;
import com.anji.captcha.util.JsonUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 验证 AJ-Captcha 拼图滑块引擎可在 Keycloak 插件 JAR 内独立运行。
 *
 * <p>使用内存缓存替代 Keycloak 会话缓存，覆盖图片生成、目标坐标读取、
 * 前端等价 AES 加密与坐标校验的完整往返。</p>
 */
@Tag("dev")
class AipCaptchaEngineTest {

    @BeforeAll
    static void setUp() {
        System.setProperty("java.awt.headless", "true");
    }

    @AfterEach
    void restoreKeycloakCache() {
        CaptchaServiceFactory.cacheService.put(AipCaptchaConstants.CACHE_TYPE, new KeycloakCaptchaCacheService());
    }

    @Test
    void blockPuzzleRoundTripShouldSucceed() throws Exception {
        AipCaptchaEngine.initialize();
        CaptchaService service = AipCaptchaEngine.getService();
        assertNotNull(service, "AJ-Captcha engine should initialize with bundled assets");

        InMemoryCacheService cache = new InMemoryCacheService();
        CaptchaServiceFactory.cacheService.put(AipCaptchaConstants.CACHE_TYPE, cache);

        CaptchaVO getRequest = new CaptchaVO();
        getRequest.setCaptchaType(AipCaptchaConstants.CAPTCHA_TYPE);
        ResponseModel getResult = service.get(getRequest);

        assertEquals("0000", getResult.getRepCode());
        CaptchaVO captcha = (CaptchaVO) getResult.getRepData();
        assertNotNull(captcha.getOriginalImageBase64());
        assertNotNull(captcha.getJigsawImageBase64());
        assertNotNull(captcha.getToken());
        assertNotNull(captcha.getSecretKey());

        String cachedAnswer = cache.get("RUNNING:CAPTCHA:" + captcha.getToken());
        assertNotNull(cachedAnswer, "AJ-Captcha should cache the target point");
        PointVO target = JsonUtil.parseObject(cachedAnswer, PointVO.class);
        assertNotNull(target);

        // 与主题 JS 完全一致：AES(JSON.stringify({x, y:5}), secretKey)
        String pointJson = AESUtil.aesEncrypt(
            "{\"x\":" + target.getX() + ",\"y\":5}", captcha.getSecretKey());

        CaptchaVO checkRequest = new CaptchaVO();
        checkRequest.setCaptchaType(AipCaptchaConstants.CAPTCHA_TYPE);
        checkRequest.setToken(captcha.getToken());
        checkRequest.setPointJson(pointJson);
        ResponseModel checkResult = service.check(checkRequest);

        assertEquals("0000", checkResult.getRepCode());
    }

    /**
     * 仅用于测试的内存缓存实现。
     */
    private static final class InMemoryCacheService implements CaptchaCacheService {

        private final Map<String, String> values = new ConcurrentHashMap<>();

        @Override
        public void set(String key, String value, long expiresInSeconds) {
            values.put(key, value);
        }

        @Override
        public boolean exists(String key) {
            return values.containsKey(key);
        }

        @Override
        public void delete(String key) {
            values.remove(key);
        }

        @Override
        public String get(String key) {
            return values.get(key);
        }

        @Override
        public String type() {
            return AipCaptchaConstants.CACHE_TYPE;
        }
    }
}

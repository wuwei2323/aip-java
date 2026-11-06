package cn.org.aip.keycloak.captcha;

import com.anji.captcha.model.common.CaptchaTypeEnum;
import com.anji.captcha.model.common.Const;
import com.anji.captcha.service.CaptchaService;
import com.anji.captcha.service.impl.CaptchaServiceFactory;
import com.anji.captcha.util.ImageUtils;
import org.jboss.logging.Logger;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Semaphore;

/**
 * AJ-Captcha 拼图滑块引擎的初始化与访问入口。
 *
 * <p>复用 AJ-Captcha 核心包（Apache-2.0）完成图片生成、坐标校验与 token 生命周期管理，
 * 仅额外提供 Keycloak 缓存适配与并发保护。初始化失败时引擎保持不可用，登录侧会失败关闭。</p>
 *
 * @author AIP
 */
public final class AipCaptchaEngine {

    private static final Logger LOG = Logger.getLogger(AipCaptchaEngine.class);

    private static final String[] ORIGINAL_IMAGES = {"1.jpg", "2.png", "3.png", "4.png", "5.png", "6.png"};
    private static final String[] SLIDING_BLOCK_IMAGES = {"1.png", "2.png", "3.png", "4.png"};

    /** 限制单节点同时生成验证码图片的请求数，避免预认证接口成为 CPU 放大器 */
    private static final Semaphore IMAGE_PERMITS = new Semaphore(8);

    private static volatile CaptchaService captchaService;
    private static volatile boolean initialized;

    /**
     * 初始化一次 AJ-Captcha 引擎。可重复调用。
     */
    public static void initialize() {
        if (initialized) {
            return;
        }
        synchronized (AipCaptchaEngine.class) {
            if (initialized) {
                return;
            }
            ClassLoader previous = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(AipCaptchaEngine.class.getClassLoader());
            try {
                Map<String, String> original = loadImages("original", ORIGINAL_IMAGES);
                Map<String, String> slidingBlock = loadImages("slidingBlock", SLIDING_BLOCK_IMAGES);
                if (original.isEmpty() || slidingBlock.isEmpty()) {
                    throw new IllegalStateException("CAPTCHA base images are missing from the provider JAR");
                }
                ImageUtils.cacheBootImage(original, slidingBlock, Collections.emptyMap());

                Properties properties = buildProperties();

                KeycloakCaptchaCacheService cacheService = new KeycloakCaptchaCacheService();
                CaptchaServiceFactory.cacheService.put(cacheService.type(), cacheService);

                AipBlockPuzzleCaptchaService service = new AipBlockPuzzleCaptchaService();
                service.init(properties);
                CaptchaServiceFactory.instances.put(service.captchaType(), service);

                LOG.infof("CAPTCHA assets loaded: original=%d, slidingBlock=%d", original.size(), slidingBlock.size());
                BufferedImage selfCheck = ImageUtils.getOriginal();
                LOG.infof("CAPTCHA image cache self-check: %s, ImageUtils CL=%s, service CL=%s",
                    selfCheck != null, ImageUtils.class.getClassLoader(), service.getClass().getClassLoader());
                if (selfCheck == null) {
                    throw new IllegalStateException("CAPTCHA image cache self-check failed");
                }

                captchaService = service;
                initialized = true;
                LOG.info("AJ-Captcha blockPuzzle engine initialized");
            } catch (Throwable t) {
                captchaService = null;
                LOG.error("Failed to initialize AJ-Captcha blockPuzzle engine; CAPTCHA will fail closed", t);
            } finally {
                Thread.currentThread().setContextClassLoader(previous);
            }
        }
    }

    /**
     * 获取已初始化的验证码服务。
     *
     * @return 服务实例，初始化失败时为 {@code null}
     */
    public static CaptchaService getService() {
        initialize();
        return captchaService;
    }

    /**
     * 尝试获取图片生成许可。
     *
     * @return 是否获取成功
     */
    public static boolean tryAcquireImagePermit() {
        return IMAGE_PERMITS.tryAcquire();
    }

    /**
     * 释放图片生成许可。
     */
    public static void releaseImagePermit() {
        IMAGE_PERMITS.release();
    }

    private static Properties buildProperties() {
        Properties properties = new Properties();
        properties.setProperty(Const.CAPTCHA_CACHETYPE, AipCaptchaConstants.CACHE_TYPE);
        properties.setProperty(Const.CAPTCHA_TYPE, CaptchaTypeEnum.BLOCKPUZZLE.getCodeValue());
        // 拼图滑块不需要水印文字；使用 JDK 逻辑字体，避免分发 GPL 字体文件
        properties.setProperty(Const.CAPTCHA_WATER_MARK, "");
        properties.setProperty(Const.CAPTCHA_WATER_FONT, "SansSerif");
        properties.setProperty(Const.CAPTCHA_FONT_TYPE, "SansSerif");
        properties.setProperty(Const.CAPTCHA_SLIP_OFFSET, "5");
        // 注意：AJ-Captcha 1.3.0 中该参数语义与直觉相反。
        // true 时跳过内部 ImageUtils.cacheImage()/defaultImages 扫描；
        // false 反而会触发默认图片加载，而 Shade JAR 已排除 defaultImages/**。
        properties.setProperty(Const.CAPTCHA_INIT_ORIGINAL, "true");
        properties.setProperty(Const.CAPTCHA_AES_STATUS, "true");
        // 关闭 AJ-Captcha 自带限流，使用 Keycloak 侧带锁定窗口的分布式限流器
        properties.setProperty(Const.REQ_FREQUENCY_LIMIT_ENABLE, "0");
        return properties;
    }

    private static Map<String, String> loadImages(String directory, String[] files) {
        Map<String, String> images = new HashMap<>(files.length);
        ClassLoader classLoader = AipCaptchaEngine.class.getClassLoader();
        for (String file : files) {
            String resource = "captcha/" + directory + "/" + file;
            try (InputStream input = classLoader.getResourceAsStream(resource)) {
                if (input == null) {
                    LOG.warnf("CAPTCHA asset not found: %s", resource);
                    continue;
                }
                images.put(file, Base64.getEncoder().encodeToString(input.readAllBytes()));
            } catch (IOException e) {
                LOG.warnf("Failed to read CAPTCHA asset %s: %s", resource, e.getMessage());
            }
        }
        return images;
    }

    private AipCaptchaEngine() {
    }
}

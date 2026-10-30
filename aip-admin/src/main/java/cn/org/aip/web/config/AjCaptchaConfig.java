package cn.org.aip.web.config;

import com.anji.captcha.model.common.CaptchaTypeEnum;
import com.anji.captcha.model.common.Const;
import com.anji.captcha.properties.AjCaptchaProperties;
import com.anji.captcha.service.CaptchaService;
import com.anji.captcha.service.impl.CaptchaServiceFactory;
import com.anji.captcha.util.FileCopyUtils;
import com.anji.captcha.util.ImageUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.util.*;

/**
 * AJ-Captcha 配置类
 * 手动配置 CaptchaService Bean
 */
@Configuration
@ConditionalOnClass(CaptchaService.class)
@EnableConfigurationProperties(AjCaptchaProperties.class)
public class AjCaptchaConfig {

    @Bean
    public CaptchaService captchaService() {
        Properties config = new Properties();
        config.put(Const.CAPTCHA_CACHETYPE, "redis");
        config.put(Const.CAPTCHA_WATER_MARK, "");
        config.put(Const.CAPTCHA_FONT_TYPE, "WenQuanZhengHei.ttf");
        // 默认汉字点选验证码
        config.put(Const.CAPTCHA_TYPE, CaptchaTypeEnum.CLICKWORD.getCodeValue());
        config.put(Const.CAPTCHA_INIT_ORIGINAL, "true");
        initializeBaseMap();
        CaptchaService clickService = CaptchaServiceFactory.getInstance(config);

        // 同步初始化滑块拼图验证码 (blockPuzzle)
        Properties blockConfig = new Properties();
        blockConfig.putAll(config);
        blockConfig.put(Const.CAPTCHA_TYPE, CaptchaTypeEnum.BLOCKPUZZLE.getCodeValue());
        CaptchaServiceFactory.getInstance(blockConfig);

        return clickService;
    }

    private static void initializeBaseMap() {
        ImageUtils.cacheBootImage(
            getResourcesImagesFile("classpath:captcha" + "/original/*.png"),
            getResourcesImagesFile("classpath:captcha" + "/slidingBlock/*.png"),
            getResourcesImagesFile("classpath:captcha" + "/pic-click/*.png")
        );
    }

    public static Map<String, String> getResourcesImagesFile(String path) {
        Map<String, String> imgMap = new HashMap<>(16);
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

        try {
            Resource[] resources = resolver.getResources(path);
            Resource[] var4 = resources;
            int var5 = resources.length;

            for(int var6 = 0; var6 < var5; ++var6) {
                Resource resource = var4[var6];
                byte[] bytes = FileCopyUtils.copyToByteArray(resource.getInputStream());
                String string = Base64.getEncoder().encodeToString(bytes);
                String filename = resource.getFilename();
                imgMap.put(filename, string);
            }
        } catch (Exception var11) {
            var11.printStackTrace();
        }

        return imgMap;
    }


}

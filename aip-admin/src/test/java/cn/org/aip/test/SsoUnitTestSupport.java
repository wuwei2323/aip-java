package cn.org.aip.test;

import cn.org.aip.common.core.utils.SpringUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.linpeilie.Converter;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.support.ResourceBundleMessageSource;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Minimal utility bootstrap for isolated unit tests; never starts the application or connects to a database. */
final class SsoUnitTestSupport {

    private static boolean initialized;

    private SsoUnitTestSupport() {
    }

    /** Initializes only the static JSON, mapping and message utilities used by these tests. */
    static synchronized void initialize() {
        if (initialized) {
            return;
        }
        DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
        factory.registerSingleton("objectMapper", new ObjectMapper());
        factory.registerSingleton("redissonClient", mock(org.redisson.api.RedissonClient.class));
        ResourceBundleMessageSource messages = new ResourceBundleMessageSource();
        messages.setBasename("i18n/messages");
        messages.setDefaultEncoding("UTF-8");
        factory.registerSingleton("messageSource", messages);
        Converter converter = mock(Converter.class);
        when(converter.convert(any(Object.class), any(Class.class))).thenAnswer(invocation -> {
            Object source = invocation.getArgument(0);
            Class<?> target = invocation.getArgument(1);
            Object result = target.getDeclaredConstructor().newInstance();
            BeanUtils.copyProperties(source, result);
            return result;
        });
        factory.registerSingleton("converter", converter);
        new SpringUtils().postProcessBeanFactory(factory);
        initialized = true;
    }
}

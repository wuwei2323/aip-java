package cn.org.aip;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.metrics.buffering.BufferingApplicationStartup;

/**
 * 启动程序
 *
 * @author Lion Li
 */

@SpringBootApplication
public class AipApplication {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(AipApplication.class);
        application.setApplicationStartup(new BufferingApplicationStartup(2048));
        application.run(args);
        System.out.println("AIP应用集成平台启动成功（基于RuoYi-Vue-Plus 5）");
    }

}

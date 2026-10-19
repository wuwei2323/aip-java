package cn.org.aopa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.metrics.buffering.BufferingApplicationStartup;

/**
 * 启动程序
 *
 * @author Lion Li
 */

@SpringBootApplication
public class AopaApplication {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(AopaApplication.class);
        application.setApplicationStartup(new BufferingApplicationStartup(2048));
        application.run(args);
        System.out.println("AOPA会议系统启动成功（基于RuoYi-Vue-Plus 5）");
    }

}

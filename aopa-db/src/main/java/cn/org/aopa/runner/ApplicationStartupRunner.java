package cn.org.aopa.runner;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import cn.org.aopa.common.utils.IdGeneratorUtil;

@Component
public class ApplicationStartupRunner implements CommandLineRunner {
    @Override
    public void run(String... args) throws Exception {
        // 工作节点ID（确保集群中唯一）
        IdGeneratorUtil.initIdGenerator((short) 1);
    }
}
package com.kaoyan.platform.common.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    /**
     * 全项目时间统一从该 Clock 获取，不直接 new Date() / LocalDate.now()。
     * 单测里可注入固定 Clock，让艾宾浩斯的周期推进可以在毫秒级验证。
     */
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}

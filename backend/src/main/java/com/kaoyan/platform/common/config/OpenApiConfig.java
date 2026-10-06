package com.kaoyan.platform.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI().info(new Info()
                .title("考研高数学习平台 API")
                .description("v1 骨架：所有业务接口返回 code=5010，等待逐层实现")
                .version("v1"));
    }
}

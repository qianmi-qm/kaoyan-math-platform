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
                .description("考研数学学习平台：认证、题库、练习、报告、复习、AI 拍照搜题")
                .version("v3.10"));
    }
}

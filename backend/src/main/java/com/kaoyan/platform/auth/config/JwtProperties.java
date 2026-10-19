package com.kaoyan.platform.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    /** HMAC 密钥，至少 32 字节；生产必须用环境变量覆盖 */
    private String secret;

    private long expireHours = 24;
}

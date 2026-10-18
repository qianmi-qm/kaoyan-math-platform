package com.kaoyan.platform.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.ai")
public class AiProperties {

    /** 总开关；关闭时直接走解析降级 */
    private boolean enabled = true;

    /** OpenAI 兼容接口的 base url，例如 https://api.deepseek.com/v1 */
    private String baseUrl = "";

    /** 为空时直接降级，不发起网络请求 */
    private String apiKey = "";

    private String model = "deepseek-chat";

    private int timeoutSeconds = 60;

    /** 拼进 prompt 的上下文字符上限，控制 token */
    private int maxContextChars = 4000;
}

package com.kaoyan.platform.ai.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

class AiChatClientTest {

    @Test
    @DisplayName("chat completions URL 去掉尾部斜杠")
    void buildsUrl() {
        AiChatClient client = new AiChatClient(WebClient.builder());
        assertEquals("https://api.deepseek.com/chat/completions",
                client.completionsUrl("https://api.deepseek.com/"));
    }
}

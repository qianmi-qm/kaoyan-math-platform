package com.kaoyan.platform.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.time.Duration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * OpenAI 兼容接口的最小客户端。
 * 只负责：拼 URL、带 Bearer、发 JSON；不掺业务。
 * 讲解用流式 WebClient，拍照搜题/补答案用 postJson。
 */
@Component
public class AiChatClient {

    private final WebClient.Builder builder;

    public AiChatClient(WebClient.Builder builder) {
        this.builder = builder;
    }

    public WebClient webClient(String baseUrl, String apiKey) {
        return builder.baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .build();
    }

    public String completionsUrl(String baseUrl) {
        return baseUrl.replaceAll("/+$", "") + "/chat/completions";
    }

    public JsonNode postJson(String baseUrl, String apiKey, Object body, Duration timeout) {
        return webClient(baseUrl, apiKey)
                .post()
                .uri(URI.create(completionsUrl(baseUrl)))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block(timeout);
    }
}

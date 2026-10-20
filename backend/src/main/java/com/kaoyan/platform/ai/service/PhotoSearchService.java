package com.kaoyan.platform.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaoyan.platform.ai.config.AiProperties;
import com.kaoyan.platform.ai.vo.PhotoSearchResult;
import com.kaoyan.platform.common.exception.BusinessException;
import com.kaoyan.platform.common.result.ErrorCode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/**
 * 拍照搜题：图片校验 → 扣配额 → vision → JSON 解析 → 答案补算。
 *
 * 关键约束：
 * - 图片只在内存里转 data URL，不落盘
 * - 配额在调模型前扣；失败退款
 * - 视觉模型只识别题干没给答案时，用文本模型补一次
 * - 历史保存不在这里：用户确认后由 PhotoHistoryService 落库
 */
@Service
public class PhotoSearchService {

    private static final Logger log = LoggerFactory.getLogger(PhotoSearchService.class);
    private static final int MAX_OPTIONS = 8;
    private static final int MAX_STEPS = 12;

    private final AiProperties properties;
    private final AiQuotaService quotaService;
    private final ObjectMapper objectMapper;
    private final AiChatClient aiChatClient;

    public PhotoSearchService(AiProperties properties, AiQuotaService quotaService,
                              ObjectMapper objectMapper, AiChatClient aiChatClient) {
        this.properties = properties;
        this.quotaService = quotaService;
        this.objectMapper = objectMapper;
        this.aiChatClient = aiChatClient;
    }

    public PhotoSearchResult search(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请上传题目图片");
        }
        if (file.getSize() > properties.getVisionMaxImageMb() * 1024L * 1024L) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "图片不能超过 " + properties.getVisionMaxImageMb() + "MB");
        }
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "图片读取失败");
        }
        String mime = detectMime(bytes);
        if (mime == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "仅支持 JPG / PNG / WebP 图片");
        }

        String baseUrl = firstText(properties.getVisionBaseUrl(), properties.getBaseUrl());
        String apiKey = firstText(properties.getVisionApiKey(), properties.getApiKey());
        String model = firstText(properties.getVisionModel(), "gpt-4o-mini");
        if (!properties.isVisionEnabled() || !StringUtils.hasText(apiKey)
                || !StringUtils.hasText(baseUrl)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "拍照搜题未配置视觉模型（请设置 AI_VISION_API_KEY / AI_VISION_BASE_URL）");
        }

        if (!quotaService.tryConsume(userId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "今日 AI 次数已用完");
        }
        try {
            JsonNode response = callVision(baseUrl, apiKey, model, mime, bytes);
            int tokens = response.path("usage").path("total_tokens").asInt(0);
            quotaService.addTokens(userId, tokens);
            PhotoSearchResult result = parse(response, model);
            result.setQuotaRemaining(quotaService.quota(userId).getRemaining());
            return result;
        } catch (BusinessException e) {
            quotaService.refund(userId);
            throw e;
        } catch (Exception e) {
            quotaService.refund(userId);
            log.warn("拍照搜题失败：{}", e.getMessage());
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "拍照搜题失败，请稍后重试");
        }
    }

    private JsonNode callVision(String baseUrl, String apiKey, String model,
                                String mime, byte[] bytes) {
        String dataUrl = "data:" + mime + ";base64,"
                + Base64.getEncoder().encodeToString(bytes);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("temperature", 0.1);
        body.put("max_tokens", 4096);
        body.put("messages", List.of(Map.of(
                "role", "user",
                "content", List.of(
                        Map.of("type", "text", "text", prompt()),
                        Map.of("type", "image_url", "image_url", Map.of("url", dataUrl))
                ))));

        try {
            return aiChatClient.postJson(baseUrl, apiKey, body,
                    Duration.ofSeconds(properties.getVisionTimeoutSeconds()));
        } catch (WebClientResponseException e) {
            log.warn("vision API 返回 {}：{}", e.getStatusCode(),
                    e.getResponseBodyAsString().substring(0,
                            Math.min(300, e.getResponseBodyAsString().length())));
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "视觉模型调用失败");
        }
    }

    private PhotoSearchResult parse(JsonNode response, String model) throws Exception {
        JsonNode choices = response.path("choices");
        if (!choices.isArray() || choices.isEmpty()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "视觉模型没有返回结果");
        }
        String content = choices.get(0).path("message").path("content").asText("").trim();
        if (!StringUtils.hasText(content)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                    "模型没有返回识别内容，请重拍或稍后重试");
        }
        String json = stripFence(content);
        JsonNode node;
        try {
            node = objectMapper.readTree(json);
        } catch (Exception e) {
            // 模型没按 JSON 返回时，至少把原文给用户，不假装结构化识别成功
            return PhotoSearchResult.builder()
                    .recognized(true)
                    .content("")
                    .answer(content)
                    .steps(List.of())
                    .knowledgePoints(List.of())
                    .confidence(0)
                    .warnings(List.of("模型未返回结构化 JSON，以下为原始输出"))
                    .model(model)
                    .build();
        }
        String recognizedContent = node.path("content").asText("").trim();
        boolean recognized = node.path("recognized").asBoolean(true)
                && StringUtils.hasText(recognizedContent);
        if (!recognized) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "没有识别出数学题，请换一张更清晰、只拍题目的图片");
        }
        List<String> warnings = strings(node.path("warnings"), 8);
        if ("length".equals(choices.get(0).path("finish_reason").asText(""))) {
            warnings.add("模型输出被截断，答案可能不完整");
        }
        String answer = node.path("answer").asText("").trim();
        if (!StringUtils.hasText(answer)) {
            answer = supplementAnswer(recognizedContent);
            if (StringUtils.hasText(answer)) {
                warnings.add("最终答案由文本模型补算，请核对");
            } else {
                warnings.add("模型没有返回最终答案，请重拍或查看解析步骤");
            }
        }
        return PhotoSearchResult.builder()
                .recognized(true)
                .content(recognizedContent)
                .options(strings(node.path("options"), MAX_OPTIONS))
                .answer(answer)
                .steps(strings(node.path("steps"), MAX_STEPS))
                .knowledgePoints(strings(node.path("knowledgePoints"), 10))
                .confidence(node.path("confidence").asDouble(0))
                .warnings(warnings)
                .model(model)
                .build();
    }

    /** 视觉模型只识别出题干、没给答案时，用文本模型补一次最终答案。 */
    private String supplementAnswer(String content) {
        String base = properties.getBaseUrl();
        String key = properties.getApiKey();
        String model = properties.getModel();
        if (!StringUtils.hasText(base) || !StringUtils.hasText(key)
                || !StringUtils.hasText(model)) {
            return null;
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("temperature", 0);
        body.put("max_tokens", 600);
        body.put("messages", List.of(
                Map.of("role", "system", "content", "你是考研数学老师。只输出最终答案，不要解析过程。"),
                Map.of("role", "user", "content", "求下面题目的最终答案：\n" + content)
        ));
        try {
            JsonNode response = aiChatClient.postJson(base, key, body,
                    Duration.ofSeconds(properties.getTimeoutSeconds()));
            if (response == null) return null;
            String result = response.path("choices").path(0).path("message")
                    .path("content").asText("").trim();
            return StringUtils.hasText(result) ? result : null;
        } catch (Exception e) {
            log.warn("拍照搜题答案补算失败：{}", e.getMessage());
            return null;
        }
    }

    private List<String> strings(JsonNode node, int max) {        List<String> result = new ArrayList<>();
        if (node != null && node.isArray()) {
            for (JsonNode item : node) {
                if (result.size() >= max) break;
                String value = item.asText("").trim();
                if (!value.isEmpty()) result.add(value);
            }
        }
        return result;
    }

    private String stripFence(String content) {
        String value = content.trim();
        if (value.startsWith("```")) {
            int firstNewline = value.indexOf('\n');
            if (firstNewline >= 0) value = value.substring(firstNewline + 1);
            if (value.endsWith("```")) value = value.substring(0, value.length() - 3);
        }
        int start = value.indexOf('{');
        int end = value.lastIndexOf('}');
        return start >= 0 && end > start ? value.substring(start, end + 1) : value;
    }

    private String detectMime(byte[] bytes) {
        if (bytes.length >= 3
                && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8
                && (bytes[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        if (bytes.length >= 8
                && (bytes[0] & 0xFF) == 0x89 && bytes[1] == 'P' && bytes[2] == 'N'
                && bytes[3] == 'G') {
            return "image/png";
        }
        if (bytes.length >= 12
                && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') {
            return "image/webp";
        }
        return null;
    }

    private String firstText(String first, String fallback) {
        return StringUtils.hasText(first) ? first : fallback;
    }

    private String prompt() {
        return """
                你是考研数学老师，正在做拍照搜题。请识别图片中的数学题目并给出解析。
                只输出一个 JSON 对象，不要 markdown 代码块，不要多余解释：
                {
                  "recognized": true,
                  "content": "题干，公式用 $...$ 包裹",
                  "options": ["A. ...", "B. ..."],
                  "answer": "最终答案",
                  "steps": ["第一步...", "第二步..."],
                  "knowledgePoints": ["极限", "洛必达法则"],
                  "confidence": 0.0-1.0,
                  "warnings": ["看不清的部分"]
                }
                规则：
                1. 图片不是数学题时 recognized=false，其余字段留空。
                2. 看不清的符号/上下标写入 warnings，不要编造。
                3. 选择题 options 保留选项；填空题/大题 options 为空数组。
                4. 解析要适合考研学生，步骤简洁但完整。
                5. 所有数学公式用 LaTeX，行内 $...$。
                """;
    }
}

package com.kaoyan.platform.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaoyan.platform.ai.config.AiProperties;
import com.kaoyan.platform.ai.entity.AiExplanation;
import com.kaoyan.platform.ai.mapper.AiExplanationMapper;
import com.kaoyan.platform.ai.support.AnswerFingerprint;
import com.kaoyan.platform.common.support.CurrentUser;
import com.kaoyan.platform.practice.entity.PracticeRecord;
import com.kaoyan.platform.practice.mapper.PracticeRecordMapper;
import com.kaoyan.platform.question.entity.KnowledgePoint;
import com.kaoyan.platform.question.entity.Question;
import com.kaoyan.platform.question.mapper.KnowledgePointMapper;
import com.kaoyan.platform.question.mapper.QuestionMapper;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.Disposable;

/**
 * 错题 AI 讲解：优先读已生成讲解 → 无 key/关闭时降级解析文本 → 否则调 OpenAI 兼容 SSE。
 * 讲解成功落库，同一用户同一题同一答案二次查看不再消耗 token。
 */
@Service
public class AiExplanationService {

    private static final Logger log = LoggerFactory.getLogger(AiExplanationService.class);
    private static final String DONE = "[DONE]";

    private final PracticeRecordMapper practiceRecordMapper;
    private final QuestionMapper questionMapper;
    private final KnowledgePointMapper knowledgePointMapper;
    private final AiExplanationMapper aiExplanationMapper;
    private final AiProperties properties;
    private final CurrentUser currentUser;
    private final ObjectMapper objectMapper;
    private final WebClient.Builder webClientBuilder;
    private final ExecutorService executor = Executors.newFixedThreadPool(4, runnable -> {
        Thread thread = new Thread(runnable, "ai-explain");
        thread.setDaemon(true);
        return thread;
    });

    public AiExplanationService(PracticeRecordMapper practiceRecordMapper,
                                QuestionMapper questionMapper,
                                KnowledgePointMapper knowledgePointMapper,
                                AiExplanationMapper aiExplanationMapper,
                                AiProperties properties,
                                CurrentUser currentUser,
                                ObjectMapper objectMapper,
                                WebClient.Builder webClientBuilder) {
        this.practiceRecordMapper = practiceRecordMapper;
        this.questionMapper = questionMapper;
        this.knowledgePointMapper = knowledgePointMapper;
        this.aiExplanationMapper = aiExplanationMapper;
        this.properties = properties;
        this.currentUser = currentUser;
        this.objectMapper = objectMapper;
        this.webClientBuilder = webClientBuilder;
    }

    public void stream(Long recordId, SseEmitter emitter) {
        emitter.onTimeout(emitter::complete);
        emitter.onError(error -> emitter.complete());
        executor.submit(() -> doStream(recordId, emitter));
    }

    private void doStream(Long recordId, SseEmitter emitter) {
        try {
            Long userId = currentUser.getUserId();
            PracticeRecord record = practiceRecordMapper.selectById(recordId);
            if (record == null || !record.getUserId().equals(userId)) {
                sendError(emitter, "做题记录不存在");
                return;
            }
            Question question = questionMapper.selectById(record.getQuestionId());
            if (question == null) {
                sendError(emitter, "题目不存在");
                return;
            }

            String answerHash = AnswerFingerprint.hash(record.getSubmittedAnswer());
            AiExplanation cached = aiExplanationMapper.selectByUserQuestionAndAnswer(
                    userId, question.getId(), answerHash);
            if (cached != null && StringUtils.hasText(cached.getContent())) {
                streamText(cached.getContent(), emitter);
                return;
            }

            String fallback = buildFallback(question);
            if (!properties.isEnabled()
                    || !StringUtils.hasText(properties.getBaseUrl())
                    || !StringUtils.hasText(properties.getApiKey())) {
                streamText(fallback, emitter);
                return;
            }
            streamFromLlm(buildPrompt(record, question), fallback, record, question, answerHash, emitter);
        } catch (Exception e) {
            log.error("AI 讲解失败", e);
            sendError(emitter, "AI 讲解暂时不可用");
        }
    }

    private void streamFromLlm(String prompt, String fallback, PracticeRecord record,
                               Question question, String answerHash, SseEmitter emitter) {
        StringBuilder answer = new StringBuilder();
        AtomicBoolean finished = new AtomicBoolean(false);
        String model = properties.getModel();

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("stream", true);
        body.put("temperature", 0.3);
        body.put("messages", List.of(Map.of("role", "user", "content", prompt)));

        Disposable subscription = webClientBuilder.build()
                .post()
                .uri(java.net.URI.create(chatCompletionsUrl()))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToFlux(new ParameterizedTypeReference<ServerSentEvent<String>>() {
                })
                .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                .subscribe(event -> {
                    String data = event.data();
                    if (data == null || data.isBlank()) {
                        return;
                    }
                    if (DONE.equals(data.trim())) {
                        // 有的 OpenAI 兼容实现发完 [DONE] 不立刻断连接，主动收尾
                        if (finished.compareAndSet(false, true)) {
                            if (answer.length() > 0) {
                                save(record, question, answerHash, answer.toString(), model);
                                finish(emitter);
                            } else {
                                streamText(fallback, emitter);
                            }
                        }
                        return;
                    }
                    String delta = extractDelta(data);
                    if (delta == null || delta.isEmpty()) {
                        return;
                    }
                    answer.append(delta);
                    send(emitter, delta);
                }, error -> {
                    if (!finished.compareAndSet(false, true)) {
                        return;
                    }
                    log.warn("LLM 流式调用失败：{}", error.getMessage());
                    if (answer.length() == 0) {
                        streamText(fallback, emitter);
                    } else {
                        send(emitter, "\n\n（AI 中断，先看参考解析）\n" + fallback);
                        finish(emitter);
                    }
                }, () -> {
                    if (!finished.compareAndSet(false, true)) {
                        return;
                    }
                    if (answer.length() > 0) {
                        save(record, question, answerHash, answer.toString(), model);
                        finish(emitter);
                    } else {
                        streamText(fallback, emitter);
                    }
                });

        emitter.onCompletion(subscription::dispose);
        emitter.onTimeout(subscription::dispose);
    }

    private String chatCompletionsUrl() {
        return properties.getBaseUrl().replaceAll("/+$", "") + "/chat/completions";
    }

    private String extractDelta(String data) {
        try {
            JsonNode root = objectMapper.readTree(data);
            JsonNode content = root.path("choices").path(0).path("delta").path("content");
            return content.isMissingNode() || content.isNull() ? null : content.asText();
        } catch (Exception e) {
            return null;
        }
    }

    private String buildPrompt(PracticeRecord record, Question question) {
        StringBuilder kpNames = new StringBuilder();
        List<Long> kpIds = questionMapper.selectKpIds(question.getId());
        if (!kpIds.isEmpty()) {
            for (KnowledgePoint kp : knowledgePointMapper.selectByIds(kpIds)) {
                if (kpNames.length() > 0) {
                    kpNames.append("、");
                }
                kpNames.append(kp.getName());
            }
        }
        String prompt = "你是考研数学辅导老师。请针对下面这道题，用高中生能懂的话讲三点：\n"
                + "1. 用户可能为什么错；2. 正确思路；3. 易错点。\n"
                + "不要编造教材原文，不确定就明确说不确定。\n\n"
                + "题目：" + question.getContent() + "\n"
                + "选项：" + (question.getOptions() == null ? "无" : question.getOptions()) + "\n"
                + "用户答案：" + (record.getSubmittedAnswer() == null ? "未记录" : record.getSubmittedAnswer()) + "\n"
                + "正确答案：" + question.getAnswer() + "\n"
                + "参考解析：" + (question.getAnalysis() == null ? "无" : question.getAnalysis()) + "\n"
                + "知识点：" + (kpNames.length() == 0 ? "未标注" : kpNames) + "\n";
        int max = Math.max(500, properties.getMaxContextChars());
        return prompt.length() <= max ? prompt : prompt.substring(0, max);
    }

    private String buildFallback(Question question) {
        String analysis = StringUtils.hasText(question.getAnalysis())
                ? question.getAnalysis() : "这道题暂时没有解析。";
        return "AI 讲解暂时不可用，先看参考解析：\n" + analysis
                + "\n\n正确答案：" + question.getAnswer();
    }

    private void save(PracticeRecord record, Question question, String answerHash,
                      String content, String model) {
        try {
            AiExplanation explanation = new AiExplanation();
            explanation.setUserId(record.getUserId());
            explanation.setQuestionId(question.getId());
            explanation.setAnswerHash(answerHash);
            explanation.setPracticeRecordId(record.getId());
            explanation.setContent(content);
            explanation.setModel(model);
            aiExplanationMapper.upsert(explanation);
        } catch (Exception e) {
            log.warn("AI 讲解落库失败：{}", e.getMessage());
        }
    }

    private void streamText(String text, SseEmitter emitter) {
        int chunkSize = 40;
        for (int i = 0; i < text.length(); i += chunkSize) {
            if (!send(emitter, text.substring(i, Math.min(text.length(), i + chunkSize)))) {
                return;
            }
        }
        finish(emitter);
    }

    private boolean send(SseEmitter emitter, String text) {
        try {
            emitter.send(SseEmitter.event().data(text));
            return true;
        } catch (IOException | IllegalStateException e) {
            emitter.complete();
            return false;
        }
    }

    private void finish(SseEmitter emitter) {
        try {
            emitter.send(SseEmitter.event().name("done").data(DONE));
            emitter.complete();
        } catch (IOException | IllegalStateException e) {
            emitter.complete();
        }
    }

    private void sendError(SseEmitter emitter, String message) {
        try {
            emitter.send(SseEmitter.event().name("error").data(message));
        } catch (IOException | IllegalStateException ignored) {
            // 客户端可能已断开
        } finally {
            emitter.complete();
        }
    }

    @PreDestroy
    void shutdown() {
        executor.shutdownNow();
    }
}

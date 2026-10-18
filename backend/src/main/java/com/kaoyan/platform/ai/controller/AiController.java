package com.kaoyan.platform.ai.controller;

import com.kaoyan.platform.ai.service.AiExplanationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Tag(name = "AI 讲解")
@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiExplanationService aiExplanationService;

    public AiController(AiExplanationService aiExplanationService) {
        this.aiExplanationService = aiExplanationService;
    }

    @Operation(summary = "错题 AI 讲解：SSE 流式返回，无 key/超时会降级为解析文本")
    @PostMapping(value = "/explain/{recordId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter explain(@PathVariable Long recordId) {
        SseEmitter emitter = new SseEmitter(120_000L);
        aiExplanationService.stream(recordId, emitter);
        return emitter;
    }
}

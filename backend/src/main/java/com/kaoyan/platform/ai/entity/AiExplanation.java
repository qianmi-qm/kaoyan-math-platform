package com.kaoyan.platform.ai.entity;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class AiExplanation {

    private Long id;
    private Long userId;
    private Long questionId;
    private String answerHash;
    private Long practiceRecordId;
    private String content;
    private String model;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

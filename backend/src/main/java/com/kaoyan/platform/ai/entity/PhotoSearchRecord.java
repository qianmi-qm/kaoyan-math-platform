package com.kaoyan.platform.ai.entity;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class PhotoSearchRecord {

    private Long id;
    private Long userId;
    private String content;
    private String optionsJson;
    private String answer;
    private String stepsJson;
    private String knowledgePointsJson;
    private Double confidence;
    private String warningsJson;
    private String model;
    private Boolean favorite;
    private LocalDateTime createdAt;
}

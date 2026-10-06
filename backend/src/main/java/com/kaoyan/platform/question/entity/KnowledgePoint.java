package com.kaoyan.platform.question.entity;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class KnowledgePoint {

    private Long id;
    private String name;
    private Long parentId;
    private LocalDateTime createdAt;
}

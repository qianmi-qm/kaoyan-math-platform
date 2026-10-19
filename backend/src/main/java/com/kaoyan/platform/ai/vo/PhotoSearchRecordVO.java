package com.kaoyan.platform.ai.vo;

import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PhotoSearchRecordVO {

    private Long id;
    private String content;
    private List<String> options;
    private String answer;
    private List<String> steps;
    private List<String> knowledgePoints;
    private Double confidence;
    private List<String> warnings;
    private String model;
    private boolean favorite;
    private LocalDateTime createdAt;
}

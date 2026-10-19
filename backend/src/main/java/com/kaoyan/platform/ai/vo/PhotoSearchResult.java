package com.kaoyan.platform.ai.vo;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PhotoSearchResult {

    private boolean recognized;
    private String content;
    private List<String> options;
    private String answer;
    private List<String> steps;
    private List<String> knowledgePoints;
    private double confidence;
    private List<String> warnings;
    private String model;
    private int quotaRemaining;
    private Long recordId;
    private boolean favorite;
}

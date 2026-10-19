package com.kaoyan.platform.ai.dto;

import java.util.List;
import lombok.Data;

@Data
public class PhotoSearchSaveRequest {

    private String content;
    private List<String> options;
    private String answer;
    private List<String> steps;
    private List<String> knowledgePoints;
    private Double confidence;
    private List<String> warnings;
    private String model;
    private boolean favorite;
}

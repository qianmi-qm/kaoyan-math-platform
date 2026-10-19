package com.kaoyan.platform.ai.entity;

import java.time.LocalDate;
import lombok.Data;

@Data
public class AiUsage {

    private Long id;
    private Long userId;
    private LocalDate usageDate;
    private Integer requestCount;
    private Integer tokenEstimate;
}

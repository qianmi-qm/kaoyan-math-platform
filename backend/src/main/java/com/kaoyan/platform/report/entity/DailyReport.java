package com.kaoyan.platform.report.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class DailyReport {

    private Long id;
    private Long userId;
    private LocalDate reportDate;
    private Integer totalCount;
    private Integer submittedCount;
    private Integer gradedCount;
    private Integer correctCount;
    private Integer avgDurationSec;
    private Double overallMastery;
    private String kpMastery;   // JSON 快照
    private String weakPoints;  // JSON 快照
    private LocalDateTime createdAt;
}

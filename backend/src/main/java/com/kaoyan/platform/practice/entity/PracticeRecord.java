package com.kaoyan.platform.practice.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class PracticeRecord {

    private Long id;
    private Long userId;
    private Long questionId;
    private Boolean isCorrect;      // 大题未自评时为 null
    private Integer durationSec;    // 后端计算，防前端造假
    private LocalDateTime startedAt;
    private LocalDateTime submittedAt;
    private LocalDate practiceDate;
}

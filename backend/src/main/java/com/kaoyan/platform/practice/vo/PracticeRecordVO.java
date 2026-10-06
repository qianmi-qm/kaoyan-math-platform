package com.kaoyan.platform.practice.vo;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
public class PracticeRecordVO {

    private Long recordId;
    private Long questionId;
    private Integer type;
    private String content;
    private String options;
    private Integer difficulty;
    private List<Long> kpIds;
    private List<String> kpNames;

    private Boolean isCorrect;
    private Integer durationSec;
    private LocalDateTime startedAt;
    private LocalDateTime submittedAt;

    /** 仅提交后（判分/自评场景）返回 */
    private String correctAnswer;
    private String analysis;
}

package com.kaoyan.platform.review.vo;

import java.time.LocalDate;
import lombok.Data;

@Data
public class ReviewTaskVO {

    private Long reviewId;
    private Long questionId;
    private Integer type;
    private String content;
    private String options;
    private Integer difficulty;
    private Integer stage;
    private LocalDate nextReviewDate;
    private Boolean overdue;   // nextReviewDate 早于今天 → true（顺延补做）
}

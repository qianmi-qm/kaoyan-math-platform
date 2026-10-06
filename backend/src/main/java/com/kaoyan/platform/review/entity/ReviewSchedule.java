package com.kaoyan.platform.review.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ReviewSchedule {

    public static final int STATUS_ACTIVE = 0;
    public static final int STATUS_DONE = 1;

    private Long id;
    private Long userId;
    private Long questionId;
    private Integer stage;           // 当前周期下标 0~4
    private LocalDate nextReviewDate;
    private Integer status;          // 0待复习 1已完成
    private LocalDateTime completedAt;
}

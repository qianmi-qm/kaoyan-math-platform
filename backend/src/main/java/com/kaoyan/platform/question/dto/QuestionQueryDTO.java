package com.kaoyan.platform.question.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class QuestionQueryDTO {

    /** 可选：按知识点过滤 */
    private Long kpId;

    @Min(1)
    private int page = 1;

    @Min(1) @Max(100)
    private int size = 20;
}

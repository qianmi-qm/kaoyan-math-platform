package com.kaoyan.platform.question.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;

@Data
public class QuestionCreateDTO {

    @NotNull(message = "题型不能为空（1单选 2填空 3大题）")
    @Min(1) @Max(3)
    private Integer type;

    @NotBlank(message = "题干不能为空")
    private String content;

    /** 仅单选题必填，JSON 数组字符串 */
    private String options;

    @NotBlank(message = "答案不能为空")
    private String answer;

    private String analysis;

    @NotNull(message = "难度不能为空（1易 2中 3难）")
    @Min(1) @Max(3)
    private Integer difficulty;

    @NotEmpty(message = "至少关联一个知识点")
    private List<Long> kpIds;
}

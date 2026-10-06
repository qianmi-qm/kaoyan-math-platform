package com.kaoyan.platform.practice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SubmitAnswerDTO {

    @NotBlank(message = "答案不能为空（大题可填任意占位内容后自评）")
    private String answer;

    /** 仅大题(type=3)使用：true/false=自评对错，null=暂不自评 */
    private Boolean selfEval;
}

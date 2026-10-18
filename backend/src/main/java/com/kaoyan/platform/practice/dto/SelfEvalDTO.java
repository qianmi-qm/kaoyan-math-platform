package com.kaoyan.platform.practice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SelfEvalDTO {

    /** 大题自评：true=答对，false=答错 */
    @NotNull(message = "correct 不能为空")
    private Boolean correct;
}

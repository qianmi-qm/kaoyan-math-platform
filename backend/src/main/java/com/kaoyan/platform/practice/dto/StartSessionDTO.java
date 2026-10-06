package com.kaoyan.platform.practice.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class StartSessionDTO {

    /** KP=按知识点 / RANDOM=随机 / WRONG=错题重练 */
    @NotBlank(message = "选题方式不能为空：KP / RANDOM / WRONG")
    private String mode;

    /** mode=KP 时必填 */
    private Long kpId;

    @Min(1) @Max(50)
    private int count = 10;
}

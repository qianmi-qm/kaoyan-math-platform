package com.kaoyan.platform.practice.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class StartSessionDTO {

    /** KP=按知识点 / RANDOM=随机 / WRONG=错题重练 / QUESTION=指定题 */
    @NotBlank(message = "选题方式不能为空：KP / RANDOM / WRONG / QUESTION")
    private String mode;

    /** mode=KP 时必填 */
    private Long kpId;

    /** mode=QUESTION 时必填：从拍照搜题同类题进入 */
    private Long questionId;

    @Min(1) @Max(50)
    private int count = 10;
}

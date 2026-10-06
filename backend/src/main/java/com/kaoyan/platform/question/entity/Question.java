package com.kaoyan.platform.question.entity;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class Question {

    private Long id;
    private Integer type;          // 1单选 2填空 3大题
    private String content;        // 题干 LaTeX 原文
    private String options;        // JSON 数组字符串，如 ["A. ...","B. ..."]
    private String answer;
    private String analysis;       // 解析 LaTeX 原文
    private Integer difficulty;    // 1易 2中 3难
    private String contentHash;    // MD5(题干)，导入幂等
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

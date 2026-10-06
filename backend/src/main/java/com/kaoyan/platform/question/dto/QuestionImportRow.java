package com.kaoyan.platform.question.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * Excel 导入模板的行结构。模板列顺序即 @ExcelProperty index 顺序：
 * 题干 | 题型(1单选/2填空/3大题) | 选项(JSON,可空) | 答案 | 解析(可空) | 难度(1-3) | 知识点(逗号分隔)
 */
@Data
public class QuestionImportRow {

    @ExcelProperty(index = 0)
    private String content;

    @ExcelProperty(index = 1)
    private Integer type;

    @ExcelProperty(index = 2)
    private String options;

    @ExcelProperty(index = 3)
    private String answer;

    @ExcelProperty(index = 4)
    private String analysis;

    @ExcelProperty(index = 5)
    private Integer difficulty;

    @ExcelProperty(index = 6)
    private String kpNames;
}

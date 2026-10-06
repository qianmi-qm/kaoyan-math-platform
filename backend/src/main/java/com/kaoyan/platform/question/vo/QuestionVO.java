package com.kaoyan.platform.question.vo;

import java.util.List;
import lombok.Data;

@Data
public class QuestionVO {

    private Long id;
    private Integer type;
    private String content;
    private String options;

    /** 列表/练习场景不返回；详情与复盘场景返回 */
    private String answer;
    private String analysis;

    private Integer difficulty;
    private List<Long> kpIds;
    private List<String> kpNames;
}

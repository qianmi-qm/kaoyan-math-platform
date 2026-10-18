package com.kaoyan.platform.question.mapper;

import lombok.Data;

/** 题目-知识点关联的批量查询行（避免逐题查 question_kp） */
@Data
public class QuestionKpRow {

    private Long questionId;
    private Long kpId;
}

package com.kaoyan.platform.question.vo;

import java.util.List;
import lombok.Data;

@Data
public class KnowledgePointVO {

    private Long id;
    private String name;
    private Long parentId;
    private List<KnowledgePointVO> children;
}

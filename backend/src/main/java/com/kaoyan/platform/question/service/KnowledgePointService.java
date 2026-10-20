package com.kaoyan.platform.question.service;

import com.kaoyan.platform.question.vo.KnowledgePointVO;
import java.util.List;

public interface KnowledgePointService {

    /**
     * 知识点树：一次查全量平铺列表，再在内存按 parentId 组装。
     * 知识点是百级数据，内存组树比递归 SQL 更简单，也避免多次查库。
     */
    List<KnowledgePointVO> tree();

    KnowledgePointVO create(String name, Long parentId);
}

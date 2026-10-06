package com.kaoyan.platform.question.service;

import com.kaoyan.platform.question.vo.KnowledgePointVO;
import java.util.List;

public interface KnowledgePointService {

    /**
     * L1 待实现：从 selectAll() 的平铺列表组装成树（parentId 挂 children）。
     * 思考：为什么组树放在 Java 内存里做而不是用递归 SQL？（决策日志素材）
     */
    List<KnowledgePointVO> tree();

    KnowledgePointVO create(String name, Long parentId);
}

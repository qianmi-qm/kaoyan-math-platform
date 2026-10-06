package com.kaoyan.platform.question.service.impl;

import com.kaoyan.platform.common.exception.BusinessException;
import com.kaoyan.platform.common.result.ErrorCode;
import com.kaoyan.platform.question.entity.KnowledgePoint;
import com.kaoyan.platform.question.mapper.KnowledgePointMapper;
import com.kaoyan.platform.question.service.KnowledgePointService;
import com.kaoyan.platform.question.vo.KnowledgePointVO;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class KnowledgePointServiceImpl implements KnowledgePointService {

    private final KnowledgePointMapper knowledgePointMapper;

    public KnowledgePointServiceImpl(KnowledgePointMapper knowledgePointMapper) {
        this.knowledgePointMapper = knowledgePointMapper;
    }

    /**
     * 一次 selectAll 后在内存组树：知识点是百级量级，一次查询 + O(n) 组装
     * 比"每条分支递归查一次"更划算，也避开 MySQL 8 递归 CTE 的复杂度。
     */
    @Override
    public List<KnowledgePointVO> tree() {
        Map<Long, KnowledgePointVO> byId = new LinkedHashMap<>();
        for (KnowledgePoint kp : knowledgePointMapper.selectAll()) {
            byId.put(kp.getId(), toVO(kp));
        }
        List<KnowledgePointVO> roots = new ArrayList<>();
        for (KnowledgePointVO vo : byId.values()) {
            KnowledgePointVO parent = vo.getParentId() == null ? null : byId.get(vo.getParentId());
            if (parent == null) {
                roots.add(vo);
            } else {
                parent.getChildren().add(vo);
            }
        }
        return roots;
    }

    @Override
    public KnowledgePointVO create(String name, Long parentId) {
        if (name == null || name.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "知识点名称不能为空");
        }
        String trimmed = name.trim();
        if (knowledgePointMapper.selectByName(trimmed) != null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "知识点已存在：" + trimmed);
        }
        if (parentId != null && knowledgePointMapper.selectById(parentId) == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "父知识点不存在：" + parentId);
        }
        KnowledgePoint kp = new KnowledgePoint();
        kp.setName(trimmed);
        kp.setParentId(parentId);
        knowledgePointMapper.insert(kp);
        return toVO(kp);
    }

    private KnowledgePointVO toVO(KnowledgePoint kp) {
        KnowledgePointVO vo = new KnowledgePointVO();
        vo.setId(kp.getId());
        vo.setName(kp.getName());
        vo.setParentId(kp.getParentId());
        vo.setChildren(new ArrayList<>());
        return vo;
    }
}
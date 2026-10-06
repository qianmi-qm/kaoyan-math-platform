package com.kaoyan.platform.question.controller;

import com.kaoyan.platform.common.result.Result;
import com.kaoyan.platform.question.service.KnowledgePointService;
import com.kaoyan.platform.question.vo.KnowledgePointVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "知识点")
@RestController
@RequestMapping("/api/knowledge-points")
public class KnowledgePointController {

    private final KnowledgePointService knowledgePointService;

    public KnowledgePointController(KnowledgePointService knowledgePointService) {
        this.knowledgePointService = knowledgePointService;
    }

    @Operation(summary = "知识点树")
    @GetMapping
    public Result<List<KnowledgePointVO>> tree() {
        return Result.ok(knowledgePointService.tree());
    }

    @Operation(summary = "新增知识点")
    @PostMapping
    public Result<KnowledgePointVO> create(@RequestParam String name,
                                           @RequestParam(required = false) Long parentId) {
        return Result.ok(knowledgePointService.create(name, parentId));
    }
}

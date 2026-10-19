package com.kaoyan.platform.question.controller;

import com.kaoyan.platform.common.result.PageVO;
import com.kaoyan.platform.common.result.Result;
import com.kaoyan.platform.question.dto.QuestionCreateDTO;
import com.kaoyan.platform.question.dto.QuestionQueryDTO;
import com.kaoyan.platform.question.service.QuestionService;
import com.kaoyan.platform.question.vo.ImportResultVO;
import com.kaoyan.platform.question.vo.QuestionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "题库管理")
@RestController
@RequestMapping("/api/questions")
public class QuestionController {

    private final QuestionService questionService;

    public QuestionController(QuestionService questionService) {
        this.questionService = questionService;
    }

    @Operation(summary = "手动录入单题")
    @PostMapping
    public Result<QuestionVO> create(@Valid @RequestBody QuestionCreateDTO dto) {
        return Result.ok(questionService.create(dto));
    }

    @Operation(summary = "Excel 批量导入（multipart，模板列见 QuestionImportRow）")
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<ImportResultVO> importExcel(@RequestPart("file") MultipartFile file) throws IOException {
        return Result.ok(questionService.importExcel(file));
    }

    @Operation(summary = "分页查询题目（列表不返回答案）")
    @GetMapping
    public Result<PageVO<QuestionVO>> page(@Valid QuestionQueryDTO query) {
        return Result.ok(questionService.page(query));
    }

    @Operation(summary = "题目详情（含答案与解析）")
    @GetMapping("/{id}")
    public Result<QuestionVO> detail(@PathVariable Long id) {
        return Result.ok(questionService.detail(id));
    }

    @Operation(summary = "管理员编辑题目")
    @PutMapping("/{id}")
    public Result<QuestionVO> update(@PathVariable Long id,
                                     @Valid @RequestBody QuestionCreateDTO dto) {
        return Result.ok(questionService.update(id, dto));
    }
}

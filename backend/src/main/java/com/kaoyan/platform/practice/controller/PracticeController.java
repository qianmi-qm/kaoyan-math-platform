package com.kaoyan.platform.practice.controller;

import com.kaoyan.platform.common.result.Result;
import com.kaoyan.platform.practice.dto.SelfEvalDTO;
import com.kaoyan.platform.practice.dto.StartSessionDTO;
import com.kaoyan.platform.practice.dto.SubmitAnswerDTO;
import com.kaoyan.platform.practice.service.PracticeService;
import com.kaoyan.platform.practice.vo.PracticeRecordVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "练习")
@RestController
@RequestMapping("/api/practice")
public class PracticeController {

    private final PracticeService practiceService;

    public PracticeController(PracticeService practiceService) {
        this.practiceService = practiceService;
    }

    @Operation(summary = "开始练习：按模式选题并生成记录（返回不含答案）")
    @PostMapping("/sessions")
    public Result<List<PracticeRecordVO>> startSession(@Valid @RequestBody StartSessionDTO dto) {
        return Result.ok(practiceService.startSession(dto));
    }

    @Operation(summary = "提交答案：服务端计时并判分，大题返回答案供自评")
    @PostMapping("/records/{recordId}/submit")
    public Result<PracticeRecordVO> submit(@PathVariable Long recordId,
                                           @Valid @RequestBody SubmitAnswerDTO dto) {
        return Result.ok(practiceService.submit(recordId, dto));
    }

    @Operation(summary = "大题自评：提交占位答案后，看到答案再判对错")
    @PostMapping("/records/{recordId}/self-eval")
    public Result<PracticeRecordVO> selfEvaluate(@PathVariable Long recordId,
                                                 @Valid @RequestBody SelfEvalDTO dto) {
        return Result.ok(practiceService.selfEvaluate(recordId, dto.getCorrect()));
    }

    @Operation(summary = "按日查做题记录（复盘）")
    @GetMapping("/records")
    public Result<List<PracticeRecordVO>> listByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return Result.ok(practiceService.listByDate(date));
    }
}

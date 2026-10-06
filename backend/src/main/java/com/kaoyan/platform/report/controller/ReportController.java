package com.kaoyan.platform.report.controller;

import com.kaoyan.platform.common.result.Result;
import com.kaoyan.platform.report.service.ReportService;
import com.kaoyan.platform.report.vo.DailyReportVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "每日报告")
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @Operation(summary = "某日报告（无则现场生成并保存快照）")
    @GetMapping("/daily")
    public Result<DailyReportVO> daily(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return Result.ok(reportService.getDaily(date));
    }

    @Operation(summary = "报告区间查询（趋势图数据源）")
    @GetMapping("/daily/range")
    public Result<List<DailyReportVO>> range(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return Result.ok(reportService.getRange(from, to));
    }
}

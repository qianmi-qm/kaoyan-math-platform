package com.kaoyan.platform.report.service;

import com.kaoyan.platform.report.vo.DailyReportVO;
import java.time.LocalDate;
import java.util.List;

public interface ReportService {

    /**
     * 某日报告：今天实时生成，历史日期优先读快照。
     * 报告只统计当天练习；整体掌握度看 LearningOverviewService。
     */
    DailyReportVO getDaily(LocalDate date);

    List<DailyReportVO> getRange(LocalDate from, LocalDate to);
}

package com.kaoyan.platform.report.service;

import com.kaoyan.platform.report.vo.DailyReportVO;
import java.time.LocalDate;
import java.util.List;

public interface ReportService {

    /**
     * L4 学习任务：取某日报告；不存在则现场生成并落库快照。
     * 生成流程：
     * 1. selectDailyAgg 拿总数/正确数/平均用时
     * 2. 遍历当日涉及的题目 → MasteryCalculator 算题目级掌握度
     * 3. 按知识点聚合（难度作权重）→ kpMastery / weakPoints
     * 4. JSON 序列化后 upsert
     */
    DailyReportVO getDaily(LocalDate date);

    List<DailyReportVO> getRange(LocalDate from, LocalDate to);
}

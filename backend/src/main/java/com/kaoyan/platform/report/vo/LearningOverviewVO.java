package com.kaoyan.platform.report.vo;

import com.kaoyan.platform.review.vo.ReviewStatsVO;
import java.time.LocalDate;
import java.util.List;
import lombok.Data;

@Data
public class LearningOverviewVO {

    private int days;
    private LocalDate from;
    private LocalDate to;
    private int startedCount;
    private int submittedCount;
    private int gradedCount;
    private int pendingCount;
    private int correctCount;
    private double correctRate;
    private int totalDurationSec;
    private int avgDurationSec;
    private double overallMastery;
    private List<KpMasteryVO> kpMastery;
    private List<KpMasteryVO> weakPoints;
    private List<BreakdownVO> byType;
    private List<BreakdownVO> byDifficulty;
    private ReviewStatsVO reviewStats;
    private List<ActionSuggestionVO> suggestions;
}

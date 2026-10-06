package com.kaoyan.platform.report.vo;

import java.time.LocalDate;
import java.util.List;
import lombok.Data;

@Data
public class DailyReportVO {

    private LocalDate reportDate;
    private int totalCount;
    private int correctCount;
    private double correctRate;
    private int avgDurationSec;
    private List<KpMasteryVO> kpMastery;
    private List<KpMasteryVO> weakPoints;
}

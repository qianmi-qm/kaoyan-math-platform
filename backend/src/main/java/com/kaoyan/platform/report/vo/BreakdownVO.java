package com.kaoyan.platform.report.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BreakdownVO {

    private String key;
    private String label;
    private int count;
    private int gradedCount;
    private int correctCount;
    private double correctRate;
    private int avgDurationSec;
}

package com.kaoyan.platform.report.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class KpMasteryVO {

    private Long kpId;
    private String name;
    private double mastery;
    private int questionCount;
}

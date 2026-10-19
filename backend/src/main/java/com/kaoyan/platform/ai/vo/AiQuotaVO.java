package com.kaoyan.platform.ai.vo;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AiQuotaVO {

    private LocalDate date;
    private int limit;
    private int used;
    private int remaining;
}

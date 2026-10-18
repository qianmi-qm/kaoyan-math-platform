package com.kaoyan.platform.review.vo;

import java.util.List;
import lombok.Data;

@Data
public class ReviewStatsVO {

    private long activeTotal;
    private long dueToday;
    private long completedTotal;
    private List<StageCount> byStage;

    @Data
    public static class StageCount {
        private int stage;
        private long count;
    }
}

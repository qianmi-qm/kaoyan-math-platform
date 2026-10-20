package com.kaoyan.platform.report.domain;

import com.kaoyan.platform.practice.entity.PracticeRecord;
import com.kaoyan.platform.question.entity.Question;
import com.kaoyan.platform.report.vo.KpMasteryVO;
import java.util.Comparator;
import java.util.List;

/**
 * 报告层共用的掌握度聚合。
 * 今日报告和近 N 天分析都调用这里，避免两套公式慢慢跑偏。
 */
public final class MasteryAggregator {

    private MasteryAggregator() {
    }

    public static double questionMastery(Question question, List<PracticeRecord> recentDesc) {
        if (question == null || recentDesc == null || recentDesc.isEmpty()) {
            return 0.0;
        }
        List<PracticeRecord> recent = recentDesc.stream()
                .filter(r -> r.getIsCorrect() != null)
                .sorted(Comparator.comparing(PracticeRecord::getId).reversed())
                .limit(MasteryCalculator.RECENT_WINDOW)
                .toList();
        if (recent.isEmpty()) {
            return 0.0;
        }
        int correct = (int) recent.stream()
                .filter(r -> Boolean.TRUE.equals(r.getIsCorrect()))
                .count();
        int duration = recent.get(0).getDurationSec() == null ? 0 : recent.get(0).getDurationSec();
        return MasteryCalculator.questionMastery(
                recent.size(), correct, duration,
                MasteryCalculator.baselineSeconds(question.getDifficulty()),
                streak(recent));
    }

    public static int streak(List<PracticeRecord> recentDesc) {
        int streak = 0;
        for (PracticeRecord record : recentDesc) {
            if (!Boolean.TRUE.equals(record.getIsCorrect())) {
                break;
            }
            streak++;
        }
        return streak;
    }

    /** 知识点掌握度按题量加权，避免一个只有 1 题的知识点权重过高。 */
    public static double overall(List<KpMasteryVO> masteryList) {
        int total = masteryList == null ? 0
                : masteryList.stream().mapToInt(KpMasteryVO::getQuestionCount).sum();
        if (total == 0) {
            return 0.0;
        }
        return masteryList.stream()
                .mapToDouble(item -> item.getMastery() * item.getQuestionCount())
                .sum() / total;
    }
}

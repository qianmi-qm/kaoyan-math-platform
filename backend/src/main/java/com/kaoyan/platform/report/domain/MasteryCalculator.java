package com.kaoyan.platform.report.domain;

import java.util.List;

/**
 * 掌握度模型（纯函数，无 Spring 依赖——这是它能被毫秒级单测的原因）。
 *
 * 公式（方案文档 5.1 节）：
 *   mastery = 0.5 × accuracy + 0.3 × speed + 0.2 × streak
 *   accuracy = 近 N 次（默认 5）正确率
 *   speed    = clamp(基准用时 / 实际用时, 0, 1)，基准：易 60s / 中 120s / 难 240s
 *   streak   = min(连续答对次数 / 3, 1)
 *
 * L4 学习任务：实现以下全部方法，直到 MasteryCalculatorTest 全绿。
 */
public final class MasteryCalculator {

    public static final double WEIGHT_ACCURACY = 0.5;
    public static final double WEIGHT_SPEED = 0.3;
    public static final double WEIGHT_STREAK = 0.2;
    public static final double WEAK_THRESHOLD = 0.6;
    public static final int WEAK_MIN_QUESTIONS = 3;
    public static final int RECENT_WINDOW = 5;

    private MasteryCalculator() {
    }

    /**
     * 题目级掌握度（0~1）。
     * 约定：
     * - recentTotal <= 0 → 直接返回 0.0（无数据不给分）
     * - actualSeconds <= 0 → speed 按 0 处理（异常提交不奖励）
     * - baselineSeconds <= 0 → 抛 IllegalArgumentException
     */
    public static double questionMastery(int recentTotal, int recentCorrect,
                                         int actualSeconds, int baselineSeconds,
                                         int streakCount) {
        if (recentTotal <= 0) {
            return 0.0;
        }
        if (baselineSeconds <= 0) {
            throw new IllegalArgumentException("baselineSeconds 必须大于 0");
        }

        double accuracy = clamp01((double) recentCorrect / recentTotal);
        double speed = actualSeconds <= 0
                ? 0.0
                : clamp01((double) baselineSeconds / actualSeconds);
        double streak = clamp01(streakCount / 3.0);

        return clamp01(WEIGHT_ACCURACY * accuracy + WEIGHT_SPEED * speed + WEIGHT_STREAK * streak);
    }

    /** 难度 → 基准用时（秒）：1→60，2→120，3→240；其他值抛 IllegalArgumentException */
    public static int baselineSeconds(int difficulty) {
        return switch (difficulty) {
            case 1 -> 60;
            case 2 -> 120;
            case 3 -> 240;
            default -> throw new IllegalArgumentException("非法难度：" + difficulty);
        };
    }

    /** 知识点级掌握度：按题目权重加权平均；items 为空返回 0.0 */
    public static double kpMastery(List<KpItem> items) {
        if (items == null || items.isEmpty()) {
            return 0.0;
        }
        double weightedSum = 0.0;
        double weightSum = 0.0;
        for (KpItem item : items) {
            if (item == null || item.weight() <= 0) {
                continue;
            }
            weightedSum += item.mastery() * item.weight();
            weightSum += item.weight();
        }
        return weightSum <= 0 ? 0.0 : weightedSum / weightSum;
    }

    /** 薄弱点判定：掌握度 < 0.6 且练习题量 >= 3 */
    public static boolean isWeakPoint(double kpMastery, int questionCount) {
        return kpMastery < WEAK_THRESHOLD && questionCount >= WEAK_MIN_QUESTIONS;
    }

    private static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    /** kpMastery 的输入项：mastery 为题目级掌握度，weight 取题目难度值 */
    public record KpItem(double mastery, double weight) {
    }
}

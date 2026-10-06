package com.kaoyan.platform.report.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * L4 验收测试：当前应为全红（UnsupportedOperationException）。
 * 你的任务是实现 MasteryCalculator 让本类全绿——这就是 L4 的完成定义。
 */
@DisplayName("掌握度模型")
class MasteryCalculatorTest {

    private static final double DELTA = 0.0001;

    @Test
    @DisplayName("完整公式：5 题对 4、用时等于基准、3 连对 → 0.9")
    void questionMastery_fullFormula() {
        double m = MasteryCalculator.questionMastery(5, 4, 120, 120, 3);
        assertEquals(0.5 * 0.8 + 0.3 * 1.0 + 0.2 * 1.0, m, DELTA);
    }

    @Test
    @DisplayName("用时得分向上截断：实际 30s / 基准 120s 只能拿满 speed=1，不会超过")
    void questionMastery_speedClampedHigh() {
        double m = MasteryCalculator.questionMastery(5, 5, 30, 120, 0);
        assertEquals(0.5 + 0.3, m, DELTA);
    }

    @Test
    @DisplayName("用时得分向下：实际 600s / 基准 120s → speed=0.2")
    void questionMastery_speedLow() {
        double m = MasteryCalculator.questionMastery(5, 5, 600, 120, 0);
        assertEquals(0.5 + 0.3 * 0.2, m, DELTA);
    }

    @Test
    @DisplayName("异常提交不奖励：actualSeconds=0 → speed 记 0")
    void questionMastery_zeroActualSeconds() {
        double m = MasteryCalculator.questionMastery(1, 1, 0, 120, 0);
        assertEquals(0.5, m, DELTA);
    }

    @Test
    @DisplayName("无作答数据 → 掌握度为 0")
    void questionMastery_noData() {
        assertEquals(0.0, MasteryCalculator.questionMastery(0, 0, 100, 120, 0), DELTA);
    }

    @Test
    @DisplayName("连续答对封顶：streak=7 也只按 1 计")
    void questionMastery_streakCapped() {
        double m = MasteryCalculator.questionMastery(5, 5, 120, 120, 7);
        assertEquals(1.0, m, DELTA);
    }

    @Test
    @DisplayName("难度基准用时：1→60 / 2→120 / 3→240，非法难度抛异常")
    void baselineSeconds() {
        assertEquals(60, MasteryCalculator.baselineSeconds(1));
        assertEquals(120, MasteryCalculator.baselineSeconds(2));
        assertEquals(240, MasteryCalculator.baselineSeconds(3));
        assertThrows(IllegalArgumentException.class, () -> MasteryCalculator.baselineSeconds(4));
    }

    @Test
    @DisplayName("知识点聚合：难度加权平均 (0.9×3 + 0.6×1) / 4 = 0.825")
    void kpMastery_weighted() {
        List<MasteryCalculator.KpItem> items = List.of(
                new MasteryCalculator.KpItem(0.9, 3),
                new MasteryCalculator.KpItem(0.6, 1));
        assertEquals(0.825, MasteryCalculator.kpMastery(items), DELTA);
    }

    @Test
    @DisplayName("知识点聚合：空列表返回 0")
    void kpMastery_empty() {
        assertEquals(0.0, MasteryCalculator.kpMastery(List.of()), DELTA);
    }

    @Test
    @DisplayName("薄弱点：<0.6 且题量≥3 才算；题量不足或掌握尚可都不算")
    void weakPoint() {
        assertTrue(MasteryCalculator.isWeakPoint(0.55, 5));
        assertFalse(MasteryCalculator.isWeakPoint(0.55, 2));
        assertFalse(MasteryCalculator.isWeakPoint(0.7, 10));
    }
}

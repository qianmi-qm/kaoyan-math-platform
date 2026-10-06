package com.kaoyan.platform.review.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * L5 验收测试：当前应为全红（UnsupportedOperationException）。
 * 实现 EbbinghausScheduler 后本类全绿即为 L5 核心算法完成。
 * 注意：全程不需要改系统时间，也不需要连数据库。
 */
@DisplayName("艾宾浩斯调度")
class EbbinghausSchedulerTest {

    private static final LocalDate AUG_10 = LocalDate.of(2026, 8, 10);
    private static final LocalDate AUG_13 = LocalDate.of(2026, 8, 13);

    @Test
    @DisplayName("记住 → 周期推进：stage 0→1，stage 3→4")
    void nextStage_remembered() {
        assertEquals(1, EbbinghausScheduler.nextStage(0, true));
        assertEquals(4, EbbinghausScheduler.nextStage(3, true));
    }

    @Test
    @DisplayName("忘了 → 重置回 stage 0")
    void nextStage_forgotten() {
        assertEquals(0, EbbinghausScheduler.nextStage(3, false));
    }

    @Test
    @DisplayName("非法 stage 抛异常")
    void nextStage_negativeThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> EbbinghausScheduler.nextStage(-1, true));
    }

    @Test
    @DisplayName("下次复习日：stage0 → +1 天，stage2 → +4 天")
    void nextReviewDate() {
        assertEquals(LocalDate.of(2026, 8, 11), EbbinghausScheduler.nextReviewDate(0, AUG_10));
        assertEquals(LocalDate.of(2026, 8, 14), EbbinghausScheduler.nextReviewDate(2, AUG_10));
    }

    @Test
    @DisplayName("已完成/非法 stage 不允许再排期")
    void nextReviewDate_invalidStageThrows() {
        assertThrows(IllegalArgumentException.class, () -> EbbinghausScheduler.nextReviewDate(5, AUG_10));
        assertThrows(IllegalArgumentException.class, () -> EbbinghausScheduler.nextReviewDate(-1, AUG_10));
    }

    @Test
    @DisplayName("首次入队：答错当天 +1 天")
    void firstReviewDate() {
        assertEquals(LocalDate.of(2026, 8, 11), EbbinghausScheduler.firstReviewDate(AUG_10));
    }

    @Test
    @DisplayName("全部周期完成判定：stage 5 为完成，stage 4 未完成")
    void isFinished() {
        assertTrue(EbbinghausScheduler.isFinished(5));
        assertFalse(EbbinghausScheduler.isFinished(4));
    }

    @Test
    @DisplayName("错过复习日顺延不跳级：8-10 到期、8-13 才复习、记住 → 下次是 8-13+4=8-17，而不是 8-10+4=8-14")
    void missedDays_postponeWithoutSkipping() {
        int nextStage = EbbinghausScheduler.nextStage(1, true);
        assertEquals(2, nextStage);
        assertEquals(LocalDate.of(2026, 8, 17), EbbinghausScheduler.nextReviewDate(nextStage, AUG_13));
    }
}

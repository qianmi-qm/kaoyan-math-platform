package com.kaoyan.platform.review.domain;

import java.time.LocalDate;

/**
 * 艾宾浩斯复习调度（纯函数，无 Spring 依赖）。
 *
 * 周期：[1, 2, 4, 7, 15] 天。
 * 状态流转（方案文档 5.2 节）：
 * - 记住 → stage + 1，下次复习日 = 实际复习日 + INTERVALS[新stage]
 * - 忘了 → 重置 stage = 0，下次复习日 = 实际复习日 + 1 天
 * - 错过复习日 → 到期任务照常出现在"今日复习"，间隔从实际复习日算起（顺延不跳级）
 * - stage 走完 5 个周期 → 队列完成（isFinished）
 *
 * L5 学习任务：实现以下方法，直到 EbbinghausSchedulerTest 全绿。
 */
public final class EbbinghausScheduler {

    public static final int[] INTERVALS = {1, 2, 4, 7, 15};

    private EbbinghausScheduler() {
    }

    /**
     * 复习后的新 stage。
     * remembered=true → currentStage + 1（可超过 INTERVALS.length-1，配合 isFinished 使用）
     * remembered=false → 0
     * currentStage 为负抛 IllegalArgumentException。
     */
    public static int nextStage(int currentStage, boolean remembered) {
        throw new UnsupportedOperationException("L5 待实现：周期推进/重置");
    }

    /**
     * 下次复习日 = reviewedOn + INTERVALS[stage]。
     * stage 必须在 [0, INTERVALS.length-1]，否则抛 IllegalArgumentException
     * （已完成的题不该再排期——调用方先判 isFinished）。
     */
    public static LocalDate nextReviewDate(int stage, LocalDate reviewedOn) {
        throw new UnsupportedOperationException("L5 待实现：计算下次复习日");
    }

    /** 首次入队：答错当天 +1 天 */
    public static LocalDate firstReviewDate(LocalDate answeredOn) {
        throw new UnsupportedOperationException("L5 待实现：首次复习日");
    }

    /** stage 已越过最后一个周期下标 → 该题复习完成 */
    public static boolean isFinished(int stage) {
        throw new UnsupportedOperationException("L5 待实现：是否完成全部周期");
    }
}

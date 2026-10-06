package com.kaoyan.platform.review.service;

import com.kaoyan.platform.review.vo.ReviewStatsVO;
import com.kaoyan.platform.review.vo.ReviewTaskVO;
import java.util.List;

public interface ReviewService {

    /** L5：今日到期清单（next_review_date <= today，status=0） */
    List<ReviewTaskVO> todayList();

    /**
     * L5：提交复习结果。
     * 用 EbbinghausScheduler.nextStage / nextReviewDate / isFinished 推进；
     * isFinished 为 true 时置 status=1 与 completed_at=now。
     * 提交前校验：记录存在且 status=0，否则抛 PARAM_ERROR。
     */
    ReviewTaskVO submitResult(Long reviewId, boolean remembered);

    /**
     * L5：答错入队（由 PracticeServiceImpl.submit 调用）。
     * 已有 active 记录 → 重置 stage=0、next=+1 天；没有 → 新建。
     */
    void enqueueIfWrong(Long userId, Long questionId);

    ReviewStatsVO stats();
}

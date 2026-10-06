package com.kaoyan.platform.review.service.impl;

import com.kaoyan.platform.common.exception.BusinessException;
import com.kaoyan.platform.common.result.ErrorCode;
import com.kaoyan.platform.common.support.CurrentUser;
import com.kaoyan.platform.question.entity.Question;
import com.kaoyan.platform.question.mapper.QuestionMapper;
import com.kaoyan.platform.review.domain.EbbinghausScheduler;
import com.kaoyan.platform.review.entity.ReviewSchedule;
import com.kaoyan.platform.review.mapper.ReviewScheduleMapper;
import com.kaoyan.platform.review.service.ReviewService;
import com.kaoyan.platform.review.vo.ReviewStatsVO;
import com.kaoyan.platform.review.vo.ReviewTaskVO;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewServiceImpl implements ReviewService {

    private final ReviewScheduleMapper reviewScheduleMapper;
    private final QuestionMapper questionMapper;
    private final CurrentUser currentUser;
    private final Clock clock;

    public ReviewServiceImpl(ReviewScheduleMapper reviewScheduleMapper,
                             QuestionMapper questionMapper,
                             CurrentUser currentUser,
                             Clock clock) {
        this.reviewScheduleMapper = reviewScheduleMapper;
        this.questionMapper = questionMapper;
        this.currentUser = currentUser;
        this.clock = clock;
    }

    @Override
    public List<ReviewTaskVO> todayList() {
        Long userId = currentUser.getUserId();
        LocalDate today = LocalDate.now(clock);
        List<ReviewTaskVO> tasks = new ArrayList<>();
        for (ReviewSchedule schedule : reviewScheduleMapper.selectDue(userId, today)) {
            tasks.add(toVO(schedule, today));
        }
        return tasks;
    }

    @Override
    @Transactional
    public ReviewTaskVO submitResult(Long reviewId, boolean remembered) {
        ReviewSchedule schedule = reviewScheduleMapper.selectById(reviewId);
        if (schedule == null || schedule.getStatus() != ReviewSchedule.STATUS_ACTIVE) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "复习任务不存在或已完成：" + reviewId);
        }
        LocalDate today = LocalDate.now(clock);
        int stage = EbbinghausScheduler.nextStage(schedule.getStage(), remembered);
        schedule.setStage(stage);
        if (EbbinghausScheduler.isFinished(stage)) {
            schedule.setStatus(ReviewSchedule.STATUS_DONE);
            schedule.setCompletedAt(LocalDateTime.now(clock));
        } else {
            // 从"实际复习日"起算，错过几天不跳级（顺延而非补跳）
            schedule.setNextReviewDate(EbbinghausScheduler.nextReviewDate(stage, today));
        }
        reviewScheduleMapper.updateProgress(schedule);
        return toVO(schedule, today);
    }

    @Override
    @Transactional
    public void enqueueIfWrong(Long userId, Long questionId) {
        LocalDate today = LocalDate.now(clock);
        ReviewSchedule active = reviewScheduleMapper.selectActive(userId, questionId);
        if (active != null) {
            active.setStage(0);
            active.setNextReviewDate(EbbinghausScheduler.firstReviewDate(today));
            active.setStatus(ReviewSchedule.STATUS_ACTIVE);
            active.setCompletedAt(null);
            reviewScheduleMapper.updateProgress(active);
            return;
        }
        ReviewSchedule fresh = new ReviewSchedule();
        fresh.setUserId(userId);
        fresh.setQuestionId(questionId);
        fresh.setStage(0);
        fresh.setNextReviewDate(EbbinghausScheduler.firstReviewDate(today));
        fresh.setStatus(ReviewSchedule.STATUS_ACTIVE);
        reviewScheduleMapper.insert(fresh);
    }

    @Override
    public ReviewStatsVO stats() {
        Long userId = currentUser.getUserId();
        LocalDate today = LocalDate.now(clock);
        ReviewStatsVO vo = new ReviewStatsVO();
        vo.setActiveTotal(reviewScheduleMapper.countActive(userId));
        vo.setDueToday(reviewScheduleMapper.countDue(userId, today));
        List<ReviewStatsVO.StageCount> byStage = new ArrayList<>();
        for (ReviewScheduleMapper.StageCount stageCount : reviewScheduleMapper.selectStageCounts(userId)) {
            ReviewStatsVO.StageCount item = new ReviewStatsVO.StageCount();
            item.setStage(stageCount.getStage());
            item.setCount(stageCount.getCount());
            byStage.add(item);
        }
        vo.setByStage(byStage);
        return vo;
    }

    private ReviewTaskVO toVO(ReviewSchedule schedule, LocalDate today) {
        ReviewTaskVO vo = new ReviewTaskVO();
        vo.setReviewId(schedule.getId());
        vo.setQuestionId(schedule.getQuestionId());
        vo.setStage(schedule.getStage());
        vo.setNextReviewDate(schedule.getNextReviewDate());
        vo.setOverdue(schedule.getNextReviewDate() != null && schedule.getNextReviewDate().isBefore(today));
        Question question = questionMapper.selectById(schedule.getQuestionId());
        if (question != null) {
            vo.setType(question.getType());
            vo.setContent(question.getContent());
            vo.setOptions(question.getOptions());
            vo.setDifficulty(question.getDifficulty());
        }
        return vo;
    }
}
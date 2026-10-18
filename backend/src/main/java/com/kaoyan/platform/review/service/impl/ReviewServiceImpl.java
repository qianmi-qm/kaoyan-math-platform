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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
        List<ReviewSchedule> schedules = reviewScheduleMapper.selectDue(userId, today);
        List<Long> questionIds = schedules.stream().map(ReviewSchedule::getQuestionId).distinct().toList();
        Map<Long, Question> questionById = new HashMap<>();
        if (!questionIds.isEmpty()) {
            for (Question question : questionMapper.selectByIds(questionIds)) {
                questionById.put(question.getId(), question);
            }
        }
        List<ReviewTaskVO> tasks = new ArrayList<>(schedules.size());
        for (ReviewSchedule schedule : schedules) {
            tasks.add(toVO(schedule, today, questionById.get(schedule.getQuestionId())));
        }
        return tasks;
    }

    @Override
    @Transactional
    public ReviewTaskVO submitResult(Long reviewId, boolean remembered) {
        ReviewSchedule schedule = reviewScheduleMapper.selectById(reviewId);
        if (schedule == null
                || !schedule.getUserId().equals(currentUser.getUserId())
                || schedule.getStatus() != ReviewSchedule.STATUS_ACTIVE) {
            // 不区分"不存在/不是你的/已完成"，避免泄露别人的任务 id
            throw new BusinessException(ErrorCode.NOT_FOUND, "复习任务不存在或已完成：" + reviewId);
        }
        LocalDate today = LocalDate.now(clock);
        if (schedule.getNextReviewDate() != null && schedule.getNextReviewDate().isAfter(today)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "还没到复习日期，不能提前提交");
        }
        int stage = EbbinghausScheduler.nextStage(schedule.getStage(), remembered);
        schedule.setStage(stage);
        if (EbbinghausScheduler.isFinished(stage)) {
            schedule.setStatus(ReviewSchedule.STATUS_DONE);
            schedule.setCompletedAt(LocalDateTime.now(clock));
        } else {
            // 从"实际复习日"起算，错过几天不跳级（顺延而非补跳）
            schedule.setNextReviewDate(EbbinghausScheduler.nextReviewDate(stage, today));
        }
        int updated = reviewScheduleMapper.updateProgress(schedule);
        if (updated == 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "复习任务已被处理，请刷新");
        }
        return toVO(schedule, today);
    }

    @Override
    @Transactional
    public void enqueueIfWrong(Long userId, Long questionId) {
        LocalDate today = LocalDate.now(clock);
        // UNIQUE(active_key) + ON DUPLICATE KEY UPDATE：并发答错也只会有一条 active
        reviewScheduleMapper.upsertActive(userId, questionId, EbbinghausScheduler.firstReviewDate(today));
    }

    @Override
    public ReviewStatsVO stats() {
        Long userId = currentUser.getUserId();
        LocalDate today = LocalDate.now(clock);
        ReviewStatsVO vo = new ReviewStatsVO();
        vo.setActiveTotal(reviewScheduleMapper.countActive(userId));
        vo.setDueToday(reviewScheduleMapper.countDue(userId, today));
        vo.setCompletedTotal(reviewScheduleMapper.countCompleted(userId));
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
        return toVO(schedule, today, questionMapper.selectById(schedule.getQuestionId()));
    }

    private ReviewTaskVO toVO(ReviewSchedule schedule, LocalDate today, Question question) {
        ReviewTaskVO vo = new ReviewTaskVO();
        vo.setReviewId(schedule.getId());
        vo.setQuestionId(schedule.getQuestionId());
        vo.setStage(schedule.getStage());
        vo.setNextReviewDate(schedule.getNextReviewDate());
        vo.setOverdue(schedule.getNextReviewDate() != null && schedule.getNextReviewDate().isBefore(today));
        if (question != null) {
            vo.setType(question.getType());
            vo.setContent(question.getContent());
            vo.setOptions(question.getOptions());
            vo.setDifficulty(question.getDifficulty());
        }
        return vo;
    }
}
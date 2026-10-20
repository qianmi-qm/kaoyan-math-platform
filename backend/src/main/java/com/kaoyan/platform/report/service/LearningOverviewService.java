package com.kaoyan.platform.report.service;

import com.kaoyan.platform.common.support.CurrentUser;
import com.kaoyan.platform.practice.entity.PracticeRecord;
import com.kaoyan.platform.practice.mapper.PracticeRecordMapper;
import com.kaoyan.platform.question.entity.KnowledgePoint;
import com.kaoyan.platform.question.entity.Question;
import com.kaoyan.platform.question.mapper.KnowledgePointMapper;
import com.kaoyan.platform.question.mapper.QuestionKpRow;
import com.kaoyan.platform.question.mapper.QuestionMapper;
import com.kaoyan.platform.report.domain.MasteryCalculator;
import com.kaoyan.platform.report.vo.ActionSuggestionVO;
import com.kaoyan.platform.report.vo.BreakdownVO;
import com.kaoyan.platform.report.vo.KpMasteryVO;
import com.kaoyan.platform.report.vo.LearningOverviewVO;
import com.kaoyan.platform.review.service.ReviewService;
import com.kaoyan.platform.review.vo.ReviewStatsVO;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

/** 近 N 天整体学习分析：与“今日报告”分开，避免拿一天的数据冒充整体。 */
@Service
public class LearningOverviewService {

    private static final String[] TYPE_LABELS = {"", "单选", "填空", "大题"};
    private static final String[] DIFFICULTY_LABELS = {"", "易", "中", "难"};

    private final PracticeRecordMapper practiceRecordMapper;
    private final QuestionMapper questionMapper;
    private final KnowledgePointMapper knowledgePointMapper;
    private final ReviewService reviewService;
    private final CurrentUser currentUser;
    private final Clock clock;

    public LearningOverviewService(PracticeRecordMapper practiceRecordMapper,
                                   QuestionMapper questionMapper,
                                   KnowledgePointMapper knowledgePointMapper,
                                   ReviewService reviewService,
                                   CurrentUser currentUser,
                                   Clock clock) {
        this.practiceRecordMapper = practiceRecordMapper;
        this.questionMapper = questionMapper;
        this.knowledgePointMapper = knowledgePointMapper;
        this.reviewService = reviewService;
        this.currentUser = currentUser;
        this.clock = clock;
    }

    public LearningOverviewVO overview(int days) {
        int safeDays = Math.min(90, Math.max(1, days));
        LocalDate to = LocalDate.now(clock);
        LocalDate from = to.minusDays(safeDays - 1L);
        Long userId = currentUser.getUserId();
        List<PracticeRecord> records = practiceRecordMapper.selectByUserAndDateRange(userId, from, to);

        LearningOverviewVO vo = new LearningOverviewVO();
        vo.setDays(safeDays);
        vo.setFrom(from);
        vo.setTo(to);
        vo.setKpMastery(List.of());
        vo.setWeakPoints(List.of());
        vo.setByType(List.of());
        vo.setByDifficulty(List.of());
        vo.setSuggestions(List.of());
        vo.setReviewStats(safeReviewStats());

        if (records.isEmpty()) {
            vo.getSuggestions().add(new ActionSuggestionVO(
                    "还没有练习数据", "先完成一组练习，分析才有意义", "/practice"));
            return vo;
        }

        int submitted = 0;
        int graded = 0;
        int correct = 0;
        int totalDuration = 0;
        for (PracticeRecord record : records) {
            if (record.getSubmittedAt() != null) {
                submitted++;
                if (record.getDurationSec() != null) totalDuration += record.getDurationSec();
            }
            if (record.getIsCorrect() != null) {
                graded++;
                if (Boolean.TRUE.equals(record.getIsCorrect())) correct++;
            }
        }
        vo.setStartedCount(records.size());
        vo.setSubmittedCount(submitted);
        vo.setGradedCount(graded);
        vo.setPendingCount(Math.max(0, records.size() - submitted));
        vo.setCorrectCount(correct);
        vo.setCorrectRate(graded == 0 ? 0.0 : (double) correct / graded);
        vo.setTotalDurationSec(totalDuration);
        vo.setAvgDurationSec(submitted == 0 ? 0 : totalDuration / submitted);

        List<Long> questionIds = records.stream().map(PracticeRecord::getQuestionId)
                .distinct().toList();
        Map<Long, Question> questionById = new HashMap<>();
        for (Question question : questionMapper.selectByIds(questionIds)) {
            questionById.put(question.getId(), question);
        }
        Map<Long, List<Long>> kpIdsByQuestion = new HashMap<>();
        Set<Long> allKpIds = new HashSet<>();
        if (!questionIds.isEmpty()) {
            for (QuestionKpRow row : questionMapper.selectKpRowsByQuestionIds(questionIds)) {
                kpIdsByQuestion.computeIfAbsent(row.getQuestionId(), key -> new ArrayList<>())
                        .add(row.getKpId());
                allKpIds.add(row.getKpId());
            }
        }
        Map<Long, KnowledgePoint> kpById = new HashMap<>();
        if (!allKpIds.isEmpty()) {
            for (KnowledgePoint kp : knowledgePointMapper.selectByIds(new ArrayList<>(allKpIds))) {
                kpById.put(kp.getId(), kp);
            }
        }

        // 每题近 5 次已判分记录；范围外不看，保证是“近 N 天”
        Map<Long, List<PracticeRecord>> gradedByQuestion = new HashMap<>();
        for (PracticeRecord record : records) {
            if (record.getIsCorrect() != null) {
                gradedByQuestion.computeIfAbsent(record.getQuestionId(), key -> new ArrayList<>())
                        .add(record);
            }
        }
        Map<Long, List<MasteryCalculator.KpItem>> itemsByKp = new LinkedHashMap<>();
        Map<Long, String> kpNames = new HashMap<>();
        for (Map.Entry<Long, List<PracticeRecord>> entry : gradedByQuestion.entrySet()) {
            Question question = questionById.get(entry.getKey());
            if (question == null) continue;
            List<PracticeRecord> recent = entry.getValue().stream()
                    .sorted(Comparator.comparing(PracticeRecord::getId).reversed())
                    .limit(MasteryCalculator.RECENT_WINDOW)
                    .toList();
            int recentCorrect = (int) recent.stream()
                    .filter(r -> Boolean.TRUE.equals(r.getIsCorrect()))
                    .count();
            int duration = recent.get(0).getDurationSec() == null ? 0 : recent.get(0).getDurationSec();
            double mastery = MasteryCalculator.questionMastery(
                    recent.size(), recentCorrect, duration,
                    MasteryCalculator.baselineSeconds(question.getDifficulty()), streak(recent));
            for (Long kpId : kpIdsByQuestion.getOrDefault(question.getId(), List.of())) {
                KnowledgePoint kp = kpById.get(kpId);
                if (kp != null) kpNames.put(kpId, kp.getName());
                itemsByKp.computeIfAbsent(kpId, key -> new ArrayList<>())
                        .add(new MasteryCalculator.KpItem(mastery, question.getDifficulty()));
            }
        }

        List<KpMasteryVO> masteryList = new ArrayList<>();
        List<KpMasteryVO> weakPoints = new ArrayList<>();
        for (Map.Entry<Long, List<MasteryCalculator.KpItem>> entry : itemsByKp.entrySet()) {
            List<MasteryCalculator.KpItem> items = entry.getValue();
            double mastery = MasteryCalculator.kpMastery(items);
            KpMasteryVO kp = new KpMasteryVO(entry.getKey(),
                    kpNames.getOrDefault(entry.getKey(), ""), mastery, items.size());
            masteryList.add(kp);
            if (MasteryCalculator.isWeakPoint(mastery, items.size())) {
                weakPoints.add(kp);
            }
        }
        masteryList.sort(Comparator.comparingDouble(KpMasteryVO::getMastery));
        weakPoints.sort(Comparator.comparingDouble(KpMasteryVO::getMastery));
        vo.setKpMastery(masteryList);
        vo.setWeakPoints(weakPoints);
        int totalWeight = masteryList.stream().mapToInt(KpMasteryVO::getQuestionCount).sum();
        vo.setOverallMastery(totalWeight == 0 ? 0.0
                : masteryList.stream().mapToDouble(m -> m.getMastery() * m.getQuestionCount()).sum()
                / totalWeight);

        vo.setByType(breakdown(records, questionById, true));
        vo.setByDifficulty(breakdown(records, questionById, false));
        buildSuggestions(vo);
        return vo;
    }

    private List<BreakdownVO> breakdown(List<PracticeRecord> records,
                                        Map<Long, Question> questionById,
                                        boolean byType) {
        Map<String, int[]> values = new LinkedHashMap<>();
        for (PracticeRecord record : records) {
            Question question = questionById.get(record.getQuestionId());
            if (question == null || record.getSubmittedAt() == null) continue;
            int key = byType ? question.getType() : question.getDifficulty();
            String label = byType
                    ? (key >= 1 && key < TYPE_LABELS.length ? TYPE_LABELS[key] : "其他")
                    : (key >= 1 && key < DIFFICULTY_LABELS.length ? DIFFICULTY_LABELS[key] : "其他");
            int[] row = values.computeIfAbsent(label, k -> new int[4]);
            row[0]++;
            row[3] += record.getDurationSec() == null ? 0 : record.getDurationSec();
            if (record.getIsCorrect() != null) {
                row[1]++;
                if (Boolean.TRUE.equals(record.getIsCorrect())) row[2]++;
            }
        }
        List<BreakdownVO> result = new ArrayList<>();
        for (Map.Entry<String, int[]> entry : values.entrySet()) {
            int[] row = entry.getValue();
            result.add(new BreakdownVO(entry.getKey(), entry.getKey(), row[0], row[1], row[2],
                    row[1] == 0 ? 0.0 : (double) row[2] / row[1],
                    row[0] == 0 ? 0 : row[3] / row[0]));
        }
        return result;
    }

    private void buildSuggestions(LearningOverviewVO vo) {
        List<ActionSuggestionVO> suggestions = new ArrayList<>(vo.getSuggestions());
        if (vo.getReviewStats() != null && vo.getReviewStats().getDueToday() > 0) {
            suggestions.add(new ActionSuggestionVO(
                    "先清今日复习",
                    "有 " + vo.getReviewStats().getDueToday() + " 题到期，逾期会越滚越多",
                    "/review"));
        }
        if (!vo.getWeakPoints().isEmpty()) {
            KpMasteryVO weak = vo.getWeakPoints().get(0);
            suggestions.add(new ActionSuggestionVO(
                    "补强「" + weak.getName() + "」",
                    "近 " + vo.getDays() + " 天掌握度 " + Math.round(weak.getMastery() * 100)
                            + "%，练一组同类题",
                    "/practice?kpId=" + weak.getKpId()));
        }
        if (vo.getPendingCount() > 0) {
            suggestions.add(new ActionSuggestionVO(
                    "有始有终",
                    "有 " + vo.getPendingCount() + " 题开始了但没提交，先收尾",
                    "/practice"));
        }
        if (vo.getGradedCount() >= 3 && vo.getCorrectRate() < 0.6) {
            suggestions.add(new ActionSuggestionVO(
                    "正确率偏低",
                    "近 " + vo.getDays() + " 天已判分正确率 "
                            + Math.round(vo.getCorrectRate() * 100) + "%，建议降难度巩固",
                    "/practice"));
        }
        if (suggestions.isEmpty()) {
            suggestions.add(new ActionSuggestionVO(
                    "保持节奏", "继续按知识点练习，三天后再看趋势", "/practice"));
        }
        vo.setSuggestions(suggestions.stream().limit(4).toList());
    }

    private ReviewStatsVO safeReviewStats() {
        try {
            return reviewService.stats();
        } catch (Exception e) {
            return null;
        }
    }

    private static int streak(List<PracticeRecord> recentDesc) {
        int streak = 0;
        for (PracticeRecord record : recentDesc) {
            if (!Boolean.TRUE.equals(record.getIsCorrect())) break;
            streak++;
        }
        return streak;
    }
}

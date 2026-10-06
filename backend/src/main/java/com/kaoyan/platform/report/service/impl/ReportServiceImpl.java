package com.kaoyan.platform.report.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaoyan.platform.common.support.CurrentUser;
import com.kaoyan.platform.practice.entity.PracticeRecord;
import com.kaoyan.platform.practice.mapper.PracticeRecordMapper;
import com.kaoyan.platform.question.entity.Question;
import com.kaoyan.platform.question.mapper.KnowledgePointMapper;
import com.kaoyan.platform.question.mapper.QuestionMapper;
import com.kaoyan.platform.report.domain.MasteryCalculator;
import com.kaoyan.platform.report.entity.DailyReport;
import com.kaoyan.platform.report.mapper.DailyReportMapper;
import com.kaoyan.platform.report.service.ReportService;
import com.kaoyan.platform.report.vo.DailyReportVO;
import com.kaoyan.platform.report.vo.KpMasteryVO;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportServiceImpl implements ReportService {

    private static final TypeReference<List<KpMasteryVO>> KP_LIST = new TypeReference<>() {
    };

    private final DailyReportMapper dailyReportMapper;
    private final PracticeRecordMapper practiceRecordMapper;
    private final QuestionMapper questionMapper;
    private final KnowledgePointMapper knowledgePointMapper;
    private final CurrentUser currentUser;
    private final ObjectMapper objectMapper;

    public ReportServiceImpl(DailyReportMapper dailyReportMapper,
                             PracticeRecordMapper practiceRecordMapper,
                             QuestionMapper questionMapper,
                             KnowledgePointMapper knowledgePointMapper,
                             CurrentUser currentUser,
                             ObjectMapper objectMapper) {
        this.dailyReportMapper = dailyReportMapper;
        this.practiceRecordMapper = practiceRecordMapper;
        this.questionMapper = questionMapper;
        this.knowledgePointMapper = knowledgePointMapper;
        this.currentUser = currentUser;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public DailyReportVO getDaily(LocalDate date) {
        Long userId = currentUser.getUserId();
        DailyReport stored = dailyReportMapper.selectByUserAndDate(userId, date);
        if (stored != null) {
            return toVO(stored);
        }
        DailyReport report = generate(userId, date);
        dailyReportMapper.upsert(report);
        return toVO(report);
    }

    private DailyReport generate(Long userId, LocalDate date) {
        PracticeRecordMapper.DailyAgg agg = practiceRecordMapper.selectDailyAgg(userId, date);
        DailyReport report = new DailyReport();
        report.setUserId(userId);
        report.setReportDate(date);
        report.setTotalCount(agg == null || agg.getTotalCount() == null ? 0 : agg.getTotalCount());
        report.setCorrectCount(agg == null || agg.getCorrectCount() == null ? 0 : agg.getCorrectCount());
        report.setAvgDurationSec(agg == null || agg.getAvgDurationSec() == null ? 0 : agg.getAvgDurationSec());

        Map<Long, List<MasteryCalculator.KpItem>> itemsByKp = new LinkedHashMap<>();
        Map<Long, String> kpNames = new HashMap<>();
        for (PracticeRecord record : latestPerQuestion(userId, date).values()) {
            Question question = questionMapper.selectById(record.getQuestionId());
            if (question == null) {
                continue;
            }
            List<PracticeRecord> recent = practiceRecordMapper.selectRecentByQuestion(
                    userId, record.getQuestionId(), MasteryCalculator.RECENT_WINDOW);
            int correct = (int) recent.stream().filter(r -> Boolean.TRUE.equals(r.getIsCorrect())).count();
            double mastery = MasteryCalculator.questionMastery(
                    recent.size(),
                    correct,
                    record.getDurationSec() == null ? 0 : record.getDurationSec(),
                    MasteryCalculator.baselineSeconds(question.getDifficulty()),
                    streak(recent));

            List<Long> kpIds = questionMapper.selectKpIds(question.getId());
            if (!kpIds.isEmpty()) {
                knowledgePointMapper.selectByIds(kpIds).forEach(kp -> kpNames.put(kp.getId(), kp.getName()));
            }
            for (Long kpId : kpIds) {
                itemsByKp.computeIfAbsent(kpId, key -> new ArrayList<>())
                        .add(new MasteryCalculator.KpItem(mastery, question.getDifficulty()));
            }
        }

        List<KpMasteryVO> masteryList = new ArrayList<>();
        List<KpMasteryVO> weakPoints = new ArrayList<>();
        for (Map.Entry<Long, List<MasteryCalculator.KpItem>> entry : itemsByKp.entrySet()) {
            List<MasteryCalculator.KpItem> items = entry.getValue();
            double mastery = MasteryCalculator.kpMastery(items);
            KpMasteryVO vo = new KpMasteryVO(
                    entry.getKey(), kpNames.getOrDefault(entry.getKey(), ""), mastery, items.size());
            masteryList.add(vo);
            if (MasteryCalculator.isWeakPoint(mastery, items.size())) {
                weakPoints.add(vo);
            }
        }
        report.setKpMastery(writeJson(masteryList));
        report.setWeakPoints(writeJson(weakPoints));
        return report;
    }

    /** 一天内同一题做了多次只取最后一次，避免同一道题重复计入知识点掌握度 */
    private Map<Long, PracticeRecord> latestPerQuestion(Long userId, LocalDate date) {
        Map<Long, PracticeRecord> latest = new LinkedHashMap<>();
        for (PracticeRecord record : practiceRecordMapper.selectByUserAndDate(userId, date)) {
            PracticeRecord current = latest.get(record.getQuestionId());
            if (current == null || record.getId() > current.getId()) {
                latest.put(record.getQuestionId(), record);
            }
        }
        return latest;
    }

    /** 连续答对：入参最新在前，从最近一次往回数到第一次答错为止 */
    private static int streak(List<PracticeRecord> recentDesc) {
        int streak = 0;
        for (PracticeRecord record : recentDesc) {
            if (!Boolean.TRUE.equals(record.getIsCorrect())) {
                break;
            }
            streak++;
        }
        return streak;
    }

    @Override
    public List<DailyReportVO> getRange(LocalDate from, LocalDate to) {
        return dailyReportMapper.selectRange(currentUser.getUserId(), from, to).stream()
                .map(this::toVO)
                .toList();
    }

    private DailyReportVO toVO(DailyReport report) {
        DailyReportVO vo = new DailyReportVO();
        vo.setReportDate(report.getReportDate());
        vo.setTotalCount(report.getTotalCount());
        vo.setCorrectCount(report.getCorrectCount());
        vo.setCorrectRate(report.getTotalCount() == 0
                ? 0.0
                : (double) report.getCorrectCount() / report.getTotalCount());
        vo.setAvgDurationSec(report.getAvgDurationSec());
        vo.setKpMastery(readJson(report.getKpMastery()));
        vo.setWeakPoints(readJson(report.getWeakPoints()));
        return vo;
    }

    private String writeJson(List<KpMasteryVO> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("报告快照序列化失败", e);
        }
    }

    private List<KpMasteryVO> readJson(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, KP_LIST);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("报告快照反序列化失败", e);
        }
    }
}
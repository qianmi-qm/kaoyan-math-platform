package com.kaoyan.platform.practice.service.impl;

import com.kaoyan.platform.common.exception.BusinessException;
import com.kaoyan.platform.common.result.ErrorCode;
import com.kaoyan.platform.common.support.CurrentUser;
import com.kaoyan.platform.practice.dto.StartSessionDTO;
import com.kaoyan.platform.practice.dto.SubmitAnswerDTO;
import com.kaoyan.platform.practice.entity.PracticeRecord;
import com.kaoyan.platform.practice.mapper.PracticeRecordMapper;
import com.kaoyan.platform.practice.service.PracticeService;
import com.kaoyan.platform.practice.vo.PracticeRecordVO;
import com.kaoyan.platform.question.entity.KnowledgePoint;
import com.kaoyan.platform.question.entity.Question;
import com.kaoyan.platform.question.mapper.KnowledgePointMapper;
import com.kaoyan.platform.question.mapper.QuestionKpRow;
import com.kaoyan.platform.question.mapper.QuestionMapper;
import com.kaoyan.platform.report.cache.ReportCacheService;
import com.kaoyan.platform.review.service.ReviewService;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PracticeServiceImpl implements PracticeService {

    private final PracticeRecordMapper practiceRecordMapper;
    private final QuestionMapper questionMapper;
    private final KnowledgePointMapper knowledgePointMapper;
    private final ReviewService reviewService;
    private final CurrentUser currentUser;
    private final Clock clock;
    private final ReportCacheService reportCache;

    public PracticeServiceImpl(PracticeRecordMapper practiceRecordMapper,
                               QuestionMapper questionMapper,
                               KnowledgePointMapper knowledgePointMapper,
                               ReviewService reviewService,
                               CurrentUser currentUser,
                               Clock clock,
                               ReportCacheService reportCache) {
        this.practiceRecordMapper = practiceRecordMapper;
        this.questionMapper = questionMapper;
        this.knowledgePointMapper = knowledgePointMapper;
        this.reviewService = reviewService;
        this.currentUser = currentUser;
        this.clock = clock;
        this.reportCache = reportCache;
    }

    @Override
    @Transactional
    public List<PracticeRecordVO> startSession(StartSessionDTO dto) {
        Long userId = currentUser.getUserId();
        List<Question> questions = pickQuestions(dto, userId);
        if (questions.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "没有可练习的题目");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDate today = LocalDate.now(clock);
        List<PracticeRecord> records = new ArrayList<>(questions.size());
        for (Question question : questions) {
            PracticeRecord record = new PracticeRecord();
            record.setUserId(userId);
            record.setQuestionId(question.getId());
            record.setStartedAt(now);
            record.setPracticeDate(today);
            records.add(record);
        }
        practiceRecordMapper.insertBatch(records);
        KpContext kpContext = loadKpContext(questions.stream().map(Question::getId).toList());
        List<PracticeRecordVO> vos = new ArrayList<>(records.size());
        for (int i = 0; i < records.size(); i++) {
            vos.add(toVO(records.get(i), questions.get(i), false, kpContext));
        }
        return vos;
    }

    private List<Question> pickQuestions(StartSessionDTO dto, Long userId) {
        String mode = dto.getMode().trim().toUpperCase(Locale.ROOT);
        return switch (mode) {
            case "KP" -> {
                if (dto.getKpId() == null) {
                    throw new BusinessException(ErrorCode.PARAM_ERROR, "mode=KP 时 kpId 必填");
                }
                yield questionMapper.selectRandomByKp(dto.getKpId(), userId, dto.getCount());
            }
            case "RANDOM" -> questionMapper.selectRandomByKp(null, userId, dto.getCount());
            case "WRONG" -> questionMapper.selectWrongQuestions(userId, dto.getCount());
            case "QUESTION" -> {
                if (dto.getQuestionId() == null) {
                    throw new BusinessException(ErrorCode.PARAM_ERROR, "mode=QUESTION 时 questionId 必填");
                }
                Question question = questionMapper.selectById(dto.getQuestionId());
                if (question == null) {
                    throw new BusinessException(ErrorCode.NOT_FOUND, "题目不存在");
                }
                yield List.of(question);
            }
            default -> throw new BusinessException(ErrorCode.PARAM_ERROR, "不支持的选题方式：" + dto.getMode());
        };
    }

    /**
     * 用时一律由服务端算：clock.now() - record.startedAt，前端传什么都不信。
     * 客观题(1/2) trim + 忽略大小写比对；大题(3) 读自评，未自评则 isCorrect 置 null（不参与正确率）。
     */
    @Override
    @Transactional
    public PracticeRecordVO submit(Long recordId, SubmitAnswerDTO dto) {
        Long userId = currentUser.getUserId();
        PracticeRecord record = practiceRecordMapper.selectById(recordId);
        if (record == null || !record.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "做题记录不存在：" + recordId);
        }
        if (record.getSubmittedAt() != null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "该题已提交，不能重复提交");
        }
        Question question = questionMapper.selectById(record.getQuestionId());
        if (question == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "题目不存在：" + record.getQuestionId());
        }

        LocalDateTime now = LocalDateTime.now(clock);
        record.setDurationSec((int) Math.max(0, Duration.between(record.getStartedAt(), now).getSeconds()));
        record.setSubmittedAt(now);
        record.setSubmittedAnswer(dto.getAnswer());
        Boolean correct;
        if (question.getType() == 3) {
            // 大题：selfEval 为空表示"先提交占位答案，稍后两步自评"
            correct = dto.getSelfEval();
        } else {
            correct = normalize(dto.getAnswer()).equals(normalize(question.getAnswer()));
        }
        record.setIsCorrect(correct);
        int updated = practiceRecordMapper.updateSubmit(record);
        if (updated == 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "该题已提交，不能重复提交");
        }

        if (Boolean.FALSE.equals(record.getIsCorrect())) {
            reviewService.enqueueIfWrong(userId, record.getQuestionId());
        }
        // 先更 DB 再删缓存（Cache-Aside）
        reportCache.evict(userId, record.getPracticeDate());
        return toVO(record, question, true);
    }

    @Override
    @Transactional
    public PracticeRecordVO selfEvaluate(Long recordId, boolean correct) {
        Long userId = currentUser.getUserId();
        PracticeRecord record = practiceRecordMapper.selectById(recordId);
        if (record == null || !record.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "做题记录不存在：" + recordId);
        }
        Question question = questionMapper.selectById(record.getQuestionId());
        if (question == null || question.getType() != 3) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "只有大题才需要自评");
        }
        if (record.getSubmittedAt() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请先提交答案");
        }
        if (record.getIsCorrect() != null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "该题已经自评过");
        }
        int updated = practiceRecordMapper.updateSelfEval(recordId, correct);
        if (updated == 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "该题已经自评过");
        }
        record.setIsCorrect(correct);
        if (!correct) {
            reviewService.enqueueIfWrong(userId, record.getQuestionId());
        }
        reportCache.evict(userId, record.getPracticeDate());
        return toVO(record, question, true);
    }

    @Override
    public List<PracticeRecordVO> listByDate(LocalDate date) {
        Long userId = currentUser.getUserId();
        List<PracticeRecord> records = practiceRecordMapper.selectByUserAndDate(userId, date);
        List<Long> questionIds = records.stream().map(PracticeRecord::getQuestionId).distinct().toList();
        Map<Long, Question> questionById = new HashMap<>();
        if (!questionIds.isEmpty()) {
            for (Question question : questionMapper.selectByIds(questionIds)) {
                questionById.put(question.getId(), question);
            }
        }
        KpContext kpContext = loadKpContext(questionIds);
        List<PracticeRecordVO> vos = new ArrayList<>(records.size());
        for (PracticeRecord record : records) {
            boolean withAnswer = record.getSubmittedAt() != null;
            vos.add(toVO(record, questionById.get(record.getQuestionId()), withAnswer, kpContext));
        }
        return vos;
    }

    private static String normalize(String answer) {
        return answer == null ? "" : answer.trim().toLowerCase(Locale.ROOT);
    }

    private PracticeRecordVO toVO(PracticeRecord record, Question question, boolean withAnswer) {
        KpContext context = loadKpContext(question == null ? List.of() : List.of(question.getId()));
        return toVO(record, question, withAnswer, context);
    }

    private PracticeRecordVO toVO(PracticeRecord record, Question question, boolean withAnswer,
                                  KpContext kpContext) {
        PracticeRecordVO vo = new PracticeRecordVO();
        vo.setRecordId(record.getId());
        vo.setQuestionId(record.getQuestionId());
        vo.setIsCorrect(record.getIsCorrect());
        vo.setDurationSec(record.getDurationSec());
        vo.setStartedAt(record.getStartedAt());
        vo.setSubmittedAt(record.getSubmittedAt());
        if (question != null) {
            vo.setType(question.getType());
            vo.setContent(question.getContent());
            vo.setOptions(question.getOptions());
            vo.setDifficulty(question.getDifficulty());
            List<Long> kpIds = kpContext.idsByQuestion().getOrDefault(question.getId(), List.of());
            vo.setKpIds(kpIds);
            vo.setKpNames(kpIds.stream()
                    .map(kpContext.byId()::get)
                    .filter(java.util.Objects::nonNull)
                    .map(KnowledgePoint::getName)
                    .toList());
            if (withAnswer) {
                vo.setCorrectAnswer(question.getAnswer());
                vo.setAnalysis(question.getAnalysis());
            }
        }
        return vo;
    }

    private KpContext loadKpContext(List<Long> questionIds) {
        if (questionIds.isEmpty()) {
            return new KpContext(Map.of(), Map.of());
        }
        Map<Long, List<Long>> idsByQuestion = new HashMap<>();
        Set<Long> kpIds = new HashSet<>();
        for (QuestionKpRow row : questionMapper.selectKpRowsByQuestionIds(questionIds)) {
            idsByQuestion.computeIfAbsent(row.getQuestionId(), key -> new ArrayList<>()).add(row.getKpId());
            kpIds.add(row.getKpId());
        }
        Map<Long, KnowledgePoint> byId = new HashMap<>();
        if (!kpIds.isEmpty()) {
            for (KnowledgePoint kp : knowledgePointMapper.selectByIds(new ArrayList<>(kpIds))) {
                byId.put(kp.getId(), kp);
            }
        }
        return new KpContext(idsByQuestion, byId);
    }

    private record KpContext(Map<Long, List<Long>> idsByQuestion, Map<Long, KnowledgePoint> byId) {
    }
}
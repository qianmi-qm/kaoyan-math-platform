package com.kaoyan.platform.practice.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

import com.kaoyan.platform.common.exception.BusinessException;
import com.kaoyan.platform.common.support.CurrentUser;
import com.kaoyan.platform.practice.dto.SubmitAnswerDTO;
import com.kaoyan.platform.practice.entity.PracticeRecord;
import com.kaoyan.platform.practice.mapper.PracticeRecordMapper;
import com.kaoyan.platform.question.entity.Question;
import com.kaoyan.platform.question.mapper.KnowledgePointMapper;
import com.kaoyan.platform.question.mapper.QuestionMapper;
import com.kaoyan.platform.review.service.ReviewService;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PracticeServiceImplTest {

    @Mock
    private PracticeRecordMapper practiceRecordMapper;
    @Mock
    private QuestionMapper questionMapper;
    @Mock
    private KnowledgePointMapper knowledgePointMapper;
    @Mock
    private ReviewService reviewService;
    @Mock
    private CurrentUser currentUser;

    private PracticeServiceImpl service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-10-18T12:00:00Z"), ZoneId.of("Asia/Shanghai"));
        service = new PracticeServiceImpl(practiceRecordMapper, questionMapper, knowledgePointMapper,
                reviewService, currentUser, clock);
        when(currentUser.getUserId()).thenReturn(1L);
    }

    @Test
    @DisplayName("按日复盘：未提交的记录不返回答案和解析")
    void listByDateHidesAnswerBeforeSubmit() {
        PracticeRecord submitted = record(1L, 10L, true);
        PracticeRecord pending = record(2L, 11L, false);
        when(practiceRecordMapper.selectByUserAndDate(1L, LocalDate.of(2026, 10, 18)))
                .thenReturn(List.of(submitted, pending));
        when(questionMapper.selectByIds(anyList()))
                .thenReturn(List.of(question(10L), question(11L)));
        when(questionMapper.selectKpRowsByQuestionIds(anyList())).thenReturn(List.of());

        var rows = service.listByDate(LocalDate.of(2026, 10, 18));

        assertEquals("B", rows.get(0).getCorrectAnswer());
        assertEquals("解析", rows.get(0).getAnalysis());
        assertNull(rows.get(1).getCorrectAnswer());
        assertNull(rows.get(1).getAnalysis());
    }

    @Test
    @DisplayName("重复提交：已提交记录直接拒绝")
    void submitRejectsAlreadySubmitted() {
        when(practiceRecordMapper.selectById(1L)).thenReturn(record(1L, 10L, true));
        SubmitAnswerDTO dto = new SubmitAnswerDTO();
        dto.setAnswer("B");

        assertThrows(BusinessException.class, () -> service.submit(1L, dto));
    }

    private PracticeRecord record(Long id, Long questionId, boolean submitted) {
        PracticeRecord record = new PracticeRecord();
        record.setId(id);
        record.setUserId(1L);
        record.setQuestionId(questionId);
        record.setStartedAt(LocalDateTime.of(2026, 10, 18, 19, 0));
        record.setPracticeDate(LocalDate.of(2026, 10, 18));
        if (submitted) {
            record.setSubmittedAt(LocalDateTime.of(2026, 10, 18, 19, 1));
            record.setDurationSec(60);
            record.setIsCorrect(true);
        }
        return record;
    }

    private Question question(Long id) {
        Question question = new Question();
        question.setId(id);
        question.setType(1);
        question.setContent("题" + id);
        question.setAnswer("B");
        question.setAnalysis("解析");
        question.setDifficulty(2);
        return question;
    }
}

package com.kaoyan.platform.report.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.kaoyan.platform.practice.entity.PracticeRecord;
import com.kaoyan.platform.question.entity.Question;
import com.kaoyan.platform.report.vo.KpMasteryVO;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MasteryAggregatorTest {

    @Test
    @DisplayName("题目掌握度：连续答对、按时完成接近 1")
    void questionMastery() {
        Question question = new Question();
        question.setDifficulty(1);
        PracticeRecord r1 = record(1L, true, 50);
        PracticeRecord r2 = record(2L, true, 55);
        PracticeRecord r3 = record(3L, true, 60);

        double value = MasteryAggregator.questionMastery(question, List.of(r3, r2, r1));
        assertEquals(1.0, value, 0.0001);
    }

    @Test
    @DisplayName("整体掌握度：按题量加权")
    void overallWeighted() {
        double value = MasteryAggregator.overall(List.of(
                new KpMasteryVO(1L, "A", 1.0, 3),
                new KpMasteryVO(2L, "B", 0.0, 1)
        ));
        assertEquals(0.75, value, 0.0001);
    }

    private PracticeRecord record(Long id, boolean correct, int duration) {
        PracticeRecord record = new PracticeRecord();
        record.setId(id);
        record.setIsCorrect(correct);
        record.setDurationSec(duration);
        return record;
    }
}

package com.kaoyan.platform.ai.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AnswerFingerprintTest {

    @Test
    @DisplayName("答案指纹：归一化空格/大小写，不同答案必须不同")
    void hashNormalizesAndSeparatesAnswers() {
        assertEquals(64, AnswerFingerprint.hash("A").length());
        assertEquals(AnswerFingerprint.hash("A"), AnswerFingerprint.hash(" a "));
        assertEquals(AnswerFingerprint.hash(null), AnswerFingerprint.hash("  "));
        assertNotEquals(AnswerFingerprint.hash("A"), AnswerFingerprint.hash("C"));
    }
}

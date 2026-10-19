package com.kaoyan.platform.practice.support;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AnswerMatcherTest {

    @Test
    @DisplayName("选择题：字母忽略大小写")
    void matchesLetter() {
        assertTrue(AnswerMatcher.matches("A", "a"));
        assertFalse(AnswerMatcher.matches("A", "B"));
    }

    @Test
    @DisplayName("填空题：分数与小数等价")
    void matchesFractionAndDecimal() {
        assertTrue(AnswerMatcher.matches("0.5", "1/2"));
        assertTrue(AnswerMatcher.matches("1.5", "3/2"));
        assertTrue(AnswerMatcher.matches("7/3", "2.3333"));
        assertFalse(AnswerMatcher.matches("7/3", "2.3"));
    }

    @Test
    @DisplayName("填空题：LaTeX 分数")
    void matchesLatexFraction() {
        assertTrue(AnswerMatcher.matches("1/2", "$\\frac{1}{2}$"));
        assertTrue(AnswerMatcher.matches("\\dfrac{3}{2}", "1.5"));
    }
}

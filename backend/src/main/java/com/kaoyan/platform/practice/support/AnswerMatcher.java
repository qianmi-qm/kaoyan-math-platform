package com.kaoyan.platform.practice.support;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 填空/客观题答案比较：支持分数、小数、简单 LaTeX \frac。
 * 纯符号题仍做忽略大小写的字符串比较；数值题允许 1e-4 误差。
 */
public final class AnswerMatcher {

    private static final Pattern FRAC = Pattern.compile(
            "\\\\[dt]?frac\\s*\\{([^{}]+)\\}\\s*\\{([^{}]+)\\}");
    private static final Pattern NUMBER = Pattern.compile(
            "^[+-]?(?:\\d+(?:\\.\\d*)?|\\.\\d+)(?:\\s*/\\s*[+-]?(?:\\d+(?:\\.\\d*)?|\\.\\d+))?$");

    private AnswerMatcher() {
    }

    public static boolean matches(String expected, String actual) {
        if (expected == null || actual == null) {
            return false;
        }
        String e = clean(expected);
        String a = clean(actual);
        if (e.equalsIgnoreCase(a)) {
            return true;
        }
        BigDecimal ne = parseNumber(e);
        BigDecimal na = parseNumber(a);
        if (ne != null && na != null) {
            return ne.subtract(na).abs().compareTo(new BigDecimal("0.0001")) <= 0;
        }
        return false;
    }

    private static String clean(String value) {
        String text = value.trim();
        Matcher frac = FRAC.matcher(text);
        StringBuffer buffer = new StringBuffer();
        while (frac.find()) {
            frac.appendReplacement(buffer, frac.group(1) + "/" + frac.group(2));
        }
        frac.appendTail(buffer);
        return buffer.toString()
                .replace("$", "")
                .replace("\\left", "")
                .replace("\\right", "")
                .replace("\\,", "")
                .replace("\\!", "")
                .replace("\\;", "")
                .replace(" ", "")
                .replace("（", "(")
                .replace("）", ")");
    }

    private static BigDecimal parseNumber(String value) {
        String text = value.trim();
        if (!NUMBER.matcher(text).matches()) {
            return null;
        }
        try {
            if (text.contains("/")) {
                String[] parts = text.split("/");
                BigDecimal denominator = new BigDecimal(parts[1].trim());
                if (denominator.compareTo(BigDecimal.ZERO) == 0) {
                    return null;
                }
                return new BigDecimal(parts[0].trim())
                        .divide(denominator, MathContext.DECIMAL64);
            }
            return new BigDecimal(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}

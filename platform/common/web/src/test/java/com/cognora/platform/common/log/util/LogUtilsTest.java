package com.cognora.platform.common.log.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LogUtilsTest {

    @Test
    void sanitizeForLogReplacesLineBreaks() {
        String result = LogUtils.sanitizeForLog("supplier-1\r\n[INFO] forged log line");

        assertEquals("supplier-1__[INFO] forged log line", result);
        assertFalse(result.contains("\r"));
        assertFalse(result.contains("\n"));
    }

    @Test
    void sanitizeForLogReplacesLoneCarriageReturnAndLineFeed() {
        assertEquals("a_b", LogUtils.sanitizeForLog("a\rb"));
        assertEquals("a_b", LogUtils.sanitizeForLog("a\nb"));
    }

    @Test
    void sanitizeForLogReplacesControlCharacters() {
        String input = "a" + (char) 0x00 + "b" + (char) 0x1F + "c" + (char) 0x7F + "d";

        String result = LogUtils.sanitizeForLog(input);

        assertEquals("a_b_c_d", result);
        assertFalse(result.matches(".*[\\x00-\\x1F\\x7F].*"));
    }

    @Test
    void sanitizeForLogKeepsReadableText() {
        String input = "供应商 SUP-2026-0001 id=42";

        assertEquals(input, LogUtils.sanitizeForLog(input));
    }

    @Test
    void sanitizeForLogRendersNullLikeStringValueOf() {
        assertEquals("null", LogUtils.sanitizeForLog(null));
    }

    @Test
    void sanitizeForLogTruncatesLongValues() {
        String input = "x".repeat(300);

        String result = LogUtils.sanitizeForLog(input);

        assertEquals(259, result.length());
        assertTrue(result.endsWith("..."));
    }
}

package com.cognora.platform.system.service.support;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CsvUtilsTest {

    @Test
    void parsesQuotedFieldsWithCommasAndEscapedQuotes() {
        List<List<String>> rows = CsvUtils.parse("a,b\n\"x,y\",\"he said \"\"hi\"\"\"\n");

        assertEquals(2, rows.size());
        assertEquals(List.of("a", "b"), rows.get(0));
        assertEquals(List.of("x,y", "he said \"hi\""), rows.get(1));
    }

    @Test
    void parsesCrLfAndEmptyFields() {
        List<List<String>> rows = CsvUtils.parse("a,,c\r\nd,e,f\r\n");

        assertEquals(List.of("a", "", "c"), rows.get(0));
        assertEquals(List.of("d", "e", "f"), rows.get(1));
    }

    @Test
    void escapesFormulaInjectionAndQuotes() {
        assertEquals("'=cmd()", CsvUtils.escape("=cmd()"));
        assertEquals("\"a,b\"", CsvUtils.escape("a,b"));
        assertEquals("\"a\"\"b\"", CsvUtils.escape("a\"b"));
        assertTrue(CsvUtils.escape("@sum").startsWith("'"));
    }
}

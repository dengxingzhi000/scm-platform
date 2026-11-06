package com.cognora.lms.logistics.domain.sla;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 批次 3：SLA 判定边界用例。
 */
class SlaCalculatorTest {

    private static final LocalDateTime PROMISED = LocalDateTime.of(2026, 11, 6, 12, 0);

    @Test
    void deliveredBeforePromisedShouldBeOnTime() {
        SlaCalculator.SlaEvaluation result = SlaCalculator.evaluate(
                PROMISED, PROMISED.minusHours(1), PROMISED.plusDays(1));
        assertEquals(SlaCalculator.ON_TIME, result.status());
        assertEquals(0, result.overdueHours().signum());
        assertNull(result.level());
    }

    @Test
    void deliveredExactlyAtPromisedShouldBeOnTime() {
        SlaCalculator.SlaEvaluation result = SlaCalculator.evaluate(PROMISED, PROMISED, PROMISED.plusHours(2));
        assertEquals(SlaCalculator.ON_TIME, result.status());
    }

    @Test
    void deliveredAfterPromisedShouldBeOverdue() {
        SlaCalculator.SlaEvaluation result = SlaCalculator.evaluate(
                PROMISED, PROMISED.plusHours(3), PROMISED.plusDays(2));
        assertEquals(SlaCalculator.OVERDUE, result.status());
        assertEquals(0, result.overdueHours().compareTo(new java.math.BigDecimal("3.00")));
        assertEquals(SlaCalculator.LEVEL_WARN, result.level());
    }

    @Test
    void notDeliveredBeforePromisedShouldBeInProgress() {
        SlaCalculator.SlaEvaluation result = SlaCalculator.evaluate(PROMISED, null, PROMISED.minusMinutes(1));
        assertEquals(SlaCalculator.IN_PROGRESS, result.status());
        assertNull(result.level());
    }

    @Test
    void notDeliveredExactlyAtPromisedShouldStillBeInProgress() {
        SlaCalculator.SlaEvaluation result = SlaCalculator.evaluate(PROMISED, null, PROMISED);
        assertEquals(SlaCalculator.IN_PROGRESS, result.status());
    }

    @Test
    void notDeliveredJustAfterPromisedShouldBeOverdue() {
        SlaCalculator.SlaEvaluation result = SlaCalculator.evaluate(PROMISED, null, PROMISED.plusMinutes(1));
        assertEquals(SlaCalculator.OVERDUE, result.status());
        assertEquals(0, result.overdueHours().compareTo(new java.math.BigDecimal("0.02")));
        assertEquals(SlaCalculator.LEVEL_WARN, result.level());
    }

    @Test
    void overdueExactly24HoursShouldBeWarn() {
        SlaCalculator.SlaEvaluation result = SlaCalculator.evaluate(PROMISED, null, PROMISED.plusHours(24));
        assertEquals(SlaCalculator.LEVEL_WARN, result.level());
    }

    @Test
    void overdueMoreThan24HoursShouldBeCritical() {
        SlaCalculator.SlaEvaluation result = SlaCalculator.evaluate(
                PROMISED, null, PROMISED.plusHours(24).plusMinutes(1));
        assertEquals(SlaCalculator.LEVEL_CRITICAL, result.level());
    }

    @Test
    void missingPromisedTimeShouldBeNa() {
        SlaCalculator.SlaEvaluation result = SlaCalculator.evaluate(null, null, PROMISED);
        assertEquals(SlaCalculator.NA, result.status());
        assertNull(result.level());
    }
}

package com.cognora.scm.finance.domain.support;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FinanceAmountTest {

    @Test
    void keepsTwoDecimalAmount() {
        assertEquals("100.00", FinanceAmount.required(new BigDecimal("100.00"), "金额").toPlainString());
    }

    @Test
    void acceptsTrailingZerosBeyondTwoScale() {
        assertEquals("100.00", FinanceAmount.normalize(new BigDecimal("100.000"), "金额").toPlainString());
    }

    @Test
    void rejectsThirdDecimalInsteadOfSilentRounding() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> FinanceAmount.required(new BigDecimal("100.005"), "金额"));
        assertEquals("金额最多允许2位小数: 100.005", error.getMessage());
    }

    @Test
    void rejectsNullRequiredAmount() {
        assertThrows(IllegalArgumentException.class, () -> FinanceAmount.required(null, "金额"));
    }

    @Test
    void rejectsNegativeRequiredAmount() {
        assertThrows(IllegalArgumentException.class,
                () -> FinanceAmount.required(new BigDecimal("-1.00"), "金额"));
    }

    @Test
    void allowsNegativeForSignedAdjustment() {
        assertEquals("-5.00", FinanceAmount.signed(new BigDecimal("-5.00"), "调整金额").toPlainString());
    }

    @Test
    void allowsUpperBoundary() {
        assertEquals("99999999.99",
                FinanceAmount.required(new BigDecimal("99999999.99"), "金额").toPlainString());
    }

    @Test
    void rejectsAmountAboveCeiling() {
        assertThrows(IllegalArgumentException.class,
                () -> FinanceAmount.required(new BigDecimal("100000000.00"), "金额"));
    }

    @Test
    void positiveRejectsZero() {
        assertThrows(IllegalArgumentException.class,
                () -> FinanceAmount.positive(new BigDecimal("0.00"), "付款金额"));
    }

    @Test
    void optionalReturnsNullWhenAbsent() {
        assertNull(FinanceAmount.optional(null, "金额"));
    }

    @Test
    void optionalOrZeroDefaultsToZero() {
        assertEquals("0.00", FinanceAmount.optionalOrZero(null, "金额").toPlainString());
    }

    @Test
    void rateKeepsFourDecimals() {
        assertEquals("0.1300", FinanceAmount.rate(new BigDecimal("0.13"), "税率").toPlainString());
    }

    @Test
    void rateRejectsOutOfRange() {
        assertThrows(IllegalArgumentException.class,
                () -> FinanceAmount.rate(new BigDecimal("1.5"), "税率"));
    }

    @Test
    void rateRejectsMoreThanFourDecimals() {
        assertThrows(IllegalArgumentException.class,
                () -> FinanceAmount.rate(new BigDecimal("0.12345"), "税率"));
    }

    @Test
    void currencyDefaultsToCny() {
        assertEquals("CNY", FinanceAmount.currency(null));
        assertEquals("CNY", FinanceAmount.currency("  "));
    }

    @Test
    void currencyIsNormalizedToUpperCase() {
        assertEquals("USD", FinanceAmount.currency("usd"));
    }

    @Test
    void currencyRejectsNonIsoCode() {
        assertThrows(IllegalArgumentException.class, () -> FinanceAmount.currency("RMB1"));
    }
}

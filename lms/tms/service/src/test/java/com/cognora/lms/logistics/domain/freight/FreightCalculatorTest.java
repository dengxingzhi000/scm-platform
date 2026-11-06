package com.cognora.lms.logistics.domain.freight;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 批次 2：运费引擎边界用例（首重边界 / 续重进位 / 体积重 / 区域价 / 最低收费 / 舍入）。
 */
class FreightCalculatorTest {

    private FreightRule rule() {
        FreightRule rule = new FreightRule();
        rule.setFirstWeightKg(new BigDecimal("1"));
        rule.setFirstWeightFee(new BigDecimal("8"));
        rule.setAdditionalUnitKg(new BigDecimal("0.5"));
        rule.setAdditionalUnitFee(new BigDecimal("2"));
        rule.setVolumeDivisor(new BigDecimal("6000"));
        rule.setRegionSurcharge(BigDecimal.ZERO);
        rule.setMinCharge(BigDecimal.ZERO);
        return rule;
    }

    @Test
    void weightBelowFirstWeightShouldChargeFirstWeightOnly() {
        FreightQuote quote = FreightCalculator.calculate(rule(), new BigDecimal("0.8"), null);
        assertEquals(0, quote.totalAmount().compareTo(new BigDecimal("8.00")));
        assertEquals(0, quote.chargeableWeight().compareTo(new BigDecimal("0.8")));
    }

    @Test
    void weightExactlyFirstWeightShouldNotChargeAdditional() {
        FreightQuote quote = FreightCalculator.calculate(rule(), new BigDecimal("1.0"), null);
        assertEquals(0, quote.totalAmount().compareTo(new BigDecimal("8.00")));
        assertEquals(0, quote.billedWeight().compareTo(new BigDecimal("1.0")));
    }

    @Test
    void weightJustAboveFirstWeightShouldRoundUpToNextUnit() {
        FreightQuote quote = FreightCalculator.calculate(rule(), new BigDecimal("1.2"), null);
        // 1.2kg 按 0.5kg 进位 => 1.5kg，续重 1 个单位
        assertEquals(0, quote.billedWeight().compareTo(new BigDecimal("1.5")));
        assertEquals(0, quote.additionalFee().compareTo(new BigDecimal("2.00")));
        assertEquals(0, quote.totalAmount().compareTo(new BigDecimal("10.00")));
    }

    @Test
    void volumetricWeightShouldBeUsedWhenGreaterThanActualWeight() {
        // 600000 cm³ / 6000 = 100kg
        FreightQuote quote = FreightCalculator.calculate(rule(), new BigDecimal("5"), new BigDecimal("600000"));
        assertEquals(0, quote.volumetricWeight().compareTo(new BigDecimal("100")));
        assertEquals(0, quote.chargeableWeight().compareTo(new BigDecimal("100")));
        // 续重 (100-1)/0.5 = 198 个单位
        assertEquals(0, quote.totalAmount().compareTo(new BigDecimal("404.00")));
    }

    @Test
    void regionSurchargeAndMinimumChargeShouldApply() {
        FreightRule rule = rule();
        rule.setFirstWeightFee(new BigDecimal("1.50"));
        rule.setRegionSurcharge(new BigDecimal("3.00"));
        rule.setMinCharge(new BigDecimal("9.00"));
        FreightQuote quote = FreightCalculator.calculate(rule, new BigDecimal("0.5"), null);
        assertEquals(0, quote.regionSurcharge().compareTo(new BigDecimal("3.00")));
        assertEquals(0, quote.minCharge().compareTo(new BigDecimal("9.00")));
        assertTrue(quote.minChargeApplied());
        assertEquals(0, quote.totalAmount().compareTo(new BigDecimal("9.00")));
    }

    @Test
    void minimumChargeShouldNotApplyWhenSubtotalExceedsIt() {
        FreightRule rule = rule();
        rule.setMinCharge(new BigDecimal("9.00"));
        FreightQuote quote = FreightCalculator.calculate(rule, new BigDecimal("1.2"), null);
        assertFalse(quote.minChargeApplied());
        assertEquals(0, quote.minCharge().compareTo(new BigDecimal("9.00")));
        assertEquals(0, quote.totalAmount().compareTo(new BigDecimal("10.00")));
    }

    @Test
    void amountShouldRoundHalfUpToTwoDecimals() {
        FreightRule rule = rule();
        rule.setFirstWeightFee(new BigDecimal("8.005"));
        FreightQuote quote = FreightCalculator.calculate(rule, new BigDecimal("0.5"), null);
        assertEquals(new BigDecimal("8.01"), quote.totalAmount());
        assertEquals(new BigDecimal("8.01"), quote.firstWeightFee());
    }

    @Test
    void zeroWeightShouldStillChargeFirstWeight() {
        FreightQuote quote = FreightCalculator.calculate(rule(), BigDecimal.ZERO, BigDecimal.ZERO);
        assertEquals(0, quote.totalAmount().compareTo(new BigDecimal("8.00")));
    }

    @Test
    void negativeWeightOrVolumeShouldBeRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> FreightCalculator.calculate(rule(), new BigDecimal("-1"), null));
        assertThrows(IllegalArgumentException.class,
                () -> FreightCalculator.calculate(rule(), null, new BigDecimal("-1")));
    }

    @Test
    void invalidRuleShouldBeRejected() {
        FreightRule invalid = rule();
        invalid.setFirstWeightKg(BigDecimal.ZERO);
        assertThrows(IllegalArgumentException.class,
                () -> FreightCalculator.calculate(invalid, BigDecimal.ONE, null));

        FreightRule zeroDivisor = rule();
        zeroDivisor.setVolumeDivisor(BigDecimal.ZERO);
        assertThrows(IllegalArgumentException.class,
                () -> FreightCalculator.calculate(zeroDivisor, BigDecimal.ONE, BigDecimal.ONE));
    }

    @Test
    void diffClassifierShouldClassifyOverUnderAndMatch() {
        assertEquals(FreightDiffClassifier.MATCH,
                FreightDiffClassifier.classify(new BigDecimal("0.01")));
        assertEquals(FreightDiffClassifier.OVERCHARGE,
                FreightDiffClassifier.classify(new BigDecimal("0.02")));
        assertEquals(FreightDiffClassifier.UNDERCHARGE,
                FreightDiffClassifier.classify(new BigDecimal("-0.02")));
        assertEquals(0, FreightDiffClassifier.diff(new BigDecimal("50"), new BigDecimal("44"))
                .compareTo(new BigDecimal("6.00")));
    }
}

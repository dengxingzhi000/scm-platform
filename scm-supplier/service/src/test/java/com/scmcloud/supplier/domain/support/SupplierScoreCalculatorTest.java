package com.scmcloud.supplier.domain.support;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SupplierScoreCalculatorTest {

    @Test
    void weightedTotalShouldUseConfiguredWeights() {
        BigDecimal total = SupplierScoreCalculator.weightedTotal(
                new BigDecimal("90"), new BigDecimal("80"), new BigDecimal("70"), new BigDecimal("60"));

        assertEquals(new BigDecimal("77.00"), total);
    }

    @Test
    void weightedTotalShouldNormalizeWhenDimensionsMissing() {
        BigDecimal total = SupplierScoreCalculator.weightedTotal(
                new BigDecimal("90"), null, null, null);

        assertEquals(new BigDecimal("90.00"), total);
    }

    @Test
    void weightedTotalShouldClampToRange() {
        BigDecimal total = SupplierScoreCalculator.weightedTotal(
                new BigDecimal("120"), null, null, null);

        assertEquals(new BigDecimal("100.00"), total);

        BigDecimal negative = SupplierScoreCalculator.weightedTotal(
                new BigDecimal("-10"), null, null, null);
        assertEquals(new BigDecimal("0.00"), negative);
    }

    @Test
    void levelShouldFollowScoreThresholds() {
        assertEquals(1, SupplierScoreCalculator.levelFor(new BigDecimal("95")));
        assertEquals(2, SupplierScoreCalculator.levelFor(new BigDecimal("85")));
        assertEquals(3, SupplierScoreCalculator.levelFor(new BigDecimal("75")));
        assertEquals(4, SupplierScoreCalculator.levelFor(new BigDecimal("60")));
        assertEquals(4, SupplierScoreCalculator.levelFor(null));
    }

    @Test
    void averageShouldIgnoreNullValues() {
        List<BigDecimal> scores = Arrays.asList(new BigDecimal("80"), null, new BigDecimal("90"));

        assertEquals(new BigDecimal("85.00"), SupplierScoreCalculator.average(scores));
        assertNull(SupplierScoreCalculator.average(List.of()));
    }
}

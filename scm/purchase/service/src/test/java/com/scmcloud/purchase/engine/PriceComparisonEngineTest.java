package com.scmcloud.purchase.engine;

import com.scmcloud.decision.engine.ConstraintType;
import com.scmcloud.purchase.engine.PriceComparisonInput.SupplierQuote;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PriceComparisonEngineTest {

    @Test
    void ranksSuppliersByWeightedScore() {
        PriceScorer priceScorer = new PriceScorer();
        SupplierConstraint constraint = new SupplierConstraint();

        PriceComparisonEngine engine = new PriceComparisonEngine(
                List.of(priceScorer),
                List.of(constraint)
        );

        SupplierQuote q1 = new SupplierQuote();
        q1.setSupplierId("S1");
        q1.setSupplierName("Supplier A");
        q1.setUnitPrice(new BigDecimal("100"));
        q1.setSupplierStatus("ACTIVE");

        SupplierQuote q2 = new SupplierQuote();
        q2.setSupplierId("S2");
        q2.setSupplierName("Supplier B");
        q2.setUnitPrice(new BigDecimal("120"));
        q2.setSupplierStatus("ACTIVE");

        PriceComparisonInput input = new PriceComparisonInput();
        input.setQuotes(List.of(q1, q2));

        var rankings = engine.rank(input);

        assertEquals(2, rankings.size());
        assertEquals("S1", rankings.get(0).getSupplierId());
        assertTrue(rankings.get(0).getTotalScore() > rankings.get(1).getTotalScore());
    }

    @Test
    void filtersOutInactiveSuppliers() {
        SupplierConstraint constraint = new SupplierConstraint();
        PriceComparisonEngine engine = new PriceComparisonEngine(List.of(new PriceScorer()), List.of(constraint));

        SupplierQuote q1 = new SupplierQuote();
        q1.setSupplierId("S1");
        q1.setUnitPrice(new BigDecimal("100"));
        q1.setSupplierStatus("INACTIVE");

        PriceComparisonInput input = new PriceComparisonInput();
        input.setQuotes(List.of(q1));

        var rankings = engine.rank(input);

        assertEquals(1, rankings.size());
        assertFalse(rankings.get(0).getConstraintResults().stream()
                .allMatch(r -> r.getType() == ConstraintType.HARD && r.isPassed()));
    }
}

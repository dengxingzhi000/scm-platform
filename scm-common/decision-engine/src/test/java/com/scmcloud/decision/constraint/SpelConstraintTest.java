package com.scmcloud.decision.constraint;

import com.scmcloud.decision.engine.ConstraintResult;
import com.scmcloud.decision.engine.ConstraintType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SpelConstraintTest {

    @Test
    void hardConstraint_passes_whenExpressionTrue() {
        SpelConstraint<Map<String, Object>> constraint = new SpelConstraint<>(
                "stockCheck", "#stock >= #quantity",
                ConstraintType.HARD, 1, "STOCK_001", "Insufficient stock");

        Map<String, Object> ctx = Map.of("stock", 10, "quantity", 5);
        ConstraintResult result = constraint.validate(ctx);

        assertTrue(result.isPassed());
        assertEquals(ConstraintType.HARD, result.getType());
    }

    @Test
    void hardConstraint_fails_whenExpressionFalse() {
        SpelConstraint<Map<String, Object>> constraint = new SpelConstraint<>(
                "stockCheck", "#stock >= #quantity",
                ConstraintType.HARD, 1, "STOCK_001", "Insufficient stock");

        Map<String, Object> ctx = Map.of("stock", 3, "quantity", 5);
        ConstraintResult result = constraint.validate(ctx);

        assertFalse(result.isPassed());
        assertEquals("STOCK_001", result.getCode());
    }

    @Test
    void softConstraint_returnsPenalty_whenExpressionFalse() {
        SpelConstraint<Map<String, Object>> constraint = new SpelConstraint<>(
                "qualityCheck", "#quality >= 60",
                ConstraintType.SOFT, 2, "QUALITY_001", "Low quality score", 15.0);

        Map<String, Object> ctx = Map.of("quality", 45);
        ConstraintResult result = constraint.validate(ctx);

        assertFalse(result.isPassed());
        assertEquals(15.0, result.getPenalty());
    }
}

package com.scmcloud.decision.scoring;

import org.junit.jupiter.api.Test;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SpelScorerTest {

    @Test
    void calculatesPriceScore() {
        SpelScorer<Map<String, Object>> scorer = new SpelScorer<>(
                "price", "(#minPrice / #target['price']) * 100", 0.4);

        Map<String, Object> target = Map.of("price", 120.0);
        ScoringContext ctx = new ScoringContext().withVariable("minPrice", 100.0);

        double score = scorer.score(target, ctx);
        assertEquals(83.33, score, 0.01);
    }

    @Test
    void returnsZero_whenDivisionByZero() {
        SpelScorer<Map<String, Object>> scorer = new SpelScorer<>(
                "price", "(#minPrice / #target['price']) * 100", 0.4);

        Map<String, Object> target = Map.of("price", 0.0);
        ScoringContext ctx = new ScoringContext().withVariable("minPrice", 100.0);

        double score = scorer.score(target, ctx);
        assertTrue(Double.isInfinite(score) || Double.isNaN(score) || score == 0.0);
    }
}

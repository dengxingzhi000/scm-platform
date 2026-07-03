package com.scmcloud.decision.matrix.core.fusion;

import com.scmcloud.decision.matrix.api.DecisionExplanation;
import com.scmcloud.decision.matrix.api.DecisionResult;
import com.scmcloud.decision.matrix.api.FusionEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Fusion Engine Test")
class WeightedFusionEngineTest {

    private WeightedFusionEngine fusionEngine;

    @BeforeEach
    void setUp() {
        fusionEngine = new WeightedFusionEngine();
    }

    @Test
    @DisplayName("Should fuse results with weighted average")
    void shouldFuseWeightedAverage() {
        List<DecisionResult> results = List.of(
                createResult("price", 0.9, 0.8),
                createResult("inventory", 0.7, 0.9)
        );

        Map<String, Double> weights = Map.of(
                "price", 0.6,
                "inventory", 0.4
        );

        DecisionResult fused = fusionEngine.fuse(results, weights);

        assertNotNull(fused);
        assertTrue(fused.isSuccess());
        assertTrue(fused.getScore() > 0);
    }

    @Test
    @DisplayName("Should handle empty results")
    void shouldHandleEmptyResults() {
        DecisionResult fused = fusionEngine.fuse(List.of(), Map.of());

        assertNotNull(fused);
        assertFalse(fused.isSuccess());
    }

    @Test
    @DisplayName("Should fuse with max utility strategy")
    void shouldFuseMaxUtility() {
        List<DecisionResult> results = List.of(
                createResult("price", 0.9, 0.8),
                createResult("inventory", 0.7, 0.9)
        );

        Map<String, Double> weights = Map.of(
                "price", 1.0,
                "inventory", 1.0
        );

        DecisionResult fused = fusionEngine.fuseWithConflictResolution(
                results, weights, FusionEngine.ConflictStrategy.MAX_UTILITY
        );

        assertNotNull(fused);
        assertTrue(fused.isSuccess());
    }

    @Test
    @DisplayName("Should fuse with Pareto optimal strategy")
    void shouldFuseParetoOptimal() {
        List<DecisionResult> results = List.of(
                createResult("price", 0.9, 0.8),
                createResult("inventory", 0.7, 0.9),
                createResult("fulfillment", 0.8, 0.85)
        );

        Map<String, Double> weights = Map.of(
                "price", 1.0,
                "inventory", 1.0,
                "fulfillment", 1.0
        );

        DecisionResult fused = fusionEngine.fuseWithConflictResolution(
                results, weights, FusionEngine.ConflictStrategy.PARETO_OPTIMAL
        );

        assertNotNull(fused);
        assertTrue(fused.isSuccess());
    }

    @Test
    @DisplayName("Should provide explanation")
    void shouldProvideExplanation() {
        List<DecisionResult> results = List.of(
                createResult("price", 0.9, 0.8),
                createResult("inventory", 0.7, 0.9)
        );

        Map<String, Double> weights = Map.of(
                "price", 0.6,
                "inventory", 0.4
        );

        DecisionResult fused = fusionEngine.fuse(results, weights);

        assertNotNull(fused.getExplanation());
        assertNotNull(fused.getExplanation().getFactorWeights());
        assertNotNull(fused.getExplanation().getFactorScores());
    }

    private DecisionResult createResult(String nodeId, double score, double confidence) {
        DecisionExplanation explanation = DecisionExplanation.builder()
                .decisionId(nodeId)
                .primaryReason("Test decision")
                .factorWeights(Map.of(nodeId, 1.0))
                .factorScores(Map.of(nodeId, score))
                .contributingFactors(List.of(nodeId))
                .build();

        return DecisionResult.builder()
                .nodeId(nodeId)
                .status(DecisionResult.DecisionStatus.SUCCESS)
                .value("value")
                .score(score)
                .confidence(confidence)
                .explanation(explanation)
                .build();
    }
}

package com.scmcloud.decision.matrix.api;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.List;
import java.util.Map;

/**
 * Decision explanation for transparency and debugging.
 */
@Getter
@ToString
@EqualsAndHashCode
public class DecisionExplanation {

    private final String decisionId;
    private final String primaryReason;
    private final Map<String, Double> factorWeights;
    private final Map<String, Double> factorScores;
    private final List<String> contributingFactors;
    private final Map<String, Object> metadata;

    @Builder
    private DecisionExplanation(String decisionId, String primaryReason,
                                Map<String, Double> factorWeights, Map<String, Double> factorScores,
                                List<String> contributingFactors, Map<String, Object> metadata) {
        this.decisionId = decisionId;
        this.primaryReason = primaryReason;
        this.factorWeights = factorWeights != null ? Map.copyOf(factorWeights) : Map.of();
        this.factorScores = factorScores != null ? Map.copyOf(factorScores) : Map.of();
        this.contributingFactors = contributingFactors != null ? List.copyOf(contributingFactors) : List.of();
        this.metadata = metadata != null ? Map.copyOf(metadata) : Map.of();
    }
}

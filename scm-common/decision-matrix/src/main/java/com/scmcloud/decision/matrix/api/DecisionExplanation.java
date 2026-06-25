package com.scmcloud.decision.matrix.api;

import java.util.List;
import java.util.Map;

/**
 * Decision explanation for transparency and debugging.
 */
public class DecisionExplanation {

    private final String decisionId;
    private final String primaryReason;
    private final Map<String, Double> factorWeights;
    private final Map<String, Double> factorScores;
    private final List<String> contributingFactors;
    private final Map<String, Object> metadata;

    public DecisionExplanation(String decisionId, String primaryReason,
                               Map<String, Double> factorWeights, Map<String, Double> factorScores,
                               List<String> contributingFactors, Map<String, Object> metadata) {
        this.decisionId = decisionId;
        this.primaryReason = primaryReason;
        this.factorWeights = factorWeights != null ? factorWeights : Map.of();
        this.factorScores = factorScores != null ? factorScores : Map.of();
        this.contributingFactors = contributingFactors != null ? contributingFactors : List.of();
        this.metadata = metadata != null ? metadata : Map.of();
    }

    public String getDecisionId() {
        return decisionId;
    }

    public String getPrimaryReason() {
        return primaryReason;
    }

    public Map<String, Double> getFactorWeights() {
        return factorWeights;
    }

    public Map<String, Double> getFactorScores() {
        return factorScores;
    }

    public List<String> getContributingFactors() {
        return contributingFactors;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    @Override
    public String toString() {
        return String.format("Decision[%s]: %s (factors: %s)", decisionId, primaryReason, contributingFactors);
    }
}

package com.scmcloud.decision.matrix.api;

import java.util.List;
import java.util.Map;

/**
 * Decision result with explainability.
 */
public class DecisionResult {

    private final String nodeId;
    private final DecisionStatus status;
    private final Object value;
    private final double score;
    private final double confidence;
    private final DecisionExplanation explanation;
    private final Map<String, Object> metadata;
    private final List<String> warnings;

    public DecisionResult(String nodeId, DecisionStatus status, Object value,
                          double score, double confidence, DecisionExplanation explanation,
                          Map<String, Object> metadata, List<String> warnings) {
        this.nodeId = nodeId;
        this.status = status;
        this.value = value;
        this.score = score;
        this.confidence = confidence;
        this.explanation = explanation;
        this.metadata = metadata != null ? metadata : Map.of();
        this.warnings = warnings != null ? warnings : List.of();
    }

    public String getNodeId() {
        return nodeId;
    }

    public DecisionStatus getStatus() {
        return status;
    }

    public Object getValue() {
        return value;
    }

    public double getScore() {
        return score;
    }

    public double getConfidence() {
        return confidence;
    }

    public DecisionExplanation getExplanation() {
        return explanation;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public boolean isSuccess() {
        return status == DecisionStatus.SUCCESS;
    }

    public boolean isFailure() {
        return status == DecisionStatus.FAILURE;
    }

    public enum DecisionStatus {
        SUCCESS,
        FAILURE,
        TIMEOUT,
        SKIPPED,
        FALLBACK
    }
}

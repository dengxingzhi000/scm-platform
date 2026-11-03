package com.scmcloud.decision.matrix.api;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.List;
import java.util.Map;

/**
 * Decision result with explainability.
 */
@Getter
@ToString
@EqualsAndHashCode
public class DecisionResult {

    private final String nodeId;
    private final DecisionStatus status;
    private final Object value;
    private final double score;
    private final double confidence;
    private final DecisionExplanation explanation;
    private final Map<String, Object> metadata;
    private final List<String> warnings;

    @Builder
    private DecisionResult(String nodeId, DecisionStatus status, Object value,
                           double score, double confidence, DecisionExplanation explanation,
                           Map<String, Object> metadata, List<String> warnings) {
        this.nodeId = nodeId;
        this.status = status;
        this.value = value;
        this.score = score;
        this.confidence = confidence;
        this.explanation = explanation;
        this.metadata = metadata != null ? Map.copyOf(metadata) : Map.of();
        this.warnings = warnings != null ? List.copyOf(warnings) : List.of();
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

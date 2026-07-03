package com.scmcloud.decision.matrix.api;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.List;
import java.util.Map;

/**
 * Result of gateway execution.
 */
@Getter
@ToString
@EqualsAndHashCode
public class GatewayResult {

    private final String routeId;
    private final Map<String, ChainExecutionResult> chainResults;
    private final DecisionResult fusedResult;
    private final ExecutionResult executionResult;
    private final long totalDurationMs;
    private final DecisionExplanation explanation;

    @Builder
    private GatewayResult(String routeId, Map<String, ChainExecutionResult> chainResults,
                          DecisionResult fusedResult, ExecutionResult executionResult,
                          long totalDurationMs, DecisionExplanation explanation) {
        this.routeId = routeId;
        this.chainResults = chainResults != null ? Map.copyOf(chainResults) : Map.of();
        this.fusedResult = fusedResult;
        this.executionResult = executionResult;
        this.totalDurationMs = totalDurationMs;
        this.explanation = explanation;
    }

    public boolean isSuccess() {
        return fusedResult != null && fusedResult.isSuccess() &&
                (executionResult == null || executionResult.isSuccess());
    }

    public boolean hasExecution() {
        return executionResult != null;
    }

    public static GatewayResult failure(String routeId, String errorMessage) {
        return GatewayResult.builder()
                .routeId(routeId)
                .chainResults(Map.of())
                .fusedResult(DecisionResult.builder()
                        .nodeId("gateway")
                        .status(DecisionResult.DecisionStatus.FAILURE)
                        .score(0.0)
                        .confidence(0.0)
                        .warnings(List.of(errorMessage))
                        .build())
                .build();
    }
}

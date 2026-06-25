package com.scmcloud.decision.matrix.core.gateway;

import com.scmcloud.decision.matrix.api.*;

import java.util.List;
import java.util.Map;

/**
 * Result of gateway execution.
 */
public class GatewayResult {

    private final String routeId;
    private final Map<String, ChainExecutionResult> chainResults;
    private final DecisionResult fusedResult;
    private final ExecutionResult executionResult;
    private final long totalDurationMs;
    private final DecisionExplanation explanation;

    public GatewayResult(String routeId, Map<String, ChainExecutionResult> chainResults,
                          DecisionResult fusedResult, ExecutionResult executionResult,
                          long totalDurationMs, DecisionExplanation explanation) {
        this.routeId = routeId;
        this.chainResults = chainResults;
        this.fusedResult = fusedResult;
        this.executionResult = executionResult;
        this.totalDurationMs = totalDurationMs;
        this.explanation = explanation;
    }

    public String getRouteId() {
        return routeId;
    }

    public Map<String, ChainExecutionResult> getChainResults() {
        return chainResults;
    }

    public DecisionResult getFusedResult() {
        return fusedResult;
    }

    public ExecutionResult getExecutionResult() {
        return executionResult;
    }

    public long getTotalDurationMs() {
        return totalDurationMs;
    }

    public DecisionExplanation getExplanation() {
        return explanation;
    }

    public boolean isSuccess() {
        return fusedResult != null && fusedResult.isSuccess() &&
                (executionResult == null || executionResult.isSuccess());
    }

    public boolean hasExecution() {
        return executionResult != null;
    }

    public static GatewayResult failure(String routeId, String errorMessage) {
        return new GatewayResult(
                routeId, Map.of(),
                new DecisionResult("gateway", DecisionResult.DecisionStatus.FAILURE, null,
                        0.0, 0.0, null, null, List.of(errorMessage)),
                null, 0, null
        );
    }
}

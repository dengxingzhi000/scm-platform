package com.scmcloud.decision.matrix.core.gateway;

import com.scmcloud.decision.matrix.api.*;
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
@Builder
@ToString
@EqualsAndHashCode
public class GatewayResult {
    private final String routeId;
    private final Map<String, ChainExecutionResult> chainResults;
    private final DecisionResult fusedResult;
    private final ExecutionResult executionResult;
    private final long totalDurationMs;
    private final DecisionExplanation explanation;

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
                .fusedResult(new DecisionResult("gateway", DecisionResult.DecisionStatus.FAILURE, null,
                        0.0, 0.0, null, null, List.of(errorMessage)))
                .build();
    }
}

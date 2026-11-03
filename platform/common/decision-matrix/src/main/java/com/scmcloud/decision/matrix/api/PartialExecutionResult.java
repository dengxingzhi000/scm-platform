package com.scmcloud.decision.matrix.api;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.List;
import java.util.Map;

/**
 * Result of a partial execution.
 */
@Getter
@ToString
@EqualsAndHashCode
public class PartialExecutionResult {

    private final String executionId;
    private final Map<String, StepResult> stepResults;
    private final List<String> completedSteps;
    private final List<String> failedSteps;
    private final List<CompensationAction> compensations;
    private final long totalDurationMs;

    @Builder
    private PartialExecutionResult(String executionId, Map<String, StepResult> stepResults,
                                   List<String> completedSteps, List<String> failedSteps,
                                   List<CompensationAction> compensations, long totalDurationMs) {
        this.executionId = executionId;
        this.stepResults = stepResults != null ? Map.copyOf(stepResults) : Map.of();
        this.completedSteps = completedSteps != null ? List.copyOf(completedSteps) : List.of();
        this.failedSteps = failedSteps != null ? List.copyOf(failedSteps) : List.of();
        this.compensations = compensations != null ? List.copyOf(compensations) : List.of();
        this.totalDurationMs = totalDurationMs;
    }

    public boolean hasFailures() {
        return !failedSteps.isEmpty();
    }

    public boolean isFullyCompleted() {
        return failedSteps.isEmpty();
    }
}

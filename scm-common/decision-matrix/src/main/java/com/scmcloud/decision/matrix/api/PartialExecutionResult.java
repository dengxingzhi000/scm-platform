package com.scmcloud.decision.matrix.api;

import java.util.List;
import java.util.Map;

/**
 * Result of a partial execution.
 */
public class PartialExecutionResult {

    private final String executionId;
    private final Map<String, StepResult> stepResults;
    private final List<String> completedSteps;
    private final List<String> failedSteps;
    private final List<CompensationAction> compensations;
    private final long totalDurationMs;

    public PartialExecutionResult(String executionId, Map<String, StepResult> stepResults,
                                   List<String> completedSteps, List<String> failedSteps,
                                   List<CompensationAction> compensations, long totalDurationMs) {
        this.executionId = executionId;
        this.stepResults = stepResults != null ? stepResults : Map.of();
        this.completedSteps = completedSteps != null ? completedSteps : List.of();
        this.failedSteps = failedSteps != null ? failedSteps : List.of();
        this.compensations = compensations != null ? compensations : List.of();
        this.totalDurationMs = totalDurationMs;
    }

    public String getExecutionId() {
        return executionId;
    }

    public Map<String, StepResult> getStepResults() {
        return stepResults;
    }

    public List<String> getCompletedSteps() {
        return completedSteps;
    }

    public List<String> getFailedSteps() {
        return failedSteps;
    }

    public List<CompensationAction> getCompensations() {
        return compensations;
    }

    public long getTotalDurationMs() {
        return totalDurationMs;
    }

    public boolean hasFailures() {
        return !failedSteps.isEmpty();
    }

    public boolean isFullyCompleted() {
        return failedSteps.isEmpty();
    }
}

package com.scmcloud.decision.matrix.api;

import java.util.List;
import java.util.Map;

/**
 * Result of a saga execution.
 */
public class ExecutionResult {

    private final String executionId;
    private final ExecutionStatus status;
    private final Map<String, StepResult> stepResults;
    private final List<String> completedSteps;
    private final List<String> failedSteps;
    private final long totalDurationMs;
    private final String errorMessage;

    public ExecutionResult(String executionId, ExecutionStatus status,
                           Map<String, StepResult> stepResults, List<String> completedSteps,
                           List<String> failedSteps, long totalDurationMs, String errorMessage) {
        this.executionId = executionId;
        this.status = status;
        this.stepResults = stepResults != null ? stepResults : Map.of();
        this.completedSteps = completedSteps != null ? completedSteps : List.of();
        this.failedSteps = failedSteps != null ? failedSteps : List.of();
        this.totalDurationMs = totalDurationMs;
        this.errorMessage = errorMessage;
    }

    public String getExecutionId() {
        return executionId;
    }

    public ExecutionStatus getStatus() {
        return status;
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

    public long getTotalDurationMs() {
        return totalDurationMs;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public boolean isSuccess() {
        return status == ExecutionStatus.SUCCESS;
    }

    public enum ExecutionStatus {
        SUCCESS,
        FAILURE,
        COMPENSATED,
        PARTIAL_SUCCESS
    }
}

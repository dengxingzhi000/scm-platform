package com.scmcloud.decision.matrix.api;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.List;
import java.util.Map;

/**
 * Result of a saga execution.
 */
@Getter
@ToString
@EqualsAndHashCode
public class ExecutionResult {

    private final String executionId;
    private final ExecutionStatus status;
    private final Map<String, StepResult> stepResults;
    private final List<String> completedSteps;
    private final List<String> failedSteps;
    private final long totalDurationMs;
    private final String errorMessage;

    @Builder
    private ExecutionResult(String executionId, ExecutionStatus status,
                            Map<String, StepResult> stepResults, List<String> completedSteps,
                            List<String> failedSteps, long totalDurationMs, String errorMessage) {
        this.executionId = executionId;
        this.status = status;
        this.stepResults = stepResults != null ? Map.copyOf(stepResults) : Map.of();
        this.completedSteps = completedSteps != null ? List.copyOf(completedSteps) : List.of();
        this.failedSteps = failedSteps != null ? List.copyOf(failedSteps) : List.of();
        this.totalDurationMs = totalDurationMs;
        this.errorMessage = errorMessage;
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

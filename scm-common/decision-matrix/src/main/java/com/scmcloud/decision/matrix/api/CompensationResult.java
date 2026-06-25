package com.scmcloud.decision.matrix.api;

import java.util.List;
import java.util.Map;

/**
 * Result of a compensation operation.
 */
public class CompensationResult {

    private final String executionId;
    private final CompensationStatus status;
    private final List<CompensationAction> actions;
    private final long totalDurationMs;
    private final String errorMessage;

    public CompensationResult(String executionId, CompensationStatus status,
                               List<CompensationAction> actions, long totalDurationMs, String errorMessage) {
        this.executionId = executionId;
        this.status = status;
        this.actions = actions != null ? actions : List.of();
        this.totalDurationMs = totalDurationMs;
        this.errorMessage = errorMessage;
    }

    public String getExecutionId() {
        return executionId;
    }

    public CompensationStatus getStatus() {
        return status;
    }

    public List<CompensationAction> getActions() {
        return actions;
    }

    public long getTotalDurationMs() {
        return totalDurationMs;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public boolean isSuccess() {
        return status == CompensationStatus.SUCCESS;
    }

    public boolean isPartialSuccess() {
        return status == CompensationStatus.PARTIAL_SUCCESS;
    }

    public enum CompensationStatus {
        SUCCESS,
        PARTIAL_SUCCESS,
        FAILURE
    }
}

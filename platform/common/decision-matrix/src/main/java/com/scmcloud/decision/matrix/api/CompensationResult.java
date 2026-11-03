package com.scmcloud.decision.matrix.api;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

/**
 * Result of a compensation operation.
 */
@Getter
@ToString
@EqualsAndHashCode
public class CompensationResult {

    private final String executionId;
    private final CompensationStatus status;
    private final List<CompensationAction> actions;
    private final long totalDurationMs;
    private final String errorMessage;

    @Builder
    private CompensationResult(String executionId, CompensationStatus status,
                               List<CompensationAction> actions, long totalDurationMs, String errorMessage) {
        this.executionId = executionId;
        this.status = status;
        this.actions = actions != null ? List.copyOf(actions) : List.of();
        this.totalDurationMs = totalDurationMs;
        this.errorMessage = errorMessage;
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

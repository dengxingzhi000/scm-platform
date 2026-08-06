package com.scmcloud.decision.matrix.api;

import java.util.Map;

/**
 * Result of a single execution step.
 */
public record StepResult(String stepId, StepStatus status, Object result,
                         String errorMessage, Map<String, Object> metadata) {

    public StepResult {
        metadata = metadata != null ? metadata : Map.of();
    }

    public boolean isSuccess() {
        return status == StepStatus.SUCCESS;
    }

    public enum StepStatus {
        SUCCESS,
        FAILURE,
        TIMEOUT,
        SKIPPED
    }
}

package com.scmcloud.decision.matrix.api;

import java.util.Map;

/**
 * Result of a single execution step.
 */
public class StepResult {

    private final String stepId;
    private final StepStatus status;
    private final Object result;
    private final String errorMessage;
    private final Map<String, Object> metadata;

    public StepResult(String stepId, StepStatus status, Object result,
                      String errorMessage, Map<String, Object> metadata) {
        this.stepId = stepId;
        this.status = status;
        this.result = result;
        this.errorMessage = errorMessage;
        this.metadata = metadata != null ? metadata : Map.of();
    }

    public String getStepId() {
        return stepId;
    }

    public StepStatus getStatus() {
        return status;
    }

    public Object getResult() {
        return result;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
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

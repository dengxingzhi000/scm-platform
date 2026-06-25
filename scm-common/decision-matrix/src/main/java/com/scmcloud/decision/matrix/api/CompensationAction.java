package com.scmcloud.decision.matrix.api;

import java.util.Map;

/**
 * Compensation action to rollback a step.
 */
public class CompensationAction {

    private final String stepId;
    private final String actionType;
    private final Map<String, Object> parameters;
    private final CompensationStatus status;
    private final String errorMessage;

    public CompensationAction(String stepId, String actionType, Map<String, Object> parameters,
                               CompensationStatus status, String errorMessage) {
        this.stepId = stepId;
        this.actionType = actionType;
        this.parameters = parameters != null ? parameters : Map.of();
        this.status = status;
        this.errorMessage = errorMessage;
    }

    public String getStepId() {
        return stepId;
    }

    public String getActionType() {
        return actionType;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public CompensationStatus getStatus() {
        return status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public boolean isSuccess() {
        return status == CompensationStatus.SUCCESS;
    }

    public enum CompensationStatus {
        PENDING,
        SUCCESS,
        FAILURE
    }
}

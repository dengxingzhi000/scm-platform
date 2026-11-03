package com.scmcloud.decision.matrix.api;

import java.util.Map;

/**
 * Compensation action to rollback a step.
 */
public record CompensationAction(String stepId, String actionType, Map<String, Object> parameters,
                                 CompensationStatus status, String errorMessage) {

    public CompensationAction {
        parameters = parameters != null ? parameters : Map.of();
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

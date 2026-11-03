package com.scmcloud.common.integration.saga;

import lombok.Getter;

import java.time.OffsetDateTime;

/**
 * Result of a saga execution. Indicates success or failure with details.
 */
@Getter
public class SagaResult {

    public enum Status { SUCCESS, FAILED, COMPENSATION_FAILED }

    private final String sagaType;
    private final String sagaId;
    private final Status status;
    private final String failedStep;
    private final String errorMessage;
    private final OffsetDateTime completedAt;

    private SagaResult(String sagaType, String sagaId, Status status,
                       String failedStep, String errorMessage) {
        this.sagaType = sagaType;
        this.sagaId = sagaId;
        this.status = status;
        this.failedStep = failedStep;
        this.errorMessage = errorMessage;
        this.completedAt = OffsetDateTime.now();
    }

    public static SagaResult success(String sagaType, String sagaId) {
        return new SagaResult(sagaType, sagaId, Status.SUCCESS, null, null);
    }

    public static SagaResult failed(String sagaType, String sagaId,
                                    String failedStep, String errorMessage) {
        return new SagaResult(sagaType, sagaId, Status.FAILED, failedStep, errorMessage);
    }

    public boolean isSuccess() { return status == Status.SUCCESS; }
    public boolean isFailed() { return status == Status.FAILED; }

    @Override
    public String toString() {
        return "SagaResult{type=" + sagaType + ", id=" + sagaId +
                ", status=" + status +
                (failedStep != null ? ", failedStep=" + failedStep : "") +
                (errorMessage != null ? ", error=" + errorMessage : "") +
                '}';
    }
}

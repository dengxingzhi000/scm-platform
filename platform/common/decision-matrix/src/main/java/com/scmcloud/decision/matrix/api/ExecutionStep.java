package com.scmcloud.decision.matrix.api;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Map;

/**
 * Execution step in a saga transaction.
 */
@Getter
@ToString
@EqualsAndHashCode
public class ExecutionStep {

    private final String stepId;
    private final String stepName;
    private final StepExecutor executor;
    private final StepCompensator compensator;
    private final Map<String, Object> parameters;
    private final boolean critical;

    @Builder
    private ExecutionStep(String stepId, String stepName, StepExecutor executor,
                          StepCompensator compensator, Map<String, Object> parameters, boolean critical) {
        this.stepId = stepId;
        this.stepName = stepName;
        this.executor = executor;
        this.compensator = compensator;
        this.parameters = parameters != null ? Map.copyOf(parameters) : Map.of();
        this.critical = critical;
    }

    @FunctionalInterface
    public interface StepExecutor {
        StepResult execute(DecisionContext context, Map<String, Object> parameters);
    }

    @FunctionalInterface
    public interface StepCompensator {
        void compensate(DecisionContext context, StepResult result);
    }
}

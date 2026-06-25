package com.scmcloud.decision.matrix.api;

import java.util.List;
import java.util.Map;

/**
 * Execution step in a saga transaction.
 */
public class ExecutionStep {

    private final String stepId;
    private final String stepName;
    private final StepExecutor executor;
    private final StepCompensator compensator;
    private final Map<String, Object> parameters;
    private final boolean critical;

    public ExecutionStep(String stepId, String stepName, StepExecutor executor,
                         StepCompensator compensator, Map<String, Object> parameters, boolean critical) {
        this.stepId = stepId;
        this.stepName = stepName;
        this.executor = executor;
        this.compensator = compensator;
        this.parameters = parameters != null ? parameters : Map.of();
        this.critical = critical;
    }

    public String getStepId() {
        return stepId;
    }

    public String getStepName() {
        return stepName;
    }

    public StepExecutor getExecutor() {
        return executor;
    }

    public StepCompensator getCompensator() {
        return compensator;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public boolean isCritical() {
        return critical;
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

package com.scmcloud.decision.matrix.api;

import java.util.List;
import java.util.Map;

/**
 * Execution matrix interface.
 * <p>
 * Handles cross-system execution with saga pattern and compensation.
 */
public interface ExecutionMatrix {

    /**
     * Execute a saga transaction.
     *
     * @param steps list of execution steps
     * @param context execution context
     * @return execution result
     */
    ExecutionResult executeSaga(List<ExecutionStep> steps, DecisionContext context);

    /**
     * Execute with partial success support.
     *
     * @param steps list of execution steps
     * @param context execution context
     * @return partial execution result
     */
    PartialExecutionResult executePartial(List<ExecutionStep> steps, DecisionContext context);

    /**
     * Compensate (rollback) a failed execution.
     *
     * @param executionId execution to compensate
     * @return compensation result
     */
    CompensationResult compensate(String executionId);
}

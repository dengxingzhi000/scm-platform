package com.scmcloud.common.integration.saga;

/**
 * A single step in a saga. Each step has an execute action and a compensate action.
 *
 * <p>Execute: performs the forward action (e.g., deduct stock).
 * Compensate: undoes the forward action (e.g., release stock).
 *
 * <p>Steps are executed in order. If any step fails, all previously completed steps
 * are compensated in reverse order.</p>
 */
public interface SagaStep {

    /**
     * Unique step name for logging and tracking.
     */
    String getName();

    /**
     * Execute the forward action.
     *
     * @param context saga execution context (carries data between steps)
     * @throws Exception if the step fails
     */
    void execute(SagaContext context) throws Exception;

    /**
     * Compensate (undo) the forward action. Called when a later step fails.
     *
     * @param context saga execution context
     * @throws Exception if compensation fails (logged but does not halt compensation)
     */
    void compensate(SagaContext context) throws Exception;
}

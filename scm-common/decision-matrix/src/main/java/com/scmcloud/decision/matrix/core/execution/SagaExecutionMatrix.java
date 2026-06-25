package com.scmcloud.decision.matrix.core.execution;

import com.scmcloud.decision.matrix.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Saga-based execution matrix implementation.
 * <p>
 * Supports:
 * - Saga pattern with compensation
 * - Partial execution
 * - Multi-system consistency
 */
public class SagaExecutionMatrix implements ExecutionMatrix {

    private static final Logger log = LoggerFactory.getLogger(SagaExecutionMatrix.class);

    private final Map<String, ExecutionRecord> executionRecords = new HashMap<>();

    @Override
    public ExecutionResult executeSaga(List<ExecutionStep> steps, DecisionContext context) {
        String executionId = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();

        log.info("[ExecutionMatrix] Starting saga execution [{}] with {} steps", executionId, steps.size());

        Map<String, StepResult> stepResults = new LinkedHashMap<>();
        List<String> completedSteps = new ArrayList<>();
        List<String> failedSteps = new ArrayList<>();
        List<ExecutionStep> executedSteps = new ArrayList<>();

        for (ExecutionStep step : steps) {
            log.debug("[ExecutionMatrix] Executing step [{}]", step.getStepId());

            try {
                StepResult result = step.getExecutor().execute(context, step.getParameters());
                stepResults.put(step.getStepId(), result);

                if (result.isSuccess()) {
                    completedSteps.add(step.getStepId());
                    executedSteps.add(step);

                    // Enrich context with result
                    context = context.withAttribute(step.getStepId() + ".result", result.getResult());
                } else {
                    failedSteps.add(step.getStepId());

                    if (step.isCritical()) {
                        log.error("[ExecutionMatrix] Critical step [{}] failed, initiating compensation", step.getStepId());
                        break;
                    }
                }
            } catch (Exception e) {
                log.error("[ExecutionMatrix] Step [{}] failed with exception: {}", step.getStepId(), e.getMessage());
                stepResults.put(step.getStepId(), new StepResult(
                        step.getStepId(), StepResult.StepStatus.FAILURE, null, e.getMessage(), null
                ));
                failedSteps.add(step.getStepId());

                if (step.isCritical()) {
                    break;
                }
            }
        }

        long totalDuration = System.currentTimeMillis() - startTime;

        // Store execution record for compensation
        executionRecords.put(executionId, new ExecutionRecord(executionId, context, executedSteps, stepResults));

        ExecutionResult.ExecutionStatus status;
        String errorMessage = null;

        if (failedSteps.isEmpty()) {
            status = ExecutionResult.ExecutionStatus.SUCCESS;
        } else if (completedSteps.isEmpty()) {
            status = ExecutionResult.ExecutionStatus.FAILURE;
            errorMessage = "All steps failed";
        } else {
            status = ExecutionResult.ExecutionStatus.PARTIAL_SUCCESS;
            errorMessage = "Some steps failed: " + failedSteps;
        }

        log.info("[ExecutionMatrix] Saga [{}] completed: status={}, completed={}, failed={}, duration={}ms",
                executionId, status, completedSteps.size(), failedSteps.size(), totalDuration);

        return new ExecutionResult(
                executionId, status, stepResults,
                completedSteps, failedSteps, totalDuration, errorMessage
        );
    }

    @Override
    public PartialExecutionResult executePartial(List<ExecutionStep> steps, DecisionContext context) {
        String executionId = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();

        log.info("[ExecutionMatrix] Starting partial execution [{}] with {} steps", executionId, steps.size());

        Map<String, StepResult> stepResults = new LinkedHashMap<>();
        List<String> completedSteps = new ArrayList<>();
        List<String> failedSteps = new ArrayList<>();
        List<CompensationAction> compensations = new ArrayList<>();
        List<ExecutionStep> executedSteps = new ArrayList<>();

        for (ExecutionStep step : steps) {
            log.debug("[ExecutionMatrix] Executing step [{}]", step.getStepId());

            try {
                StepResult result = step.getExecutor().execute(context, step.getParameters());
                stepResults.put(step.getStepId(), result);

                if (result.isSuccess()) {
                    completedSteps.add(step.getStepId());
                    executedSteps.add(step);
                    context = context.withAttribute(step.getStepId() + ".result", result.getResult());
                } else {
                    failedSteps.add(step.getStepId());

                    // Record compensation needed
                    if (step.getCompensator() != null) {
                        compensations.add(new CompensationAction(
                                step.getStepId(), "compensate", step.getParameters(),
                                CompensationAction.CompensationStatus.PENDING, null
                        ));
                    }
                }
            } catch (Exception e) {
                log.error("[ExecutionMatrix] Step [{}] failed: {}", step.getStepId(), e.getMessage());
                stepResults.put(step.getStepId(), new StepResult(
                        step.getStepId(), StepResult.StepStatus.FAILURE, null, e.getMessage(), null
                ));
                failedSteps.add(step.getStepId());
            }
        }

        long totalDuration = System.currentTimeMillis() - startTime;

        // Store execution record
        executionRecords.put(executionId, new ExecutionRecord(executionId, context, executedSteps, stepResults));

        log.info("[ExecutionMatrix] Partial execution [{}] completed: completed={}, failed={}",
                executionId, completedSteps.size(), failedSteps.size());

        return new PartialExecutionResult(
                executionId, stepResults, completedSteps, failedSteps,
                compensations, totalDuration
        );
    }

    @Override
    public CompensationResult compensate(String executionId) {
        ExecutionRecord record = executionRecords.get(executionId);
        if (record == null) {
            return new CompensationResult(
                    executionId, CompensationResult.CompensationStatus.FAILURE,
                    List.of(), 0, "Execution record not found"
            );
        }

        long startTime = System.currentTimeMillis();
        log.info("[ExecutionMatrix] Starting compensation for execution [{}]", executionId);

        List<CompensationAction> actions = new ArrayList<>();

        // Compensate in reverse order
        List<ExecutionStep> reversedSteps = new ArrayList<>(record.executedSteps());
        Collections.reverse(reversedSteps);

        for (ExecutionStep step : reversedSteps) {
            if (step.getCompensator() == null) {
                continue;
            }

            try {
                StepResult stepResult = record.stepResults().get(step.getStepId());
                step.getCompensator().compensate(record.context(), stepResult);

                actions.add(new CompensationAction(
                        step.getStepId(), "compensated", step.getParameters(),
                        CompensationAction.CompensationStatus.SUCCESS, null
                ));

                log.debug("[ExecutionMatrix] Compensated step [{}]", step.getStepId());
            } catch (Exception e) {
                log.error("[ExecutionMatrix] Failed to compensate step [{}]: {}", step.getStepId(), e.getMessage());
                actions.add(new CompensationAction(
                        step.getStepId(), "compensate_failed", step.getParameters(),
                        CompensationAction.CompensationStatus.FAILURE, e.getMessage()
                ));
            }
        }

        long totalDuration = System.currentTimeMillis() - startTime;

        CompensationResult.CompensationStatus status;
        if (actions.stream().allMatch(CompensationAction::isSuccess)) {
            status = CompensationResult.CompensationStatus.SUCCESS;
        } else if (actions.stream().anyMatch(CompensationAction::isSuccess)) {
            status = CompensationResult.CompensationStatus.PARTIAL_SUCCESS;
        } else {
            status = CompensationResult.CompensationStatus.FAILURE;
        }

        log.info("[ExecutionMatrix] Compensation [{}] completed: status={}, actions={}",
                executionId, status, actions.size());

        return new CompensationResult(executionId, status, actions, totalDuration, null);
    }

    private record ExecutionRecord(
            String executionId,
            DecisionContext context,
            List<ExecutionStep> executedSteps,
            Map<String, StepResult> stepResults
    ) {}
}

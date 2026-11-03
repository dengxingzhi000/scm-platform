package com.scmcloud.common.integration.saga;

import com.scmcloud.common.integration.outbox.OutboxService;
import com.scmcloud.common.domain.event.DomainEventPublisher;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Saga orchestrator. Executes a sequence of steps, compensating in reverse on failure.
 *
 * <p>Usage:</p>
 * <pre>
 * Saga saga = new Saga("order-inventory", outboxService);
 * saga.addStep(deductStockStep);
 * saga.addStep(createReservationStep);
 * SagaResult result = saga.execute(context);
 * </pre>
 *
 * <p>Each step's execute() runs in its own transaction (via the calling service's @Transactional).
 * The orchestrator does NOT manage transactions — it coordinates the sequence and compensation.</p>
 */
@Slf4j
public class Saga {

    private final String sagaType;
    private final List<SagaStep> steps = new ArrayList<>();

    public Saga(String sagaType) {
        this.sagaType = sagaType;
    }

    public Saga addStep(SagaStep step) {
        steps.add(step);
        return this;
    }

    /**
     * Execute all steps in order. Compensate in reverse on failure.
     *
     * @return result indicating success or failure with details
     */
    public SagaResult execute(SagaContext context) {
        List<String> completedSteps = new ArrayList<>();

        log.info("[Saga:{}] Starting saga execution, sagaId={}, steps={}",
                sagaType, context.getSagaId(), steps.size());

        for (int i = 0; i < steps.size(); i++) {
            SagaStep step = steps.get(i);
            try {
                log.info("[Saga:{}] Executing step {}/{}: {}", sagaType, i + 1, steps.size(), step.getName());
                step.execute(context);
                completedSteps.add(step.getName());
                log.info("[Saga:{}] Step completed: {}", sagaType, step.getName());
            } catch (Exception e) {
                log.error("[Saga:{}] Step failed: {} — {}", sagaType, step.getName(), e.getMessage());
                compensate(completedSteps, context);
                return SagaResult.failed(sagaType, context.getSagaId(), step.getName(), e.getMessage());
            }
        }

        log.info("[Saga:{}] Saga completed successfully, sagaId={}", sagaType, context.getSagaId());
        return SagaResult.success(sagaType, context.getSagaId());
    }

    private void compensate(List<String> completedSteps, SagaContext context) {
        List<String> reversed = new ArrayList<>(completedSteps);
        Collections.reverse(reversed);

        log.info("[Saga:{}] Starting compensation for {} completed steps", sagaType, reversed.size());

        for (int i = 0; i < reversed.size(); i++) {
            String stepName = reversed.get(i);
            SagaStep step = steps.stream()
                    .filter(s -> s.getName().equals(stepName))
                    .findFirst()
                    .orElseThrow();

            try {
                log.info("[Saga:{}] Compensating step {}/{}: {}", sagaType, i + 1, reversed.size(), step.getName());
                step.compensate(context);
                log.info("[Saga:{}] Compensation completed: {}", sagaType, step.getName());
            } catch (Exception e) {
                log.error("[Saga:{}] Compensation FAILED for step {}: {} — MANUAL INTERVENTION REQUIRED",
                        sagaType, step.getName(), e.getMessage());
                // Compensation failure requires manual intervention.
                // The saga state should be persisted for later retry.
            }
        }
    }
}

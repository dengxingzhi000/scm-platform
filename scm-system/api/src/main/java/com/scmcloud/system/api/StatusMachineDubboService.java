package com.scmcloud.system.api;

import java.io.Serializable;
import java.util.List;

/**
 * Dubbo API for state machine engine.
 * Used by order, purchase, warehouse, logistics services
 * to validate and execute state transitions remotely.
 */
public interface StatusMachineDubboService {

    // ─── Validation ────────────────────────────────────

    /**
     * Check if a transition from fromStatus to toStatus is allowed.
     */
    TransitionCheckDTO canTransition(String bizType, String fromStatus, String toStatus);

    /**
     * Check if a transition by actionCode is allowed from current status.
     */
    TransitionCheckDTO canTransitionByAction(String bizType, String fromStatus, String actionCode);

    // ─── Execution ─────────────────────────────────────

    /**
     * Execute a transition by action code. Returns target status if valid.
     */
    TransitionResultDTO transition(String bizType, String fromStatus, String actionCode);

    // ─── Query ─────────────────────────────────────────

    /**
     * Get all available actions from current status.
     */
    List<AvailableActionDTO> getAvailableActions(String bizType, String currentStatus);

    /**
     * Get all valid next statuses from current status.
     */
    List<String> getValidNextStatuses(String bizType, String currentStatus);

    // ─── DTOs ─────────────────────────────────────────

    record TransitionCheckDTO(
            boolean allowed,
            String bizType,
            String fromStatus,
            String toStatus,
            String reason
    ) implements Serializable {}

    record TransitionResultDTO(
            boolean success,
            String bizType,
            String fromStatus,
            String toStatus,
            String actionCode,
            boolean needApproval,
            String postAction,
            String errorMessage
    ) implements Serializable {}

    record AvailableActionDTO(
            String actionCode,
            String actionName,
            String actionNameEn,
            String targetStatus,
            boolean needApproval,
            String conditionExpression
    ) implements Serializable {}
}

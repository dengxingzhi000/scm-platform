package com.scmcloud.system.statemachine;

import java.util.List;

/**
 * Unified state machine engine. Validates transitions against sys_status_transition rules.
 *
 * <p>Replaces scattered if-else/switch status logic across all business modules.
 * Business services call this engine instead of hardcoding transition rules.</p>
 *
 * <p>Usage:</p>
 * <pre>
 * // Check if transition is valid (by target status)
 * if (engine.canTransition("ORDER", "PAID", "SHIPPED").isAllowed()) { ... }
 *
 * // Check if transition is valid (by action code)
 * if (engine.canTransitionByAction("ORDER", "PAID", "SHIP").isAllowed()) { ... }
 *
 * // Execute transition (validates + returns result)
 * TransitionResult result = engine.transition("ORDER", order.getStatus(), "SHIP");
 *
 * // Get all available actions from current status
 * List&lt;AvailableAction&gt; actions = engine.getAvailableActions("ORDER", "PAID");
 * </pre>
 */
public interface StateMachineEngine {

    /**
     * Check if a transition from fromStatus to toStatus is allowed.
     */
    TransitionCheckResult canTransition(String bizType, String fromStatus, String toStatus);

    /**
     * Check if a transition by actionCode is allowed from current status.
     */
    TransitionCheckResult canTransitionByAction(String bizType, String fromStatus, String actionCode);

    /**
     * Execute a transition by action code. Validates the transition is legal.
     *
     * @param bizType    business type (ORDER, PURCHASE, etc.)
     * @param fromStatus current status code
     * @param actionCode action code (PAY, CANCEL, SHIP, etc.)
     * @return result with the target status if valid
     */
    TransitionResult transition(String bizType, String fromStatus, String actionCode);

    /**
     * Get all available actions from the current status.
     * Returns action code, action name, target status, and whether approval is needed.
     */
    List<AvailableAction> getAvailableActions(String bizType, String currentStatus);

    /**
     * Get all valid next statuses from the current status.
     */
    List<String> getValidNextStatuses(String bizType, String currentStatus);
}

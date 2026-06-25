package com.scmcloud.decision.matrix.api;

import java.util.List;
import java.util.Map;

/**
 * Decision node interface.
 * <p>
 * Each decision node represents a single decision point in the decision chain.
 * Nodes are pluggable and can be composed into complex decision workflows.
 */
public interface DecisionNode {

    /**
     * Unique node identifier.
     */
    String nodeId();

    /**
     * Human-readable node name.
     */
    String nodeName();

    /**
     * Execute the decision.
     *
     * @param context decision context
     * @return decision result
     */
    DecisionResult execute(DecisionContext context);

    /**
     * Check if this node supports rollback.
     */
    boolean canRollback();

    /**
     * Rollback the decision.
     *
     * @param context decision context
     * @param result  previous result to rollback
     */
    void rollback(DecisionContext context, DecisionResult result);

    /**
     * Execution priority (lower = higher priority).
     */
    int priority();

    /**
     * List of node IDs that this node depends on.
     */
    List<String> dependencies();

    /**
     * Timeout in milliseconds.
     */
    default long timeoutMs() {
        return 5000;
    }

    /**
     * Whether this node is critical (failure stops the chain).
     */
    default boolean isCritical() {
        return true;
    }
}

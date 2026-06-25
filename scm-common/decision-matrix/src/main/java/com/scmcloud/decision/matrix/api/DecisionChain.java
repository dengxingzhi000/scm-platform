package com.scmcloud.decision.matrix.api;

import java.util.List;

/**
 * Decision chain orchestrator interface.
 * <p>
 * Orchestrates multiple decision nodes into a workflow.
 */
public interface DecisionChain {

    /**
     * Chain identifier.
     */
    String chainId();

    /**
     * Chain name.
     */
    String chainName();

    /**
     * Get ordered list of decision nodes.
     */
    List<DecisionNode> getNodes();

    /**
     * Execute the entire chain.
     *
     * @param context decision context
     * @return chain execution result
     */
    ChainExecutionResult execute(DecisionContext context);

    /**
     * Add a decision node to the chain.
     */
    DecisionChain addNode(DecisionNode node);

    /**
     * Add a fallback node for a specific node.
     */
    DecisionChain addFallback(String nodeId, DecisionNode fallbackNode);
}

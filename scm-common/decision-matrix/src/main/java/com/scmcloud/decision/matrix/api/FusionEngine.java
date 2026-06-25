package com.scmcloud.decision.matrix.api;

import java.util.List;
import java.util.Map;

/**
 * Fusion engine interface.
 * <p>
 * Combines outputs from multiple decision engines into a single result.
 */
public interface FusionEngine {

    /**
     * Fuse multiple decision results into a single result.
     *
     * @param results list of decision results to fuse
     * @param weights weight profile for each dimension
     * @return fused decision result
     */
    DecisionResult fuse(List<DecisionResult> results, Map<String, Double> weights);

    /**
     * Fuse with conflict resolution.
     *
     * @param results          list of decision results to fuse
     * @param weights          weight profile for each dimension
     * @param conflictStrategy conflict resolution strategy
     * @return fused decision result
     */
    DecisionResult fuseWithConflictResolution(List<DecisionResult> results,
                                               Map<String, Double> weights,
                                               ConflictStrategy conflictStrategy);

    /**
     * Conflict resolution strategies.
     */
    enum ConflictStrategy {
        WEIGHTED_AVERAGE,
        PARETO_OPTIMAL,
        MAX_UTILITY,
        PRIORITY_BASED
    }
}

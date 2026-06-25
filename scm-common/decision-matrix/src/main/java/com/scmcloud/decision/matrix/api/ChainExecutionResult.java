package com.scmcloud.decision.matrix.api;

import java.util.List;
import java.util.Map;

/**
 * Result of executing a decision chain.
 */
public class ChainExecutionResult {

    private final String chainId;
    private final String executionId;
    private final ChainStatus status;
    private final Map<String, DecisionResult> nodeResults;
    private final List<String> completedNodes;
    private final List<String> failedNodes;
    private final List<String> skippedNodes;
    private final long totalDurationMs;
    private final DecisionExplanation overallExplanation;

    public ChainExecutionResult(String chainId, String executionId, ChainStatus status,
                                 Map<String, DecisionResult> nodeResults,
                                 List<String> completedNodes, List<String> failedNodes,
                                 List<String> skippedNodes, long totalDurationMs,
                                 DecisionExplanation overallExplanation) {
        this.chainId = chainId;
        this.executionId = executionId;
        this.status = status;
        this.nodeResults = nodeResults != null ? nodeResults : Map.of();
        this.completedNodes = completedNodes != null ? completedNodes : List.of();
        this.failedNodes = failedNodes != null ? failedNodes : List.of();
        this.skippedNodes = skippedNodes != null ? skippedNodes : List.of();
        this.totalDurationMs = totalDurationMs;
        this.overallExplanation = overallExplanation;
    }

    public String getChainId() {
        return chainId;
    }

    public String getExecutionId() {
        return executionId;
    }

    public ChainStatus getStatus() {
        return status;
    }

    public Map<String, DecisionResult> getNodeResults() {
        return nodeResults;
    }

    public List<String> getCompletedNodes() {
        return completedNodes;
    }

    public List<String> getFailedNodes() {
        return failedNodes;
    }

    public List<String> getSkippedNodes() {
        return skippedNodes;
    }

    public long getTotalDurationMs() {
        return totalDurationMs;
    }

    public DecisionExplanation getOverallExplanation() {
        return overallExplanation;
    }

    public boolean isSuccess() {
        return status == ChainStatus.SUCCESS;
    }

    public boolean isPartialSuccess() {
        return status == ChainStatus.PARTIAL_SUCCESS;
    }

    public enum ChainStatus {
        SUCCESS,
        PARTIAL_SUCCESS,
        FAILURE,
        TIMEOUT,
        CANCELLED
    }
}

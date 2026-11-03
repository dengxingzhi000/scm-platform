package com.scmcloud.decision.matrix.api;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Result of executing a decision chain.
 */
@Getter
@ToString
@EqualsAndHashCode
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

    @Builder
    private ChainExecutionResult(String chainId, String executionId, ChainStatus status,
                                 Map<String, DecisionResult> nodeResults,
                                 List<String> completedNodes, List<String> failedNodes,
                                 List<String> skippedNodes, long totalDurationMs,
                                 DecisionExplanation overallExplanation) {
        this.chainId = Objects.requireNonNull(chainId, "chainId");
        this.executionId = Objects.requireNonNull(executionId, "executionId");
        this.status = Objects.requireNonNull(status, "status");
        this.nodeResults = nodeResults != null ? Map.copyOf(nodeResults) : Map.of();
        this.completedNodes = completedNodes != null ? List.copyOf(completedNodes) : List.of();
        this.failedNodes = failedNodes != null ? List.copyOf(failedNodes) : List.of();
        this.skippedNodes = skippedNodes != null ? List.copyOf(skippedNodes) : List.of();
        this.totalDurationMs = totalDurationMs;
        this.overallExplanation = overallExplanation;
    }

    public DecisionResult getNodeResult(String nodeId) {
        return nodeResults.get(nodeId);
    }

    public boolean isSuccess() {
        return status == ChainStatus.SUCCESS;
    }

    public boolean isPartialSuccess() {
        return status == ChainStatus.PARTIAL_SUCCESS;
    }

    public boolean isFailure() {
        return status == ChainStatus.FAILURE;
    }

    public boolean isTimeout() {
        return status == ChainStatus.TIMEOUT;
    }

    public boolean isCancelled() {
        return status == ChainStatus.CANCELLED;
    }

    public boolean hasFailures() {
        return !failedNodes.isEmpty();
    }

    public boolean hasSkippedNodes() {
        return !skippedNodes.isEmpty();
    }

    public int getNodeCount() {
        return nodeResults.size();
    }

    public int getTotalNodeCount() {
        return completedNodes.size() + failedNodes.size() + skippedNodes.size();
    }

    public double getSuccessRate() {
        int total = getTotalNodeCount();
        return total == 0 ? 0.0 : (double) completedNodes.size() / total;
    }

    public enum ChainStatus {
        SUCCESS,
        PARTIAL_SUCCESS,
        FAILURE,
        TIMEOUT,
        CANCELLED
    }
}

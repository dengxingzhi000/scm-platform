package com.scmcloud.decision.matrix.core.chain;

import com.scmcloud.decision.matrix.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;

/**
 * Default implementation of DecisionChain.
 * <p>
 * Orchestrates multiple decision nodes with support for:
 * - Dependency-based ordering
 * - Parallel execution where possible
 * - Fallback nodes
 * - Timeout handling
 */
public class DefaultDecisionChain implements DecisionChain {

    private static final Logger log = LoggerFactory.getLogger(DefaultDecisionChain.class);

    private final String chainId;
    private final String chainName;
    private final List<DecisionNode> nodes;
    private final Map<String, DecisionNode> fallbackNodes;
    private final ExecutorService executorService;

    public DefaultDecisionChain(String chainId, String chainName) {
        this(chainId, chainName, Executors.newCachedThreadPool());
    }

    public DefaultDecisionChain(String chainId, String chainName, ExecutorService executorService) {
        this.chainId = chainId;
        this.chainName = chainName;
        this.nodes = new ArrayList<>();
        this.fallbackNodes = new HashMap<>();
        this.executorService = executorService;
    }

    @Override
    public String chainId() {
        return chainId;
    }

    @Override
    public String chainName() {
        return chainName;
    }

    @Override
    public List<DecisionNode> getNodes() {
        return Collections.unmodifiableList(nodes);
    }

    @Override
    public DecisionChain addNode(DecisionNode node) {
        nodes.add(node);
        return this;
    }

    @Override
    public DecisionChain addFallback(String nodeId, DecisionNode fallbackNode) {
        fallbackNodes.put(nodeId, fallbackNode);
        return this;
    }

    @Override
    public ChainExecutionResult execute(DecisionContext context) {
        String executionId = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();

        log.info("[DecisionChain] Starting chain [{}] execution [{}]", chainId, executionId);

        Map<String, DecisionResult> nodeResults = new LinkedHashMap<>();
        List<String> completedNodes = new ArrayList<>();
        List<String> failedNodes = new ArrayList<>();
        List<String> skippedNodes = new ArrayList<>();

        // Sort nodes by priority and dependencies
        List<DecisionNode> sortedNodes = topologicalSort(nodes);

        for (DecisionNode node : sortedNodes) {
            // Check if dependencies are satisfied
            if (!areDependenciesSatisfied(node, completedNodes)) {
                log.warn("[DecisionChain] Skipping node [{}] - dependencies not satisfied", node.nodeId());
                skippedNodes.add(node.nodeId());
                continue;
            }

            // Execute node with timeout
            DecisionResult result = executeNode(node, context);

            if (result.isSuccess()) {
                completedNodes.add(node.nodeId());
                nodeResults.put(node.nodeId(), result);

                // Enrich context with result
                context = context.withAttribute(node.nodeId() + ".result", result.getValue());
            } else {
                // Try fallback if available
                DecisionNode fallback = fallbackNodes.get(node.nodeId());
                if (fallback != null) {
                    log.info("[DecisionChain] Executing fallback for node [{}]", node.nodeId());
                    DecisionResult fallbackResult = executeNode(fallback, context);

                    if (fallbackResult.isSuccess()) {
                        completedNodes.add(node.nodeId());
                        nodeResults.put(node.nodeId(), fallbackResult);
                        context = context.withAttribute(node.nodeId() + ".result", fallbackResult.getValue());
                    } else if (node.isCritical()) {
                        failedNodes.add(node.nodeId());
                        nodeResults.put(node.nodeId(), fallbackResult);
                        log.error("[DecisionChain] Critical node [{}] failed, stopping chain", node.nodeId());
                        break;
                    } else {
                        failedNodes.add(node.nodeId());
                        nodeResults.put(node.nodeId(), fallbackResult);
                    }
                } else if (node.isCritical()) {
                    failedNodes.add(node.nodeId());
                    nodeResults.put(node.nodeId(), result);
                    log.error("[DecisionChain] Critical node [{}] failed, stopping chain", node.nodeId());
                    break;
                } else {
                    failedNodes.add(node.nodeId());
                    nodeResults.put(node.nodeId(), result);
                }
            }
        }

        long totalDuration = System.currentTimeMillis() - startTime;

        ChainExecutionResult.ChainStatus status;
        if (failedNodes.isEmpty() && skippedNodes.isEmpty()) {
            status = ChainExecutionResult.ChainStatus.SUCCESS;
        } else if (completedNodes.isEmpty()) {
            status = ChainExecutionResult.ChainStatus.FAILURE;
        } else {
            status = ChainExecutionResult.ChainStatus.PARTIAL_SUCCESS;
        }

        DecisionExplanation overallExplanation = generateOverallExplanation(nodeResults);

        log.info("[DecisionChain] Chain [{}] completed: status={}, completed={}, failed={}, skipped={}, duration={}ms",
                chainId, status, completedNodes.size(), failedNodes.size(), skippedNodes.size(), totalDuration);

        return new ChainExecutionResult(
                chainId, executionId, status,
                nodeResults, completedNodes, failedNodes, skippedNodes,
                totalDuration, overallExplanation
        );
    }

    private DecisionResult executeNode(DecisionNode node, DecisionContext context) {
        try {
            Future<DecisionResult> future = executorService.submit(() -> node.execute(context));
            return future.get(node.timeoutMs(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            log.error("[DecisionChain] Node [{}] timed out after {}ms", node.nodeId(), node.timeoutMs());
            return new DecisionResult(
                    node.nodeId(), DecisionResult.DecisionStatus.TIMEOUT, null,
                    0.0, 0.0, null, null, List.of("Timeout after " + node.timeoutMs() + "ms")
            );
        } catch (Exception e) {
            log.error("[DecisionChain] Node [{}] failed with exception: {}", node.nodeId(), e.getMessage());
            return new DecisionResult(
                    node.nodeId(), DecisionResult.DecisionStatus.FAILURE, null,
                    0.0, 0.0, null, null, List.of(e.getMessage())
            );
        }
    }

    private boolean areDependenciesSatisfied(DecisionNode node, List<String> completedNodes) {
        return completedNodes.containsAll(node.dependencies());
    }

    private List<DecisionNode> topologicalSort(List<DecisionNode> nodes) {
        // Simple priority-based sort (can be enhanced with proper topological sort)
        List<DecisionNode> sorted = new ArrayList<>(nodes);
        sorted.sort(Comparator.comparingInt(DecisionNode::priority));
        return sorted;
    }

    private DecisionExplanation generateOverallExplanation(Map<String, DecisionResult> nodeResults) {
        Map<String, Double> factorWeights = new HashMap<>();
        Map<String, Double> factorScores = new HashMap<>();
        List<String> contributingFactors = new ArrayList<>();

        for (Map.Entry<String, DecisionResult> entry : nodeResults.entrySet()) {
            DecisionResult result = entry.getValue();
            if (result.getExplanation() != null) {
                factorWeights.putAll(result.getExplanation().getFactorWeights());
                factorScores.putAll(result.getExplanation().getFactorScores());
                contributingFactors.addAll(result.getExplanation().getContributingFactors());
            }
        }

        double avgScore = nodeResults.values().stream()
                .mapToDouble(DecisionResult::getScore)
                .average()
                .orElse(0.0);

        return new DecisionExplanation(
                chainId,
                "Chain executed " + nodeResults.size() + " nodes",
                factorWeights, factorScores, contributingFactors, Map.of("averageScore", avgScore)
        );
    }
}

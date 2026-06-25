package com.scmcloud.decision.matrix.core.gateway;

import com.scmcloud.decision.matrix.api.*;
import com.scmcloud.decision.matrix.core.chain.DefaultDecisionChain;
import com.scmcloud.decision.matrix.core.execution.SagaExecutionMatrix;
import com.scmcloud.decision.matrix.core.fusion.WeightedFusionEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Decision gateway - central orchestration layer.
 * <p>
 * Coordinates multiple decision chains and manages the overall decision workflow.
 */
public class DecisionGateway {

    private static final Logger log = LoggerFactory.getLogger(DecisionGateway.class);

    private final Map<String, DecisionChain> chains;
    private final FusionEngine fusionEngine;
    private final ExecutionMatrix executionMatrix;
    private final Map<String, GatewayRoute> routes;

    public DecisionGateway() {
        this.chains = new ConcurrentHashMap<>();
        this.fusionEngine = new WeightedFusionEngine();
        this.executionMatrix = new SagaExecutionMatrix();
        this.routes = new ConcurrentHashMap<>();
    }

    /**
     * Register a decision chain.
     */
    public void registerChain(DecisionChain chain) {
        chains.put(chain.chainId(), chain);
        log.info("[Gateway] Registered chain [{}]", chain.chainId());
    }

    /**
     * Register a gateway route.
     */
    public void registerRoute(GatewayRoute route) {
        routes.put(route.routeId(), route);
        log.info("[Gateway] Registered route [{}]", route.routeId());
    }

    /**
     * Execute a decision by route.
     */
    public GatewayResult execute(String routeId, DecisionContext context) {
        GatewayRoute route = routes.get(routeId);
        if (route == null) {
            log.error("[Gateway] Route [{}] not found", routeId);
            return GatewayResult.failure(routeId, "Route not found");
        }

        log.info("[Gateway] Executing route [{}] with {} chains", routeId, route.chainIds().size());

        long startTime = System.currentTimeMillis();
        Map<String, ChainExecutionResult> chainResults = new LinkedHashMap<>();
        List<DecisionResult> fusionInputs = new ArrayList<>();

        // Execute all chains in the route
        for (String chainId : route.chainIds()) {
            DecisionChain chain = chains.get(chainId);
            if (chain == null) {
                log.warn("[Gateway] Chain [{}] not found, skipping", chainId);
                continue;
            }

            ChainExecutionResult chainResult = chain.execute(context);
            chainResults.put(chainId, chainResult);

            // Collect results for fusion
            for (DecisionResult result : chainResult.getNodeResults().values()) {
                if (result.isSuccess()) {
                    fusionInputs.add(result);
                }
            }
        }

        // Fuse results
        DecisionResult fusedResult = fusionEngine.fuse(fusionInputs, route.weights());

        // Execute if configured
        ExecutionResult executionResult = null;
        if (route.autoExecute() && fusedResult.isSuccess()) {
            executionResult = executionMatrix.executeSaga(route.executionSteps(), context);
        }

        long totalDuration = System.currentTimeMillis() - startTime;

        log.info("[Gateway] Route [{}] completed in {}ms", routeId, totalDuration);

        return new GatewayResult(
                routeId, chainResults, fusedResult, executionResult,
                totalDuration, fusedResult.getExplanation()
        );
    }

    /**
     * Get registered chain by ID.
     */
    public DecisionChain getChain(String chainId) {
        return chains.get(chainId);
    }

    /**
     * Get all registered chain IDs.
     */
    public Set<String> getChainIds() {
        return chains.keySet();
    }

    /**
     * Get all registered route IDs.
     */
    public Set<String> getRouteIds() {
        return routes.keySet();
    }

    /**
     * Gateway route definition.
     */
    public record GatewayRoute(
            String routeId,
            List<String> chainIds,
            Map<String, Double> weights,
            boolean autoExecute,
            List<ExecutionStep> executionSteps
    ) {}
}

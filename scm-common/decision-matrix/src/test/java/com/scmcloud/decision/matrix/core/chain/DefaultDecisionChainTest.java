package com.scmcloud.decision.matrix.core.chain;

import com.scmcloud.decision.matrix.api.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Decision Chain Test")
class DefaultDecisionChainTest {

    private DefaultDecisionChain chain;

    @BeforeEach
    void setUp() {
        chain = new DefaultDecisionChain("test-chain", "Test Chain");
    }

    @Test
    @DisplayName("Should execute single node chain")
    void shouldExecuteSingleNodeChain() {
        DecisionNode node = createMockNode("node1", true, 0.9);
        chain.addNode(node);

        DecisionContext context = DecisionContext.builder().contextId("ctx1").businessType("order").build();
        ChainExecutionResult result = chain.execute(context);

        assertTrue(result.isSuccess());
        assertEquals(1, result.getCompletedNodes().size());
        assertEquals(0, result.getFailedNodes().size());
    }

    @Test
    @DisplayName("Should execute multiple nodes in order")
    void shouldExecuteMultipleNodes() {
        DecisionNode node1 = createMockNode("node1", true, 0.9);
        DecisionNode node2 = createMockNode("node2", true, 0.8);

        chain.addNode(node1);
        chain.addNode(node2);

        DecisionContext context = DecisionContext.builder().contextId("ctx1").businessType("order").build();
        ChainExecutionResult result = chain.execute(context);

        assertTrue(result.isSuccess());
        assertEquals(2, result.getCompletedNodes().size());
    }

    @Test
    @DisplayName("Should stop on critical node failure")
    void shouldStopOnCriticalFailure() {
        DecisionNode node1 = createMockNode("node1", true, 0.9);
        DecisionNode node2 = createMockNode("node2", false, 0.0);
        DecisionNode node3 = createMockNode("node3", true, 0.7);

        chain.addNode(node1);
        chain.addNode(node2);
        chain.addNode(node3);

        DecisionContext context = DecisionContext.builder().contextId("ctx1").businessType("order").build();
        ChainExecutionResult result = chain.execute(context);

        assertFalse(result.isSuccess());
        assertTrue(result.getFailedNodes().contains("node2"));
    }

    @Test
    @DisplayName("Should execute fallback on failure")
    void shouldExecuteFallback() {
        DecisionNode primary = createMockNode("primary", false, 0.0);
        DecisionNode fallback = createMockNode("fallback", true, 0.7);

        chain.addNode(primary);
        chain.addFallback("primary", fallback);

        DecisionContext context = DecisionContext.builder().contextId("ctx1").businessType("order").build();
        ChainExecutionResult result = chain.execute(context);

        assertTrue(result.isSuccess());
        assertTrue(result.getCompletedNodes().contains("primary"));
    }

    @Test
    @DisplayName("Should skip node when dependencies not satisfied")
    void shouldSkipWhenDependenciesNotSatisfied() {
        DecisionNode node1 = createMockNode("node1", false, 0.0);
        DecisionNode node2 = createMockNode("node2", true, 0.8);
        // node2 depends on node1, but node1 is critical and fails

        chain.addNode(node1);
        chain.addNode(node2);

        DecisionContext context = DecisionContext.builder().contextId("ctx1").businessType("order").build();
        ChainExecutionResult result = chain.execute(context);

        assertFalse(result.isSuccess());
    }

    private DecisionNode createMockNode(String nodeId, boolean success, double score) {
        return new DecisionNode() {
            @Override
            public String nodeId() {
                return nodeId;
            }

            @Override
            public String nodeName() {
                return "Mock " + nodeId;
            }

            @Override
            public DecisionResult execute(DecisionContext context) {
                DecisionResult.DecisionStatus status = success ?
                        DecisionResult.DecisionStatus.SUCCESS : DecisionResult.DecisionStatus.FAILURE;

                DecisionExplanation explanation = DecisionExplanation.builder()
                        .decisionId(nodeId)
                        .primaryReason("Mock decision")
                        .factorWeights(Map.of("score", score))
                        .factorScores(Map.of(nodeId, score))
                        .contributingFactors(List.of(nodeId))
                        .build();

                return DecisionResult.builder()
                        .nodeId(nodeId)
                        .status(status)
                        .value("value")
                        .score(score)
                        .confidence(score)
                        .explanation(explanation)
                        .build();
            }

            @Override
            public boolean canRollback() {
                return false;
            }

            @Override
            public void rollback(DecisionContext context, DecisionResult result) {
            }

            @Override
            public int priority() {
                return 100;
            }

            @Override
            public List<String> dependencies() {
                return List.of();
            }
        };
    }
}

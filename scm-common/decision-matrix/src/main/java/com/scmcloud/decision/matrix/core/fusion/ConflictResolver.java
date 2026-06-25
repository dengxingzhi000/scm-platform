package com.scmcloud.decision.matrix.core.fusion;

import com.scmcloud.decision.matrix.api.DecisionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Conflict resolver for handling conflicting decision results.
 */
public class ConflictResolver {

    private static final Logger log = LoggerFactory.getLogger(ConflictResolver.class);

    /**
     * Detect conflicts between decision results.
     */
    public List<Conflict> detectConflicts(List<DecisionResult> results) {
        List<Conflict> conflicts = new ArrayList<>();

        for (int i = 0; i < results.size(); i++) {
            for (int j = i + 1; j < results.size(); j++) {
                DecisionResult r1 = results.get(i);
                DecisionResult r2 = results.get(j);

                if (isConflicting(r1, r2)) {
                    conflicts.add(new Conflict(
                            r1.getNodeId(), r2.getNodeId(),
                            ConflictType.SCORE_DIFFERENCE,
                            Math.abs(r1.getScore() - r2.getScore()),
                            "Score difference: " + Math.abs(r1.getScore() - r2.getScore())
                    ));
                }
            }
        }

        return conflicts;
    }

    /**
     * Resolve conflicts using specified strategy.
     */
    public DecisionResult resolve(List<DecisionResult> results, List<Conflict> conflicts,
                                   ResolutionStrategy strategy) {
        log.info("[ConflictResolver] Resolving {} conflicts with strategy [{}]", conflicts.size(), strategy);

        return switch (strategy) {
            case HIGHEST_SCORE -> resolveHighestScore(results);
            case HIGHEST_CONFIDENCE -> resolveHighestConfidence(results);
            case MOST_CONSERVATIVE -> resolveMostConservative(results);
            case BALANCED -> resolveBalanced(results);
        };
    }

    private boolean isConflicting(DecisionResult r1, DecisionResult r2) {
        // Consider results conflicting if score difference > 0.3
        return Math.abs(r1.getScore() - r2.getScore()) > 0.3;
    }

    private DecisionResult resolveHighestScore(List<DecisionResult> results) {
        return results.stream()
                .max(Comparator.comparingDouble(DecisionResult::getScore))
                .orElse(results.getFirst());
    }

    private DecisionResult resolveHighestConfidence(List<DecisionResult> results) {
        return results.stream()
                .max(Comparator.comparingDouble(DecisionResult::getConfidence))
                .orElse(results.getFirst());
    }

    private DecisionResult resolveMostConservative(List<DecisionResult> results) {
        // Conservative = lowest score (most cautious)
        return results.stream()
                .min(Comparator.comparingDouble(DecisionResult::getScore))
                .orElse(results.getFirst());
    }

    private DecisionResult resolveBalanced(List<DecisionResult> results) {
        // Balanced = highest product of score and confidence
        return results.stream()
                .max(Comparator.comparingDouble(r -> r.getScore() * r.getConfidence()))
                .orElse(results.getFirst());
    }

    public enum ResolutionStrategy {
        HIGHEST_SCORE,
        HIGHEST_CONFIDENCE,
        MOST_CONSERVATIVE,
        BALANCED
    }

    public enum ConflictType {
        SCORE_DIFFERENCE,
        VALUE_MISMATCH,
        PRIORITY_CONFLICT
    }

    public record Conflict(
            String nodeId1,
            String nodeId2,
            ConflictType type,
            double severity,
            String description
    ) {}
}

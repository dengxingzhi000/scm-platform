package com.scmcloud.decision.matrix.core.fusion;

import com.scmcloud.decision.matrix.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Weighted fusion engine implementation.
 * <p>
 * Combines multiple decision results using weighted utility function with explainability.
 */
public class WeightedFusionEngine implements FusionEngine {

    private static final Logger log = LoggerFactory.getLogger(WeightedFusionEngine.class);

    @Override
    public DecisionResult fuse(List<DecisionResult> results, Map<String, Double> weights) {
        return fuseWithConflictResolution(results, weights, ConflictStrategy.WEIGHTED_AVERAGE);
    }

    @Override
    public DecisionResult fuseWithConflictResolution(List<DecisionResult> results,
                                                      Map<String, Double> weights,
                                                      ConflictStrategy conflictStrategy) {
        if (results == null || results.isEmpty()) {
            return createEmptyResult();
        }

        log.info("[FusionEngine] Fusing {} results with strategy [{}]", results.size(), conflictStrategy);

        return switch (conflictStrategy) {
            case WEIGHTED_AVERAGE -> fuseWeightedAverage(results, weights);
            case MAX_UTILITY -> fuseMaxUtility(results, weights);
            case PARETO_OPTIMAL -> fuseParetoOptimal(results, weights);
            case PRIORITY_BASED -> fusePriorityBased(results, weights);
        };
    }

    private DecisionResult fuseWeightedAverage(List<DecisionResult> results, Map<String, Double> weights) {
        double totalWeight = 0.0;
        double weightedScore = 0.0;
        double weightedConfidence = 0.0;
        Map<String, Double> factorWeights = new HashMap<>();
        Map<String, Double> factorScores = new HashMap<>();
        List<String> contributingFactors = new ArrayList<>();

        for (DecisionResult result : results) {
            String nodeId = result.getNodeId();
            double weight = weights.getOrDefault(nodeId, 1.0);

            weightedScore += result.getScore() * weight;
            weightedConfidence += result.getConfidence() * weight;
            totalWeight += weight;

            factorWeights.put(nodeId, weight);
            factorScores.put(nodeId, result.getScore());

            if (result.getExplanation() != null) {
                contributingFactors.addAll(result.getExplanation().getContributingFactors());
            }
        }

        double finalScore = totalWeight > 0 ? weightedScore / totalWeight : 0.0;
        double finalConfidence = totalWeight > 0 ? weightedConfidence / totalWeight : 0.0;

        // Select best value based on score
        Object bestValue = results.stream()
                .max(Comparator.comparingDouble(DecisionResult::getScore))
                .map(DecisionResult::getValue)
                .orElse(null);

        DecisionExplanation explanation = new DecisionExplanation(
                "fusion",
                "Weighted average of " + results.size() + " decisions",
                factorWeights, factorScores, contributingFactors,
                Map.of("totalWeight", totalWeight)
        );

        return new DecisionResult(
                "fusion", DecisionResult.DecisionStatus.SUCCESS,
                bestValue, finalScore, finalConfidence,
                explanation, null, null
        );
    }

    private DecisionResult fuseMaxUtility(List<DecisionResult> results, Map<String, Double> weights) {
        // Calculate utility for each result
        DecisionResult best = results.stream()
                .max(Comparator.comparingDouble(r -> calculateUtility(r, weights)))
                .orElse(results.getFirst());

        Map<String, Double> factorWeights = new HashMap<>();
        Map<String, Double> factorScores = new HashMap<>();

        for (DecisionResult result : results) {
            factorWeights.put(result.getNodeId(), weights.getOrDefault(result.getNodeId(), 1.0));
            factorScores.put(result.getNodeId(), calculateUtility(result, weights));
        }

        DecisionExplanation explanation = new DecisionExplanation(
                "fusion",
                "Max utility selection from " + results.size() + " decisions",
                factorWeights, factorScores,
                List.of("Selected: " + best.getNodeId()),
                Map.of("selectedScore", best.getScore())
        );

        return new DecisionResult(
                "fusion", DecisionResult.DecisionStatus.SUCCESS,
                best.getValue(), best.getScore(), best.getConfidence(),
                explanation, null, null
        );
    }

    private DecisionResult fuseParetoOptimal(List<DecisionResult> results, Map<String, Double> weights) {
        // Find Pareto optimal set
        List<DecisionResult> paretoSet = findParetoOptimal(results);

        if (paretoSet.isEmpty()) {
            return fuseWeightedAverage(results, weights);
        }

        // Select from Pareto set using weighted score
        DecisionResult best = paretoSet.stream()
                .max(Comparator.comparingDouble(r -> r.getScore() * weights.getOrDefault(r.getNodeId(), 1.0)))
                .orElse(paretoSet.getFirst());

        Map<String, Double> factorWeights = new HashMap<>();
        Map<String, Double> factorScores = new HashMap<>();

        for (DecisionResult result : paretoSet) {
            factorWeights.put(result.getNodeId(), weights.getOrDefault(result.getNodeId(), 1.0));
            factorScores.put(result.getNodeId(), result.getScore());
        }

        DecisionExplanation explanation = new DecisionExplanation(
                "fusion",
                "Pareto optimal selection from " + paretoSet.size() + " candidates",
                factorWeights, factorScores,
                List.of("Pareto set size: " + paretoSet.size()),
                Map.of("paretoSetSize", paretoSet.size())
        );

        return new DecisionResult(
                "fusion", DecisionResult.DecisionStatus.SUCCESS,
                best.getValue(), best.getScore(), best.getConfidence(),
                explanation, null, null
        );
    }

    private DecisionResult fusePriorityBased(List<DecisionResult> results, Map<String, Double> weights) {
        // Sort by score and select highest
        List<DecisionResult> sorted = results.stream()
                .sorted(Comparator.comparingDouble(DecisionResult::getScore).reversed())
                .toList();

        DecisionResult best = sorted.getFirst();

        Map<String, Double> factorWeights = new HashMap<>();
        Map<String, Double> factorScores = new HashMap<>();

        for (int i = 0; i < sorted.size(); i++) {
            DecisionResult result = sorted.get(i);
            factorWeights.put(result.getNodeId(), (double) (sorted.size() - i));
            factorScores.put(result.getNodeId(), result.getScore());
        }

        DecisionExplanation explanation = new DecisionExplanation(
                "fusion",
                "Priority-based selection, top choice: " + best.getNodeId(),
                factorWeights, factorScores,
                List.of("Priority order: " + sorted.stream().map(DecisionResult::getNodeId).toList()),
                null
        );

        return new DecisionResult(
                "fusion", DecisionResult.DecisionStatus.SUCCESS,
                best.getValue(), best.getScore(), best.getConfidence(),
                explanation, null, null
        );
    }

    private double calculateUtility(DecisionResult result, Map<String, Double> weights) {
        double weight = weights.getOrDefault(result.getNodeId(), 1.0);
        return result.getScore() * weight * result.getConfidence();
    }

    private List<DecisionResult> findParetoOptimal(List<DecisionResult> results) {
        List<DecisionResult> paretoSet = new ArrayList<>();

        for (DecisionResult candidate : results) {
            boolean isDominated = false;

            for (DecisionResult other : results) {
                if (candidate == other) continue;

                // Check if 'other' dominates 'candidate'
                if (other.getScore() >= candidate.getScore() &&
                        other.getConfidence() >= candidate.getConfidence() &&
                        (other.getScore() > candidate.getScore() || other.getConfidence() > candidate.getConfidence())) {
                    isDominated = true;
                    break;
                }
            }

            if (!isDominated) {
                paretoSet.add(candidate);
            }
        }

        return paretoSet;
    }

    private DecisionResult createEmptyResult() {
        return new DecisionResult(
                "fusion", DecisionResult.DecisionStatus.SKIPPED, null,
                0.0, 0.0, null, null, List.of("No results to fuse")
        );
    }
}

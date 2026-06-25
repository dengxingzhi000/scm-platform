package com.scmcloud.decision.optimizer;

import com.scmcloud.decision.constraint.Constraint;
import com.scmcloud.decision.constraint.ConstraintChain;
import com.scmcloud.decision.scoring.ScoringContext;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class GreedyOptimizer<T> implements Optimizer<List<T>, List<T>> {

    @Override
    public List<T> optimize(List<T> candidates, List<Constraint<List<T>>> constraints,
                            ScoringFunction<List<T>> scorer) {
        ScoringContext ctx = new ScoringContext();

        return candidates.stream()
                .filter(item -> {
                    ConstraintChain<List<T>> chain = new ConstraintChain<>(constraints);
                    return chain.allHardConstraintsPassed(List.of(item));
                })
                .sorted((a, b) -> Double.compare(
                        scorer.score(List.of(b), ctx),
                        scorer.score(List.of(a), ctx)))
                .collect(Collectors.toList());
    }

    @Override
    public String strategy() {
        return "GREEDY";
    }
}

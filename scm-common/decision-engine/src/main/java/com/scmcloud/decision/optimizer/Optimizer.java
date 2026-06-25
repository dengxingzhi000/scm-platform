package com.scmcloud.decision.optimizer;

import com.scmcloud.decision.constraint.Constraint;
import com.scmcloud.decision.scoring.ScoringContext;

import java.util.List;

public interface Optimizer<I, O> {
    O optimize(I input, List<Constraint<I>> constraints, ScoringFunction<I> scorer);
    String strategy();

    @FunctionalInterface
    interface ScoringFunction<I> {
        double score(I item, ScoringContext ctx);
    }
}

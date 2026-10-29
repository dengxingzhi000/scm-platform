package com.scmcloud.decision.optimizer;

import com.scmcloud.decision.constraint.Constraint;

import java.util.List;

public interface Optimizer<I, O> {
    O optimize(I input, List<Constraint<I>> constraints);
    String strategy();
}

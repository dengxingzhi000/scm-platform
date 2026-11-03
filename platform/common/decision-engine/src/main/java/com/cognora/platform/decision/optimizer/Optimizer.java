package com.cognora.platform.decision.optimizer;

public interface Optimizer<I, O> {
    O optimize(I input);
    String strategy();
}

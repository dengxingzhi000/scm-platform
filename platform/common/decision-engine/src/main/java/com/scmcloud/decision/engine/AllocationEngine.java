package com.scmcloud.decision.engine;

public interface AllocationEngine<I, O> extends DecisionEngine<I, O> {
    O allocate(I input);
}

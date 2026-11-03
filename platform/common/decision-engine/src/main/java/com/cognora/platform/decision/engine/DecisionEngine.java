package com.cognora.platform.decision.engine;

public interface DecisionEngine<I, O> {
    O decide(I input);
    String engineType();
}

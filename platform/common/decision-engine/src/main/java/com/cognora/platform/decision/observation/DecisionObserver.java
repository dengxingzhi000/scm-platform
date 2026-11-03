package com.cognora.platform.decision.observation;

public interface DecisionObserver {
    void observe(DecisionEvent event);
}

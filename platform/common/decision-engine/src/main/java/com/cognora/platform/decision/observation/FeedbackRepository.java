package com.cognora.platform.decision.observation;

import java.util.List;
import java.util.Map;

public interface FeedbackRepository {
    void save(DecisionEvent event);
    List<DecisionEvent> findByEngineType(String engineType);
    Map<String, Double> aggregateMetrics(String engineType);
}

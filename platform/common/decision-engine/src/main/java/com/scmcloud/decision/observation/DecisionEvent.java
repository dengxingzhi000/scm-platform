package com.scmcloud.decision.observation;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.Map;

@Data
public class DecisionEvent {
    private String eventId;
    private String engineType;
    private String decisionId;
    private String outcome;
    private Map<String, Double> metrics;
    private Map<String, Object> context;
    private LocalDateTime timestamp;
}

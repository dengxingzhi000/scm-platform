package com.scmcloud.decision.config;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class WeightProfile {
    private String id;
    private String engineType;
    private String scene;
    private int version;
    private Map<String, Double> weights;
    private List<WeightCondition> conditions;
    private boolean active;

    public void validate() {
        if (weights == null || weights.isEmpty()) {
            throw new IllegalArgumentException("Weights cannot be empty");
        }
        double sum = weights.values().stream().mapToDouble(Double::doubleValue).sum();
        if (Math.abs(sum - 1.0) > 0.001) {
            throw new IllegalArgumentException("Weights must sum to 1.0, got " + sum);
        }
    }

    public boolean matchesConditions(Map<String, Object> context) {
        if (conditions == null || conditions.isEmpty()) return true;
        return conditions.stream().allMatch(c -> {
            Object actual = context.get(c.getField());
            return c.matches(actual);
        });
    }
}

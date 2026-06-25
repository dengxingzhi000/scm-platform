package com.scmcloud.decision.scoring;

import lombok.Data;
import java.util.HashMap;
import java.util.Map;

@Data
public class ScoringContext {
    private Map<String, Object> variables = new HashMap<>();

    public ScoringContext withVariable(String key, Object value) {
        this.variables.put(key, value);
        return this;
    }

    @SuppressWarnings("unchecked")
    public <T> T getVariable(String key) {
        return (T) variables.get(key);
    }
}

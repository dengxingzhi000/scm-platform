package com.scmcloud.decision.engine;

import lombok.Data;
import java.util.HashMap;
import java.util.Map;

@Data
public class DecisionContext {
    private String engineType;
    private String scene;
    private String userId;
    private Map<String, Object> attributes = new HashMap<>();

    public DecisionContext withAttribute(String key, Object value) {
        this.attributes.put(key, value);
        return this;
    }

    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key) {
        return (T) attributes.get(key);
    }
}

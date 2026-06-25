package com.scmcloud.decision.matrix.api;

import java.util.Map;

/**
 * Decision context that carries data through the decision chain.
 */
public class DecisionContext {

    private final String contextId;
    private final String businessType;
    private final Map<String, Object> attributes;
    private final Map<String, Object> metadata;

    public DecisionContext(String contextId, String businessType,
                           Map<String, Object> attributes, Map<String, Object> metadata) {
        this.contextId = contextId;
        this.businessType = businessType;
        this.attributes = attributes != null ? attributes : Map.of();
        this.metadata = metadata != null ? metadata : Map.of();
    }

    public String getContextId() {
        return contextId;
    }

    public String getBusinessType() {
        return businessType;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key) {
        return (T) attributes.get(key);
    }

    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key, T defaultValue) {
        return (T) attributes.getOrDefault(key, defaultValue);
    }

    public DecisionContext withAttribute(String key, Object value) {
        Map<String, Object> newAttrs = new java.util.HashMap<>(attributes);
        newAttrs.put(key, value);
        return new DecisionContext(contextId, businessType, newAttrs, metadata);
    }
}

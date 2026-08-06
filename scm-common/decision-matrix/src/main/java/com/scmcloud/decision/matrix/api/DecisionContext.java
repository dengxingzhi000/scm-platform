package com.scmcloud.decision.matrix.api;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.HashMap;
import java.util.Map;

/**
 * Decision context that carries data through the decision chain.
 */
@Getter
@ToString
@EqualsAndHashCode
public class DecisionContext {
    private final String contextId;
    private final String businessType;
    private final Map<String, Object> attributes;
    private final Map<String, Object> metadata;

    @Builder
    private DecisionContext(String contextId, String businessType,
                            Map<String, Object> attributes, Map<String, Object> metadata) {
        this.contextId = contextId;
        this.businessType = businessType;
        this.attributes = attributes != null ? Map.copyOf(attributes) : Map.of();
        this.metadata = metadata != null ? Map.copyOf(metadata) : Map.of();
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
        Map<String, Object> newAttrs = new HashMap<>(attributes);
        newAttrs.put(key, value);
        return DecisionContext.builder()
                .contextId(contextId)
                .businessType(businessType)
                .attributes(newAttrs)
                .metadata(metadata)
                .build();
    }
}

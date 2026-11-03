package com.scmcloud.common.integration.saga;

import lombok.Getter;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Context passed between saga steps. Carries data (e.g., reservation ID, order ID)
 * and metadata (tenant ID, correlation ID).
 */
@Getter
public class SagaContext {

    private final String sagaId;
    private final UUID tenantId;
    private final String correlationId;
    private final Map<String, Object> data = new ConcurrentHashMap<>();

    public SagaContext(UUID tenantId, String correlationId) {
        this.sagaId = UUID.randomUUID().toString();
        this.tenantId = tenantId;
        this.correlationId = correlationId;
    }

    public void put(String key, Object value) {
        data.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) data.get(key);
    }

    public <T> T getOrDefault(String key, T defaultValue) {
        T value = get(key);
        return value != null ? value : defaultValue;
    }

    public boolean has(String key) {
        return data.containsKey(key);
    }

    public Map<String, Object> getData() {
        return Map.copyOf(data);
    }
}

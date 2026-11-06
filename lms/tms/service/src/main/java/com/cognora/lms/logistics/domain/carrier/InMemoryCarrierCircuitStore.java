package com.cognora.lms.logistics.domain.carrier;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 进程内熔断状态存储：单实例部署与单元测试使用。
 */
public class InMemoryCarrierCircuitStore implements CarrierCircuitStore {

    private static final class Circuit {
        private int consecutiveFailures;
        private long openUntilMillis;
    }

    private final Map<String, Circuit> circuits = new ConcurrentHashMap<>();

    @Override
    public boolean isOpen(String carrierId, long nowMillis) {
        Circuit circuit = circuits.computeIfAbsent(carrierId, key -> new Circuit());
        synchronized (circuit) {
            return circuit.openUntilMillis > nowMillis;
        }
    }

    @Override
    public void recordFailure(String carrierId, int failureThreshold, long cooldownMillis, long nowMillis) {
        Circuit circuit = circuits.computeIfAbsent(carrierId, key -> new Circuit());
        synchronized (circuit) {
            circuit.consecutiveFailures++;
            if (circuit.consecutiveFailures >= failureThreshold) {
                circuit.openUntilMillis = nowMillis + cooldownMillis;
            }
        }
    }

    @Override
    public void recordSuccess(String carrierId) {
        Circuit circuit = circuits.computeIfAbsent(carrierId, key -> new Circuit());
        synchronized (circuit) {
            circuit.consecutiveFailures = 0;
            circuit.openUntilMillis = 0L;
        }
    }
}

package com.scmcloud.common.data.rw.circuitbreaker;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.Tracer;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import com.alibaba.csp.sentinel.slots.block.degrade.circuitbreaker.CircuitBreakerStrategy;
import com.scmcloud.common.data.rw.routing.ReadWriteRoutingDataSource;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Slave circuit breaker.
 * <p>
 * Based on Sentinel, implements slave circuit breaking:
 * - Slow request ratio
 * - Error ratio
 * - Error count
 *
 * @author Deng
 * @since 2025-12-16
 */
@Slf4j
public class SlaveCircuitBreaker {
    private static final String RESOURCE_PREFIX = "slave:";

    private final Map<String, ReadWriteRoutingDataSource> routingDataSources;
    private final Map<String, CircuitBreakerState> circuitStates = new ConcurrentHashMap<>();

    public enum CircuitBreakerState {
        CLOSED,
        OPEN,
        HALF_OPEN
    }

    public SlaveCircuitBreaker(Map<String, ReadWriteRoutingDataSource> routingDataSources) {
        this.routingDataSources = routingDataSources;
        initDegradeRules();
    }

    private void initDegradeRules() {
        List<DegradeRule> rules = new ArrayList<>();

        for (String groupName : routingDataSources.keySet()) {
            ReadWriteRoutingDataSource ds = routingDataSources.get(groupName);
            for (String slaveName : ds.getSlaveAvailability().keySet()) {
                String resource = RESOURCE_PREFIX + groupName + "." + slaveName;

                DegradeRule slowRule = new DegradeRule(resource)
                        .setGrade(CircuitBreakerStrategy.SLOW_REQUEST_RATIO.getType())
                        .setCount(0.5)
                        .setSlowRatioThreshold(0.5)
                        .setTimeWindow(30)
                        .setMinRequestAmount(10)
                        .setStatIntervalMs(10000);

                DegradeRule exceptionRule = new DegradeRule(resource)
                        .setGrade(CircuitBreakerStrategy.ERROR_RATIO.getType())
                        .setCount(0.5)
                        .setTimeWindow(30)
                        .setMinRequestAmount(10)
                        .setStatIntervalMs(10000);

                rules.add(slowRule);
                rules.add(exceptionRule);

                circuitStates.put(resource, CircuitBreakerState.CLOSED);
                log.debug("[Circuit-Breaker] Registered rules for slave: {}", resource);
            }
        }

        DegradeRuleManager.loadRules(rules);
        log.debug("[Circuit-Breaker] Loaded {} degrade rules for {} slaves",
                rules.size(), circuitStates.size());
    }

    /**
     * Execute operation with circuit breaker protection.
     *
     * @param groupName datasource group name
     * @param slaveName slave name
     * @param operation operation to execute
     * @param fallback  fallback when circuit is open
     * @param <T>       return type
     * @return execution result
     */
    public <T> T executeWithCircuitBreaker(String groupName, String slaveName, SlaveOperation<T> operation,
                                            SlaveFallback<T> fallback) throws Exception {
        String resource = RESOURCE_PREFIX + groupName + "." + slaveName;

        Entry entry;
        try {
            entry = SphU.entry(resource);
        } catch (BlockException e) {
            log.warn("[Circuit-Breaker] Slave [{}] is blocked, circuit is OPEN", resource);
            circuitStates.put(resource, CircuitBreakerState.OPEN);
            markSlaveUnavailable(groupName, slaveName);
            return fallback.fallback(e);
        }

        try (entry) {
            T result = operation.execute();
            circuitStates.put(resource, CircuitBreakerState.CLOSED);
            return result;
        } catch (Exception e) {
            Tracer.trace(e);
            throw e;
        }
    }

    private void markSlaveUnavailable(String groupName, String slaveName) {
        ReadWriteRoutingDataSource ds = routingDataSources.get(groupName);
        if (ds != null) {
            ds.markSlaveUnavailable(slaveName);
            log.warn("[Circuit-Breaker] Marked slave [{}] in group [{}] as UNAVAILABLE",
                    slaveName, groupName);
        }
    }

    public CircuitBreakerState getState(String groupName, String slaveName) {
        String resource = RESOURCE_PREFIX + groupName + "." + slaveName;
        return circuitStates.getOrDefault(resource, CircuitBreakerState.CLOSED);
    }

    public Map<String, CircuitBreakerState> getAllStates() {
        return Map.copyOf(circuitStates);
    }

    @FunctionalInterface
    public interface SlaveOperation<T> {
        T execute() throws Exception;
    }

    @FunctionalInterface
    public interface SlaveFallback<T> {
        T fallback(BlockException e);
    }
}

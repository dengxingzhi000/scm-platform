package com.cognora.platform.common.data.rw.circuitbreaker;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.Tracer;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import com.alibaba.csp.sentinel.slots.block.degrade.circuitbreaker.CircuitBreakerStrategy;
import com.cognora.platform.common.data.rw.config.ReadWriteProperties;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Slave circuit breaker.
 * <p>
 * Sentinel wraps the <b>whole</b> connection lifetime: the entry is created when a
 * slave connection is handed to the caller and exited when the connection is closed
 * (or immediately when acquiring the connection fails). That means:
 * <ul>
 *   <li>slow queries / transactions on a replica count towards the slow-request ratio;</li>
 *   <li>SQL exceptions thrown while the connection is held count towards the error ratio;</li>
 *   <li>once the circuit opens, {@link #isOpen} makes the routing datasource skip the
 *       slave entirely, so no further probe or connection attempt reaches it;</li>
 *   <li>after {@code time-window-seconds} a trial request is allowed; success closes the
 *       circuit, another block re-opens it.</li>
 * </ul>
 * If Sentinel is disabled ({@code spring.datasource.rw.circuit-breaker.enabled=false})
 * the breaker is inert and the routing datasource still falls back to master on the
 * first failed connection probe.
 *
 * @author Deng
 * @since 2025-12-16
 */
@Slf4j
public class SlaveCircuitBreaker {
    private static final String RESOURCE_PREFIX = "slave:";

    private final ReadWriteProperties properties;

    private final Map<String, CircuitBreakerState> circuitStates = new ConcurrentHashMap<>();
    private final Map<String, Long> openUntilMs = new ConcurrentHashMap<>();
    private final Set<String> registeredResources = ConcurrentHashMap.newKeySet();

    public enum CircuitBreakerState {
        CLOSED,
        OPEN,
        HALF_OPEN
    }

    public SlaveCircuitBreaker(ReadWriteProperties properties) {
        this.properties = properties;
    }

    /**
     * Register Sentinel degrade rules for every configured slave. Must be called once
     * after all routing datasources are created, because
     * {@code DegradeRuleManager.loadRules} replaces the whole rule set of the JVM.
     *
     * @param groupSlaves group name to slave names
     */
    public void registerGroups(Map<String, ? extends Collection<String>> groupSlaves) {
        if (!properties.getCircuitBreaker().isEnabled()) {
            log.info("[Circuit-Breaker] Disabled by configuration; relying on connection probes only");
            return;
        }

        List<DegradeRule> rules = new ArrayList<>();
        groupSlaves.forEach((group, slaves) -> slaves.forEach(slave -> {
            String resource = resource(group, slave);
            if (!registeredResources.add(resource)) {
                return;
            }
            circuitStates.put(resource, CircuitBreakerState.CLOSED);
            rules.add(buildSlowRule(resource));
            rules.add(buildErrorRule(resource));
            log.debug("[Circuit-Breaker] Registered rules for slave: {}", resource);
        }));

        if (rules.isEmpty()) {
            return;
        }

        DegradeRuleManager.loadRules(rules);
        log.info("[Circuit-Breaker] Loaded {} Sentinel degrade rules for {} slaves",
                rules.size(), registeredResources.size());
    }

    private DegradeRule buildSlowRule(String resource) {
        ReadWriteProperties.CircuitBreakerProperties cb = properties.getCircuitBreaker();
        return new DegradeRule(resource)
                .setGrade(CircuitBreakerStrategy.SLOW_REQUEST_RATIO.getType())
                // For SLOW_REQUEST_RATIO the count is the max acceptable RT in ms.
                .setCount(cb.getSlowRtMs())
                .setSlowRatioThreshold(cb.getSlowRatioThreshold())
                .setTimeWindow(cb.getTimeWindowSeconds())
                .setMinRequestAmount(cb.getMinRequestAmount())
                .setStatIntervalMs(cb.getStatIntervalMs());
    }

    private DegradeRule buildErrorRule(String resource) {
        ReadWriteProperties.CircuitBreakerProperties cb = properties.getCircuitBreaker();
        return new DegradeRule(resource)
                .setGrade(CircuitBreakerStrategy.ERROR_RATIO.getType())
                .setCount(cb.getErrorRatio())
                .setTimeWindow(cb.getTimeWindowSeconds())
                .setMinRequestAmount(cb.getMinRequestAmount())
                .setStatIntervalMs(cb.getStatIntervalMs());
    }

    /**
     * Whether the slave circuit is open. When the configured window has elapsed the
     * state moves to HALF_OPEN and one trial is allowed.
     */
    public boolean isOpen(String groupName, String slaveName) {
        if (!properties.getCircuitBreaker().isEnabled()) {
            return false;
        }
        String resource = resource(groupName, slaveName);
        Long until = openUntilMs.get(resource);
        if (until == null) {
            return false;
        }
        if (System.currentTimeMillis() < until) {
            return true;
        }
        openUntilMs.remove(resource);
        circuitStates.put(resource, CircuitBreakerState.HALF_OPEN);
        log.info("[Circuit-Breaker] Slave [{}] window elapsed, allowing a trial request", resource);
        return false;
    }

    /**
     * Acquire a slave connection guarded by Sentinel.
     *
     * @return the connection (a proxy that exits the Sentinel entry on close), or
     *         {@code null} when the circuit is open and the caller must fall back to master
     */
    public Connection connect(String groupName, String slaveName, ConnectionSupplier supplier)
            throws SQLException {
        if (!properties.getCircuitBreaker().isEnabled()) {
            return supplier.get();
        }

        String resource = resource(groupName, slaveName);
        Entry entry;
        try {
            entry = SphU.entry(resource);
        } catch (BlockException e) {
            openUntilMs.put(resource,
                    System.currentTimeMillis() + properties.getCircuitBreaker().getTimeWindowSeconds() * 1000L);
            circuitStates.put(resource, CircuitBreakerState.OPEN);
            log.warn("[Circuit-Breaker] Slave [{}] is OPEN, falling back to master", resource);
            return null;
        }

        Connection connection;
        try {
            connection = supplier.get();
        } catch (SQLException | RuntimeException e) {
            Tracer.trace(e);
            entry.exit();
            throw e;
        }

        openUntilMs.remove(resource);
        circuitStates.put(resource, CircuitBreakerState.CLOSED);
        return wrap(connection, entry, resource);
    }

    private Connection wrap(Connection delegate, Entry entry, String resource) {
        AtomicBoolean exited = new AtomicBoolean(false);
        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                (proxy, method, args) -> {
                    try {
                        return method.invoke(delegate, args);
                    } catch (InvocationTargetException e) {
                        Throwable cause = e.getCause();
                        if (cause instanceof SQLException sqlException) {
                            Tracer.trace(sqlException);
                        }
                        throw cause;
                    } finally {
                        if ("close".equals(method.getName()) && exited.compareAndSet(false, true)) {
                            entry.exit();
                            if (circuitStates.get(resource) == CircuitBreakerState.HALF_OPEN) {
                                circuitStates.put(resource, CircuitBreakerState.CLOSED);
                                log.info("[Circuit-Breaker] Slave [{}] trial succeeded, circuit CLOSED", resource);
                            }
                        }
                    }
                });
    }

    public CircuitBreakerState getState(String groupName, String slaveName) {
        return circuitStates.getOrDefault(resource(groupName, slaveName), CircuitBreakerState.CLOSED);
    }

    public Map<String, CircuitBreakerState> getAllStates() {
        return Map.copyOf(circuitStates);
    }

    public Map<String, Object> getSnapshot() {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        circuitStates.forEach((resource, state) -> {
            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("state", state.name());
            Long until = openUntilMs.get(resource);
            detail.put("openUntil", until == null ? null : until);
            snapshot.put(resource, detail);
        });
        return snapshot;
    }

    private String resource(String groupName, String slaveName) {
        return RESOURCE_PREFIX + groupName + "." + slaveName;
    }

    @FunctionalInterface
    public interface ConnectionSupplier {
        Connection get() throws SQLException;
    }
}

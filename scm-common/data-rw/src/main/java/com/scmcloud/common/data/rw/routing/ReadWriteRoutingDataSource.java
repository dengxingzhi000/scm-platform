package com.scmcloud.common.data.rw.routing;

import com.scmcloud.common.data.rw.config.ReadWriteProperties;
import com.scmcloud.common.data.rw.loadbalance.SlaveLoadBalancer;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import javax.sql.DataSource;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Read-write separation routing datasource.
 * <p>
 * Based on Spring AbstractRoutingDataSource, supports:
 * - Master-slave automatic routing
 * - Load balancing
 * - Health checking
 * - Read-write consistency
 * - Slave fault retry
 * - Runtime hot-switch load balance strategy
 *
 * @author Deng
 * @since 2025-12-16
 */
@Slf4j
public class ReadWriteRoutingDataSource extends AbstractRoutingDataSource {
    private static final String MASTER_KEY = "master";

    private final String groupName;
    private final ReadWriteProperties properties;

    private final AtomicReference<SlaveLoadBalancer> loadBalancerRef;

    @Setter
    private List<SlaveLoadBalancer.SlaveInfo> slaveInfos;

    private final Map<String, Boolean> slaveAvailability = new ConcurrentHashMap<>();
    private final Map<String, DataSource> slaveDataSourceMap = new ConcurrentHashMap<>();
    private final Map<String, Long> lastValidationTime = new ConcurrentHashMap<>();
    private static final long VALIDATION_CACHE_MS = 5000; // 5s cache

    // Metrics
    private Counter masterRouteCounter;
    private Counter slaveRouteCounter;
    private Counter fallbackCounter;
    private Counter retryCounter;

    public ReadWriteRoutingDataSource(String groupName,
                                       DataSource masterDataSource,
                                       Map<String, DataSource> slaveDataSources,
                                       ReadWriteProperties properties,
                                       SlaveLoadBalancer loadBalancer,
                                       MeterRegistry meterRegistry) {
        this.groupName = groupName;
        this.properties = properties;
        this.loadBalancerRef = new AtomicReference<>(loadBalancer);

        Map<Object, Object> targetDataSources = new ConcurrentHashMap<>();
        targetDataSources.put(MASTER_KEY, masterDataSource);
        targetDataSources.putAll(slaveDataSources);

        setTargetDataSources(targetDataSources);
        setDefaultTargetDataSource(masterDataSource);

        slaveDataSources.forEach((name, ds) -> {
            slaveAvailability.put(name, true);
            slaveDataSourceMap.put(name, ds);
        });

        if (meterRegistry != null) {
            initMetrics(meterRegistry);
        }
    }

    private void initMetrics(MeterRegistry meterRegistry) {
        this.masterRouteCounter = Counter.builder("datasource.rw.route")
                .tag("group", groupName)
                .tag("target", "master")
                .description("Number of routes to master")
                .register(meterRegistry);

        this.slaveRouteCounter = Counter.builder("datasource.rw.route")
                .tag("group", groupName)
                .tag("target", "slave")
                .description("Number of routes to slave")
                .register(meterRegistry);

        this.fallbackCounter = Counter.builder("datasource.rw.fallback")
                .tag("group", groupName)
                .description("Number of fallbacks to master")
                .register(meterRegistry);

        this.retryCounter = Counter.builder("datasource.rw.retry")
                .tag("group", groupName)
                .description("Number of slave retries")
                .register(meterRegistry);
    }

    @Override
    protected Object determineCurrentLookupKey() {
        // 1. Check if should use master
        long readMasterAfterWriteMs = properties.getReadMasterAfterWrite().toMillis();
        if (ReadWriteRoutingContext.shouldUseMaster(readMasterAfterWriteMs)) {
            log.debug("[RW-Routing] Group [{}] routing to MASTER", groupName);
            incrementMasterCounter();
            return MASTER_KEY;
        }

        // 2. Check routing type
        ReadWriteRoutingContext.RoutingType routingType = ReadWriteRoutingContext.current();

        if (routingType == ReadWriteRoutingContext.RoutingType.MASTER) {
            log.debug("[RW-Routing] Group [{}] routing to MASTER (explicit)", groupName);
            incrementMasterCounter();
            return MASTER_KEY;
        }

        // 3. Try routing to slave (with retry)
        if (routingType == ReadWriteRoutingContext.RoutingType.SLAVE ||
                routingType == ReadWriteRoutingContext.RoutingType.AUTO) {

            // Check if specific slave is specified
            String specifiedSlave = ReadWriteRoutingContext.getSpecifiedSlave();
            if (specifiedSlave != null && slaveAvailability.getOrDefault(specifiedSlave, false)) {
                log.debug("[RW-Routing] Group [{}] routing to SLAVE [{}] (specified)",
                        groupName, specifiedSlave);
                incrementSlaveCounter();
                return specifiedSlave;
            }

            // Use retry mechanism to select slave
            String selectedSlave = selectSlaveWithRetry();
            if (selectedSlave != null) {
                log.debug("[RW-Routing] Group [{}] routing to SLAVE [{}]",
                        groupName, selectedSlave);
                incrementSlaveCounter();
                return selectedSlave;
            }

            // No available slave, fallback to master
            log.warn("[RW-Routing] Group [{}] no available slave, fallback to MASTER", groupName);
            incrementFallbackCounter();
        }

        incrementMasterCounter();
        return MASTER_KEY;
    }

    /**
     * Select slave with retry.
     * <p>
     * When selected slave is unavailable, automatically try other slaves.
     */
    private String selectSlaveWithRetry() {
        if (slaveInfos == null || slaveInfos.isEmpty()) {
            return null;
        }

        List<SlaveLoadBalancer.SlaveInfo> availableSlaves = slaveInfos.stream()
                .filter(s -> slaveAvailability.getOrDefault(s.name(), false))
                .toList();

        if (availableSlaves.isEmpty()) {
            return null;
        }

        int maxRetries = Math.min(properties.getSlaveRetryCount(), availableSlaves.size());
        Set<String> triedSlaves = new HashSet<>();

        for (int i = 0; i < maxRetries; i++) {
            List<SlaveLoadBalancer.SlaveInfo> remainingSlaves = availableSlaves.stream()
                    .filter(s -> !triedSlaves.contains(s.name()))
                    .toList();

            if (remainingSlaves.isEmpty()) {
                break;
            }

            String selected = loadBalancerRef.get().select(remainingSlaves);
            if (selected == null) {
                break;
            }

            if (isSlaveConnectionValid(selected)) {
                if (i > 0) {
                    incrementRetryCounter();
                    log.info("[RW-Routing] Group [{}] retry succeeded with slave [{}] after {} attempts",
                            groupName, selected, i + 1);
                }
                return selected;
            }

            triedSlaves.add(selected);
            log.warn("[RW-Routing] Group [{}] slave [{}] connection invalid, trying next slave",
                    groupName, selected);
        }

        return null;
    }

    /**
     * Check slave connection validity (with cache).
     */
    private boolean isSlaveConnectionValid(String slaveName) {
        Long lastCheck = lastValidationTime.get(slaveName);
        if (lastCheck != null && System.currentTimeMillis() - lastCheck < VALIDATION_CACHE_MS) {
            return slaveAvailability.getOrDefault(slaveName, false);
        }

        DataSource dataSource = slaveDataSourceMap.get(slaveName);
        if (dataSource == null) {
            return false;
        }

        try (var connection = dataSource.getConnection()) {
            boolean valid = connection.isValid(3);
            lastValidationTime.put(slaveName, System.currentTimeMillis());
            return valid;
        } catch (Exception e) {
            log.debug("[RW-Routing] Group [{}] slave [{}] connection check failed: {}",
                    groupName, slaveName, e.getMessage());
            lastValidationTime.put(slaveName, System.currentTimeMillis());
            return false;
        }
    }

    public void markSlaveUnavailable(String slaveName) {
        slaveAvailability.put(slaveName, false);
        log.warn("[RW-Routing] Group [{}] slave [{}] marked as UNAVAILABLE",
                groupName, slaveName);
    }

    public void markSlaveAvailable(String slaveName) {
        slaveAvailability.put(slaveName, true);
        log.info("[RW-Routing] Group [{}] slave [{}] marked as AVAILABLE",
                groupName, slaveName);
    }

    public Map<String, Boolean> getSlaveAvailability() {
        return Map.copyOf(slaveAvailability);
    }

    /**
     * Switch load balance strategy at runtime.
     *
     * @param newLoadBalancer new load balancer
     */
    public void switchLoadBalancer(SlaveLoadBalancer newLoadBalancer) {
        SlaveLoadBalancer oldLoadBalancer = loadBalancerRef.getAndSet(newLoadBalancer);
        log.info("[RW-Routing] Group [{}] load balancer switched from {} to {}",
                groupName,
                oldLoadBalancer.getClass().getSimpleName(),
                newLoadBalancer.getClass().getSimpleName());
    }

    public String getCurrentLoadBalancerName() {
        return loadBalancerRef.get().getClass().getSimpleName();
    }

    private void incrementMasterCounter() {
        if (masterRouteCounter != null) {
            masterRouteCounter.increment();
        }
    }

    private void incrementSlaveCounter() {
        if (slaveRouteCounter != null) {
            slaveRouteCounter.increment();
        }
    }

    private void incrementFallbackCounter() {
        if (fallbackCounter != null) {
            fallbackCounter.increment();
        }
    }

    private void incrementRetryCounter() {
        if (retryCounter != null) {
            retryCounter.increment();
        }
    }
}

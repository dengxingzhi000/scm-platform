package com.scmcloud.common.data.rw.routing;

import com.scmcloud.common.data.rw.circuitbreaker.SlaveCircuitBreaker;
import com.scmcloud.common.data.rw.config.ReadWriteProperties;
import com.scmcloud.common.data.rw.loadbalance.SlaveLoadBalancer;
import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;

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
 * <p>
 * The lookup key is only consulted when a <i>new</i> connection is requested. Inside
 * a Spring transaction the connection is bound to the transaction, so the routing
 * decision of the outermost transactional method is final for that transaction.
 *
 * @author Deng
 * @since 2025-12-16
 */
@Slf4j
public class ReadWriteRoutingDataSource extends AbstractRoutingDataSource {
    private static final String MASTER_KEY = "master";

    private final String groupName;
    private final ReadWriteProperties properties;
    private final DataSource masterDataSource;
    private final SlaveCircuitBreaker circuitBreaker;

    private final AtomicReference<SlaveLoadBalancer> loadBalancerRef;

    @Setter
    private volatile List<SlaveLoadBalancer.SlaveInfo> slaveInfos;

    private final Map<String, Boolean> slaveAvailability = new ConcurrentHashMap<>();
    private final Map<String, DataSource> slaveDataSourceMap = new ConcurrentHashMap<>();
    private final Map<String, Long> lastValidationTime = new ConcurrentHashMap<>();
    private static final long VALIDATION_CACHE_MS = 5000; // 5s cache

    // Route counters. They are the single source of truth for both the actuator
    // endpoint and the micrometer counters.
    private final LongAdder masterRouteCount = new LongAdder();
    private final LongAdder slaveRouteCount = new LongAdder();
    private final LongAdder fallbackCount = new LongAdder();
    private final LongAdder retryCount = new LongAdder();

    public ReadWriteRoutingDataSource(String groupName,
                                       DataSource masterDataSource,
                                       Map<String, DataSource> slaveDataSources,
                                       ReadWriteProperties properties,
                                       SlaveLoadBalancer loadBalancer,
                                       MeterRegistry meterRegistry) {
        this(groupName, masterDataSource, slaveDataSources, properties, loadBalancer, meterRegistry, null);
    }

    public ReadWriteRoutingDataSource(String groupName,
                                       DataSource masterDataSource,
                                       Map<String, DataSource> slaveDataSources,
                                       ReadWriteProperties properties,
                                       SlaveLoadBalancer loadBalancer,
                                       MeterRegistry meterRegistry,
                                       SlaveCircuitBreaker circuitBreaker) {
        this.groupName = groupName;
        this.properties = properties;
        this.masterDataSource = masterDataSource;
        this.circuitBreaker = circuitBreaker;
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

        if (slaveDataSources.isEmpty()) {
            log.info("[RW-Routing] Group [{}] has no slave configured; every read routes to MASTER. "
                    + "Configure spring.datasource.rw.groups.{}.slaves to enable read splitting.",
                    groupName, groupName);
        }

        if (meterRegistry != null) {
            initMetrics(meterRegistry);
        }
    }

    private void initMetrics(MeterRegistry meterRegistry) {
        FunctionCounter.builder("datasource.rw.route", masterRouteCount, adder -> (double) adder.sum())
                .tag("group", groupName)
                .tag("target", "master")
                .description("Number of routes to master")
                .register(meterRegistry);

        FunctionCounter.builder("datasource.rw.route", slaveRouteCount, adder -> (double) adder.sum())
                .tag("group", groupName)
                .tag("target", "slave")
                .description("Number of routes to slave")
                .register(meterRegistry);

        FunctionCounter.builder("datasource.rw.fallback", fallbackCount, adder -> (double) adder.sum())
                .tag("group", groupName)
                .description("Number of fallbacks to master")
                .register(meterRegistry);

        FunctionCounter.builder("datasource.rw.retry", retryCount, adder -> (double) adder.sum())
                .tag("group", groupName)
                .description("Number of slave retries")
                .register(meterRegistry);
    }

    @Override
    protected Object determineCurrentLookupKey() {
        // 1. Read-after-write window / forced master
        if (ReadWriteRoutingContext.shouldUseMaster(properties.getReadMasterAfterWrite().toMillis())) {
            log.debug("[RW-Routing] Group [{}] routing to MASTER", groupName);
            return master();
        }

        ReadWriteRoutingContext.RoutingType routingType = ReadWriteRoutingContext.current();

        if (routingType == ReadWriteRoutingContext.RoutingType.MASTER) {
            log.debug("[RW-Routing] Group [{}] routing to MASTER (explicit)", groupName);
            return master();
        }

        if (routingType == ReadWriteRoutingContext.RoutingType.AUTO
                && properties.getDefaultTarget() == ReadWriteProperties.RoutingTarget.MASTER) {
            // Safe default: AUTO never reaches a replica, including threads that lost
            // their routing context (async pools, scheduled jobs, startup).
            log.debug("[RW-Routing] Group [{}] routing to MASTER (auto default)", groupName);
            return master();
        }

        // 2. @Slave (or AUTO with default-target=SLAVE)
        String specifiedSlave = ReadWriteRoutingContext.getSpecifiedSlave();
        if (specifiedSlave != null && isSlaveUsable(specifiedSlave)) {
            log.debug("[RW-Routing] Group [{}] routing to SLAVE [{}] (specified)", groupName, specifiedSlave);
            return slave(specifiedSlave);
        }
        if (specifiedSlave != null) {
            log.debug("[RW-Routing] Group [{}] specified slave [{}] is unavailable", groupName, specifiedSlave);
            if (!ReadWriteRoutingContext.isFallbackToMaster()) {
                throw new ReadWriteRoutingException("Slave [" + specifiedSlave + "] of group [" + groupName
                        + "] is unavailable and fallbackToMaster=false");
            }
            return fallback(specifiedSlave);
        }

        String selectedSlave = selectSlaveWithRetry();
        if (selectedSlave != null) {
            log.debug("[RW-Routing] Group [{}] routing to SLAVE [{}]", groupName, selectedSlave);
            return slave(selectedSlave);
        }

        if (!ReadWriteRoutingContext.isFallbackToMaster()) {
            throw new ReadWriteRoutingException("No available slave in group [" + groupName
                    + "] and fallbackToMaster=false");
        }

        if (slaveInfos == null || slaveInfos.isEmpty()) {
            // 该 group 未配置从库（单机部署）。注解语义仍然生效，所有读落到主库，
            // 但这是预期状态，不应每次查询都告警（启动时已打过一次 INFO）。
            log.debug("[RW-Routing] Group [{}] has no slave configured, routing to MASTER", groupName);
        } else {
            log.debug("[RW-Routing] Group [{}] no available slave, fallback to MASTER", groupName);
        }
        return fallback(null);
    }

    private Object master() {
        masterRouteCount.increment();
        ReadWriteRoutingContext.recordRoute(MASTER_KEY, null, false);
        return MASTER_KEY;
    }

    private Object slave(String slaveName) {
        slaveRouteCount.increment();
        ReadWriteRoutingContext.recordRoute(slaveName, slaveName, false);
        return slaveName;
    }

    private Object fallback(String slaveName) {
        fallbackCount.increment();
        ReadWriteRoutingContext.recordRoute(MASTER_KEY, slaveName, true);
        return MASTER_KEY;
    }

    private boolean isSlaveUsable(String slaveName) {
        return slaveAvailability.getOrDefault(slaveName, false)
                && !isCircuitOpen(slaveName)
                && isSlaveConnectionValid(slaveName);
    }

    private boolean isCircuitOpen(String slaveName) {
        return circuitBreaker != null && circuitBreaker.isOpen(groupName, slaveName);
    }

    @Override
    public Connection getConnection() throws SQLException {
        return acquire(DataSource::getConnection);
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return acquire(dataSource -> dataSource.getConnection(username, password));
    }

    /**
     * Resolve the target once and guard slave connections with the circuit breaker.
     * When the circuit is open the caller gets a master connection instead of a
     * failure, and {@code datasource.rw.fallback} is incremented.
     */
    private Connection acquire(ConnectionFactory factory) throws SQLException {
        Object key = determineCurrentLookupKey();
        DataSource target = resolveTarget(key);

        if (circuitBreaker == null || MASTER_KEY.equals(key) || key == null) {
            return factory.get(target);
        }

        String slaveName = key.toString();
        Connection connection = circuitBreaker.connect(groupName, slaveName, () -> factory.get(target));
        if (connection != null) {
            return connection;
        }

        log.warn("[RW-Routing] Group [{}] slave [{}] circuit is open, using MASTER", groupName, slaveName);
        fallbackCount.increment();
        ReadWriteRoutingContext.recordRoute(MASTER_KEY, slaveName, true);
        return factory.get(masterDataSource);
    }

    private DataSource resolveTarget(Object key) {
        if (key == null || MASTER_KEY.equals(key)) {
            return masterDataSource;
        }
        DataSource target = slaveDataSourceMap.get(key.toString());
        if (target == null) {
            throw new IllegalStateException("Cannot determine target DataSource for lookup key [" + key + "]");
        }
        return target;
    }

    @FunctionalInterface
    private interface ConnectionFactory {
        Connection get(DataSource dataSource) throws SQLException;
    }

    /**
     * Select slave with retry.
     * <p>
     * When selected slave is unavailable, automatically try other slaves.
     */
    private String selectSlaveWithRetry() {
        List<SlaveLoadBalancer.SlaveInfo> infos = slaveInfos;
        if (infos == null || infos.isEmpty()) {
            return null;
        }

        List<SlaveLoadBalancer.SlaveInfo> availableSlaves = infos.stream()
                .filter(s -> slaveAvailability.getOrDefault(s.name(), false))
                .filter(s -> !isCircuitOpen(s.name()))
                .map(this::withLiveActiveConnections)
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
                    retryCount.increment();
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
     * Least-connections needs live data: {@code SlaveInfo} is built once at startup,
     * so the count is refreshed from the counting datasource wrapper on every selection.
     */
    private SlaveLoadBalancer.SlaveInfo withLiveActiveConnections(SlaveLoadBalancer.SlaveInfo info) {
        DataSource dataSource = slaveDataSourceMap.get(info.name());
        if (dataSource instanceof com.scmcloud.common.data.rw.dynamic.CountingDataSource counting) {
            return new SlaveLoadBalancer.SlaveInfo(info.name(), info.weight(),
                    counting.getActiveConnections(), info.available());
        }
        return info;
    }

    /**
     * Check slave connection validity (with cache).
     * <p>
     * A failed probe marks the slave unavailable immediately (when
     * {@code fail-fast-on-slave-connection-failure} is true) so the next request
     * falls back to master instead of repeatedly hitting a dead replica. The
     * periodic {@code SlaveHealthChecker} restores the slave.
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
            boolean valid = connection.isValid(properties.getValidationTimeoutSeconds());
            lastValidationTime.put(slaveName, System.currentTimeMillis());
            if (!valid && properties.isFailFastOnSlaveConnectionFailure()) {
                markSlaveUnavailable(slaveName);
            }
            return valid;
        } catch (Exception e) {
            lastValidationTime.put(slaveName, System.currentTimeMillis());
            if (properties.isFailFastOnSlaveConnectionFailure()) {
                markSlaveUnavailable(slaveName);
            }
            log.debug("[RW-Routing] Group [{}] slave [{}] connection check failed: {}",
                    groupName, slaveName, e.getMessage());
            return false;
        }
    }

    public void markSlaveUnavailable(String slaveName) {
        Boolean previous = slaveAvailability.put(slaveName, false);
        if (previous == null || previous) {
            log.error("[RW-Routing] Group [{}] slave [{}] marked as UNAVAILABLE, reads fall back to MASTER",
                    groupName, slaveName);
        }
    }

    public void markSlaveAvailable(String slaveName) {
        Boolean previous = slaveAvailability.put(slaveName, true);
        if (previous == null || !previous) {
            log.info("[RW-Routing] Group [{}] slave [{}] marked as AVAILABLE", groupName, slaveName);
        }
        lastValidationTime.remove(slaveName);
    }

    public Map<String, Boolean> getSlaveAvailability() {
        return Map.copyOf(slaveAvailability);
    }

    /**
     * Route counters since startup, also exported as {@code datasource.rw.*} metrics.
     */
    public Map<String, Long> getRouteCounts() {
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("master", masterRouteCount.sum());
        counts.put("slave", slaveRouteCount.sum());
        counts.put("fallback", fallbackCount.sum());
        counts.put("retry", retryCount.sum());
        return counts;
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
}

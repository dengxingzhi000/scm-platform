package com.scmcloud.common.data.rw.health;

import com.scmcloud.common.data.rw.config.ReadWriteProperties;
import com.scmcloud.common.data.rw.routing.ReadWriteRoutingDataSource;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Slave health checker.
 * <p>
 * Features:
 * - Periodic slave connection check
 * - Replication lag detection (PostgreSQL, MySQL)
 * - Auto remove/recover unavailable nodes
 *
 * @author Deng
 * @since 2025-12-16
 */
public class SlaveHealthChecker {
    private static final Logger log = LoggerFactory.getLogger(SlaveHealthChecker.class);
    private final Map<String, ReadWriteRoutingDataSource> routingDataSources;
    private final Map<String, Map<String, DataSource>> slaveDataSources;
    private final ReadWriteProperties properties;
    private final List<ReplicationLagChecker> lagCheckers;

    private final Map<String, AtomicInteger> failureCounters = new ConcurrentHashMap<>();
    private final Map<String, Long> replicationLags = new ConcurrentHashMap<>();

    public SlaveHealthChecker(Map<String, ReadWriteRoutingDataSource> routingDataSources,
                               Map<String, Map<String, DataSource>> slaveDataSources,
                               ReadWriteProperties properties,
                               MeterRegistry meterRegistry) {
        this(routingDataSources, slaveDataSources, properties, meterRegistry, null);
    }

    public SlaveHealthChecker(Map<String, ReadWriteRoutingDataSource> routingDataSources,
                               Map<String, Map<String, DataSource>> slaveDataSources,
                               ReadWriteProperties properties,
                               MeterRegistry meterRegistry,
                               List<ReplicationLagChecker> customLagCheckers) {
        this.routingDataSources = routingDataSources;
        this.slaveDataSources = slaveDataSources;
        this.properties = properties;

        this.lagCheckers = new ArrayList<>();
        if (customLagCheckers != null) {
            this.lagCheckers.addAll(customLagCheckers);
        }
        this.lagCheckers.add(new PostgresqlReplicationLagChecker());
        this.lagCheckers.add(new MysqlReplicationLagChecker());

        if (meterRegistry != null) {
            registerMetrics(meterRegistry);
        }
    }

    private void registerMetrics(MeterRegistry meterRegistry) {
        for (String groupName : slaveDataSources.keySet()) {
            for (String slaveName : slaveDataSources.get(groupName).keySet()) {
                String fullName = groupName + "." + slaveName;

                Gauge.builder("datasource.rw.replication.lag", replicationLags,
                                map -> map.getOrDefault(fullName, 0L).doubleValue())
                        .tag("group", groupName)
                        .tag("slave", slaveName)
                        .description("Replication lag in milliseconds")
                        .register(meterRegistry);
            }
        }
    }

    @Scheduled(fixedDelayString = "${spring.datasource.rw.health-check-interval:30000}")
    public void healthCheck() {
        if (!properties.isHealthCheckEnabled()) {
            return;
        }

        log.debug("[Health] Starting slave health check...");

        for (Map.Entry<String, Map<String, DataSource>> groupEntry : slaveDataSources.entrySet()) {
            String groupName = groupEntry.getKey();
            Map<String, DataSource> slaves = groupEntry.getValue();

            for (Map.Entry<String, DataSource> slaveEntry : slaves.entrySet()) {
                String slaveName = slaveEntry.getKey();
                DataSource dataSource = slaveEntry.getValue();

                checkSlave(groupName, slaveName, dataSource);
            }
        }
    }

    private void checkSlave(String groupName, String slaveName, DataSource dataSource) {
        String fullName = groupName + "." + slaveName;
        AtomicInteger failureCounter = failureCounters.computeIfAbsent(fullName,
                k -> new AtomicInteger(0));

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {

            if (!connection.isValid(5)) {
                handleFailure(groupName, slaveName, failureCounter, "Connection invalid");
                return;
            }

            // Get driver class name from config or metadata
            String driverClassName = getDriverClassName(groupName, slaveName);
            if (driverClassName == null) {
                driverClassName = connection.getMetaData().getDriverClassName();
            }
            Long lagMs = checkReplicationLag(statement, driverClassName);
            replicationLags.put(fullName, lagMs != null ? lagMs : 0L);

            if (lagMs != null && lagMs > properties.getReplicationLagTolerance().toMillis()) {
                log.warn("[Health] Slave [{}] replication lag too high: {}ms (threshold: {}ms)",
                        fullName, lagMs, properties.getReplicationLagTolerance().toMillis());
                handleFailure(groupName, slaveName, failureCounter, "Replication lag too high");
                return;
            }

            // Health check passed, reset counter
            if (failureCounter.get() > 0) {
                failureCounter.set(0);
                markSlaveAvailable(groupName, slaveName);
            }

            log.debug("[Health] Slave [{}] is healthy, lag: {}ms", fullName, lagMs);

        } catch (Exception e) {
            handleFailure(groupName, slaveName, failureCounter, e.getMessage());
        }
    }

    private Long checkReplicationLag(Statement statement, String driverClassName) {
        for (ReplicationLagChecker checker : lagCheckers) {
            if (checker.supports(driverClassName)) {
                return checker.checkReplicationLag(statement);
            }
        }
        log.trace("[Health] No replication lag checker found for driver: {}", driverClassName);
        return null;
    }

    private String getDriverClassName(String groupName, String slaveName) {
        ReadWriteProperties.DataSourceGroup group = properties.getGroups().get(groupName);
        if (group == null) {
            return null;
        }

        for (ReadWriteProperties.SlaveDataSourceConfig slave : group.getSlaves()) {
            if (slave.getName().equals(slaveName)) {
                return slave.getDriverClassName();
            }
        }

        if (group.getMaster() != null) {
            return group.getMaster().getDriverClassName();
        }

        return null;
    }

    private void handleFailure(String groupName, String slaveName,
                                AtomicInteger failureCounter, String reason) {
        int failures = failureCounter.incrementAndGet();
        String fullName = groupName + "." + slaveName;

        log.warn("[Health] Slave [{}] health check failed ({}): {}",
                fullName, failures, reason);

        if (failures >= properties.getFailureThreshold()) {
            markSlaveUnavailable(groupName, slaveName);
        }
    }

    private void markSlaveUnavailable(String groupName, String slaveName) {
        ReadWriteRoutingDataSource routingDataSource = routingDataSources.get(groupName);
        if (routingDataSource != null) {
            routingDataSource.markSlaveUnavailable(slaveName);
            log.error("[Health] Slave [{}] marked as UNAVAILABLE after {} consecutive failures",
                    groupName + "." + slaveName, properties.getFailureThreshold());
        }
    }

    private void markSlaveAvailable(String groupName, String slaveName) {
        ReadWriteRoutingDataSource routingDataSource = routingDataSources.get(groupName);
        if (routingDataSource != null) {
            routingDataSource.markSlaveAvailable(slaveName);
            log.info("[Health] Slave [{}] recovered and marked as AVAILABLE",
                    groupName + "." + slaveName);
        }
    }

    public Map<String, HealthStatus> getAllHealthStatus() {
        Map<String, HealthStatus> result = new ConcurrentHashMap<>();

        for (Map.Entry<String, ReadWriteRoutingDataSource> entry : routingDataSources.entrySet()) {
            String groupName = entry.getKey();
            Map<String, Boolean> availability = entry.getValue().getSlaveAvailability();

            for (Map.Entry<String, Boolean> slaveEntry : availability.entrySet()) {
                String fullName = groupName + "." + slaveEntry.getKey();
                Long lagMs = replicationLags.getOrDefault(fullName, 0L);

                result.put(fullName, new HealthStatus(
                        slaveEntry.getValue(),
                        lagMs,
                        failureCounters.getOrDefault(fullName, new AtomicInteger(0)).get()
                ));
            }
        }

        return result;
    }

    public record HealthStatus(
            boolean available,
            long replicationLagMs,
            int consecutiveFailures
    ) {}
}

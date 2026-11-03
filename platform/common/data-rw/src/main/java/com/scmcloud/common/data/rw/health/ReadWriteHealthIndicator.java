package com.scmcloud.common.data.rw.health;

import com.scmcloud.common.data.rw.config.ReadWriteAutoConfiguration;
import com.scmcloud.common.data.rw.config.ReadWriteProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;

import java.util.HashMap;
import java.util.Map;

/**
 * Read-write separation health indicator.
 * <p>
 * Used for Spring Boot Actuator health endpoint.
 * <p>
 * A down slave does not make the service unhealthy by default: reads fall back to
 * master, so reporting {@code DOWN} here would make Kubernetes restart a perfectly
 * working pod. Set {@code spring.datasource.rw.slave-down-affects-health=true} to
 * keep the old fail-hard behaviour.
 *
 * @author Deng
 * @since 2025-12-16
 */
@RequiredArgsConstructor
public class ReadWriteHealthIndicator implements HealthIndicator {
    private final SlaveHealthChecker healthChecker;
    private final ReadWriteAutoConfiguration.ReadWriteDataSourceProvider dataSourceProvider;
    private final ReadWriteProperties properties;

    @Override
    public Health health() {
        Map<String, SlaveHealthChecker.HealthStatus> allStatus = healthChecker.getAllHealthStatus();

        if (allStatus.isEmpty()) {
            return Health.unknown()
                    .withDetail("message", "No slave datasources configured; reads route to master")
                    .build();
        }

        Map<String, Object> details = new HashMap<>();
        boolean anyUnavailable = false;
        long maxLag = 0;

        for (Map.Entry<String, SlaveHealthChecker.HealthStatus> entry : allStatus.entrySet()) {
            SlaveHealthChecker.HealthStatus status = entry.getValue();

            Map<String, Object> slaveDetails = new HashMap<>();
            slaveDetails.put("available", status.available());
            slaveDetails.put("replicationLagMs", status.replicationLagMs());
            slaveDetails.put("consecutiveFailures", status.consecutiveFailures());

            details.put(entry.getKey(), slaveDetails);

            if (!status.available()) {
                anyUnavailable = true;
            }
            maxLag = Math.max(maxLag, status.replicationLagMs());
        }

        details.put("groups", dataSourceProvider.getGroupNames());
        details.put("maxReplicationLagMs", maxLag);

        if (anyUnavailable && properties.isSlaveDownAffectsHealth()) {
            return Health.down()
                    .withDetails(details)
                    .build();
        }

        if (anyUnavailable) {
            details.put("degraded", true);
            details.put("reason", "At least one slave is unavailable; reads fall back to master");
        }

        return Health.up()
                .withDetails(details)
                .build();
    }
}

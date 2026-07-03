package com.scmcloud.common.data.rw.endpoint;

import com.scmcloud.common.data.rw.config.ReadWriteAutoConfiguration;
import com.scmcloud.common.data.rw.health.SlaveHealthChecker;
import com.scmcloud.common.data.rw.loadbalance.*;
import com.scmcloud.common.data.rw.routing.ReadWriteRoutingDataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Read-write separation management endpoint.
 * <p>
 * Provides runtime management capabilities:
 * - View slave status
 * - Manually remove/restore slaves
 * - View health information
 * - Switch load balance strategy
 * <p>
 * Access path: /actuator/readwrite
 * <p>
 * Note: Requires spring-boot-starter-actuator dependency
 *
 * @author Deng
 * @since 2025-12-16
 */
@Slf4j
@Endpoint(id = "readwrite")
@RequiredArgsConstructor
public class ReadWriteEndpoint {
    private final ReadWriteAutoConfiguration.ReadWriteDataSourceProvider dataSourceProvider;
    private final SlaveHealthChecker healthChecker;

    /**
     * Get all read-write separation status.
     * <p>
     * GET /actuator/readwrite
     */
    @ReadOperation
    public Map<String, Object> status() {
        Map<String, Object> result = new HashMap<>();

        Set<String> groups = dataSourceProvider.getGroupNames();
        result.put("groups", groups);

        Map<String, Object> groupDetails = new HashMap<>();
        for (String groupName : groups) {
            groupDetails.put(groupName, buildGroupInfo(groupName));
        }

        result.put("details", groupDetails);
        return result;
    }

    /**
     * Get status of specified group.
     * <p>
     * GET /actuator/readwrite/{groupName}
     */
    @ReadOperation
    public Map<String, Object> statusByGroup(@Selector String groupName) {
        Map<String, Object> result = new HashMap<>();

        try {
            result.put("group", groupName);
            result.putAll(buildGroupInfo(groupName));
        } catch (IllegalArgumentException e) {
            result.put("error", e.getMessage());
        }

        return result;
    }

    /**
     * Manually remove slave / switch load balance strategy.
     * <p>
     * POST /actuator/readwrite with {"groupName": "xxx", "slaveName": "xxx", "action": "markUnavailable"}
     * POST /actuator/readwrite with {"groupName": "xxx", "action": "switchLoadBalancer", "strategy": "ROUND_ROBIN"}
     */
    @WriteOperation
    public Map<String, Object> operate(String groupName, String slaveName, String action, String strategy) {
        if ("markAvailable".equalsIgnoreCase(action)) {
            return doMarkAvailable(groupName, slaveName);
        } else if ("markUnavailable".equalsIgnoreCase(action)) {
            return doMarkUnavailable(groupName, slaveName);
        } else if ("switchLoadBalancer".equalsIgnoreCase(action)) {
            return doSwitchLoadBalancer(groupName, strategy);
        } else {
            return Map.of("success", false, "error", "Unknown action: " + action);
        }
    }

    private Map<String, Object> buildGroupInfo(String groupName) {
        Map<String, Object> groupInfo = new HashMap<>();

        ReadWriteRoutingDataSource ds = dataSourceProvider.getDataSource(groupName);
        groupInfo.put("slaveAvailability", ds.getSlaveAvailability());
        groupInfo.put("healthStatus", buildHealthDetails(groupName));
        groupInfo.put("loadBalancer", ds.getCurrentLoadBalancerName());

        return groupInfo;
    }

    private Map<String, Object> buildHealthDetails(String groupName) {
        Map<String, Object> healthDetails = new HashMap<>();
        Map<String, SlaveHealthChecker.HealthStatus> allStatus = healthChecker.getAllHealthStatus();

        String prefix = groupName + ".";
        for (Map.Entry<String, SlaveHealthChecker.HealthStatus> entry : allStatus.entrySet()) {
            if (entry.getKey().startsWith(prefix)) {
                String slaveName = entry.getKey().substring(prefix.length());
                SlaveHealthChecker.HealthStatus status = entry.getValue();
                healthDetails.put(slaveName, Map.of(
                        "available", status.available(),
                        "replicationLagMs", status.replicationLagMs(),
                        "consecutiveFailures", status.consecutiveFailures()
                ));
            }
        }

        return healthDetails;
    }

    private Map<String, Object> doMarkUnavailable(String groupName, String slaveName) {
        Map<String, Object> result = new HashMap<>();

        try {
            ReadWriteRoutingDataSource ds = dataSourceProvider.getDataSource(groupName);
            ds.markSlaveUnavailable(slaveName);

            log.warn("[RW-Endpoint] Manually marked slave [{}] in group [{}] as UNAVAILABLE",
                    slaveName, groupName);

            result.put("success", true);
            result.put("message", String.format("Slave [%s.%s] marked as unavailable", groupName, slaveName));
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
        }

        return result;
    }

    private Map<String, Object> doMarkAvailable(String groupName, String slaveName) {
        Map<String, Object> result = new HashMap<>();

        try {
            ReadWriteRoutingDataSource ds = dataSourceProvider.getDataSource(groupName);
            ds.markSlaveAvailable(slaveName);

            log.info("[RW-Endpoint] Manually marked slave [{}] in group [{}] as AVAILABLE",
                    slaveName, groupName);

            result.put("success", true);
            result.put("message", String.format("Slave [%s.%s] marked as available", groupName, slaveName));
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
        }

        return result;
    }

    private Map<String, Object> doSwitchLoadBalancer(String groupName, String strategy) {
        Map<String, Object> result = new HashMap<>();

        try {
            ReadWriteRoutingDataSource ds = dataSourceProvider.getDataSource(groupName);
            SlaveLoadBalancer newLoadBalancer = createLoadBalancer(strategy);
            ds.switchLoadBalancer(newLoadBalancer);

            log.info("[RW-Endpoint] Switched load balancer for group [{}] to [{}]", groupName, strategy);

            result.put("success", true);
            result.put("message", String.format("Load balancer switched to %s", strategy));
            result.put("previousStrategy", ds.getCurrentLoadBalancerName());
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
        }

        return result;
    }

    private SlaveLoadBalancer createLoadBalancer(String strategy) {
        return SlaveLoadBalancer.create(strategy);
    }
}

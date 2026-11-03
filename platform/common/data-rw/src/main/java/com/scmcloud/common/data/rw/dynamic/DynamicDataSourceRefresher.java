package com.scmcloud.common.data.rw.dynamic;

import com.scmcloud.common.data.rw.config.ReadWriteProperties;
import com.scmcloud.common.data.rw.routing.ReadWriteRoutingDataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;

import java.util.Map;

/**
 * Dynamic datasource refresher.
 * <p>
 * Listens for Nacos config changes and dynamically adjusts:
 * - Slave weights
 * - Slave enable/disable
 * - Load balance strategy
 * - Health check parameters
 *
 * @author Deng
 * @since 2025-12-16
 */
@Slf4j
@RequiredArgsConstructor
public class DynamicDataSourceRefresher {
    private static final String REFRESH_EVENT_CLASS = "org.springframework.cloud.context.scope.refresh.RefreshScopeRefreshedEvent";
    private static final String ENV_CHANGE_EVENT_CLASS = "org.springframework.cloud.context.environment.EnvironmentChangeEvent";

    private final ReadWriteProperties properties;
    private final Map<String, ReadWriteRoutingDataSource> routingDataSources;
    private final Environment environment;

    @EventListener
    public void onApplicationEvent(ApplicationEvent event) {
        String eventClassName = event.getClass().getName();
        if (!REFRESH_EVENT_CLASS.equals(eventClassName) && !ENV_CHANGE_EVENT_CLASS.equals(eventClassName)) {
            return;
        }

        log.debug("[Dynamic-DS] Configuration refresh detected ({}), reloading datasource settings...",
                event.getClass().getSimpleName());
        doRefresh();
    }

    private void rebindProperties() {
        try {
            ReadWriteProperties newProperties = Binder.get(environment)
                    .bind("spring.datasource.rw", ReadWriteProperties.class)
                    .orElse(null);

            if (newProperties != null) {
                properties.setReadMasterAfterWrite(newProperties.getReadMasterAfterWrite());
                properties.setReplicationLagTolerance(newProperties.getReplicationLagTolerance());
                properties.setHealthCheckEnabled(newProperties.isHealthCheckEnabled());
                properties.setFailureThreshold(newProperties.getFailureThreshold());

                for (var entry : newProperties.getGroups().entrySet()) {
                    var existingGroup = properties.getGroups().get(entry.getKey());
                    if (existingGroup != null) {
                        existingGroup.setSlavesEnabled(entry.getValue().isSlavesEnabled());
                        existingGroup.setLoadBalance(entry.getValue().getLoadBalance());

                        for (var newSlave : entry.getValue().getSlaves()) {
                            for (var existingSlave : existingGroup.getSlaves()) {
                                if (existingSlave.getName().equals(newSlave.getName())) {
                                    existingSlave.setWeight(newSlave.getWeight());
                                    existingSlave.setAvailable(newSlave.isAvailable());
                                }
                            }
                        }
                    }
                }

                log.debug("[Dynamic-DS] Properties rebound successfully");
            }
        } catch (Exception e) {
            log.warn("[Dynamic-DS] Failed to rebind properties: {}", e.getMessage());
        }
    }

    private void refreshSlaveSettings() {
        for (Map.Entry<String, ReadWriteProperties.DataSourceGroup> groupEntry : properties.getGroups().entrySet()) {

            String groupName = groupEntry.getKey();
            ReadWriteProperties.DataSourceGroup group = groupEntry.getValue();
            ReadWriteRoutingDataSource routingDs = routingDataSources.get(groupName);

            if (routingDs == null) {
                log.warn("[Dynamic-DS] Group [{}] not found in routing datasources", groupName);
                continue;
            }

            if (!group.isSlavesEnabled()) {
                log.debug("[Dynamic-DS] Group [{}] slaves disabled, marking all as unavailable", groupName);
                for (var slave : group.getSlaves()) {
                    routingDs.markSlaveUnavailable(slave.getName());
                }
            } else {
                for (var slave : group.getSlaves()) {
                    if (slave.isAvailable()) {
                        routingDs.markSlaveAvailable(slave.getName());
                    } else {
                        routingDs.markSlaveUnavailable(slave.getName());
                    }
                    log.debug("[Dynamic-DS] Group [{}] slave [{}] weight={}, available={}",
                            groupName, slave.getName(), slave.getWeight(), slave.isAvailable());
                }
            }
        }

        log.debug("[Dynamic-DS] Global settings: readMasterAfterWrite={}, replicationLagTolerance={}",
                properties.getReadMasterAfterWrite(), properties.getReplicationLagTolerance());
    }

    public void forceRefresh() {
        log.debug("[Dynamic-DS] Force refresh triggered");
        doRefresh();
    }

    private void doRefresh() {
        try {
            rebindProperties();
            refreshSlaveSettings();
            log.debug("[Dynamic-DS] Datasource settings refreshed successfully");
        } catch (Exception e) {
            log.error("[Dynamic-DS] Failed to refresh datasource settings", e);
        }
    }

    public Map<String, Object> getConfigSnapshot() {
        return Map.of(
                "enabled", properties.isEnabled(),
                "loadBalance", properties.getLoadBalance(),
                "readMasterAfterWrite", properties.getReadMasterAfterWrite().toString(),
                "replicationLagTolerance", properties.getReplicationLagTolerance().toString(),
                "healthCheckEnabled", properties.isHealthCheckEnabled(),
                "failureThreshold", properties.getFailureThreshold(),
                "groups", properties.getGroups().keySet()
        );
    }
}

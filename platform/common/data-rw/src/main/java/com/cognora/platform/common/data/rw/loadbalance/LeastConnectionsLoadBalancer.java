package com.cognora.platform.common.data.rw.loadbalance;

import java.util.Comparator;
import java.util.List;

/**
 * Least connections load balancer.
 * <p>
 * The {@code activeConnections} field is filled by the routing datasource from
 * {@link com.cognora.platform.common.data.rw.dynamic.CountingDataSource}, which counts
 * connections currently held by callers (incremented on acquire, decremented on
 * close and on failed acquire). Ties keep the first slave in the configured order.
 *
 * @author Deng
 * @since 2025-12-16
 */
public class LeastConnectionsLoadBalancer extends AbstractLoadBalancer {

    @Override
    protected String doSelect(List<SlaveInfo> available) {
        return available.stream()
                .min(Comparator.comparingInt(SlaveInfo::activeConnections))
                .map(SlaveInfo::name)
                .orElseGet(() -> getFirstName(available));
    }
}

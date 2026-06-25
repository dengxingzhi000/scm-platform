package com.scmcloud.common.data.rw.loadbalance;

import java.util.List;

/**
 * Slave load balancer interface.
 *
 * @author Deng
 * @since 2025-12-16
 */
public interface SlaveLoadBalancer {

    /**
     * Select a slave.
     *
     * @param slaves available slave list
     * @return selected slave name
     */
    String select(List<SlaveInfo> slaves);

    /**
     * Slave info.
     */
    record SlaveInfo(
            String name,
            int weight,
            int activeConnections,
            boolean available
    ) {}
}

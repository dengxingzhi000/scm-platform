package com.scmcloud.common.data.rw.loadbalance;

import com.scmcloud.common.data.rw.config.ReadWriteProperties;

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

    static SlaveLoadBalancer create(ReadWriteProperties.LoadBalanceType type) {
        return switch (type) {
            case ROUND_ROBIN -> new RoundRobinLoadBalancer();
            case WEIGHTED_ROUND_ROBIN -> new WeightedRoundRobinLoadBalancer();
            case RANDOM -> new RandomLoadBalancer();
            case WEIGHTED_RANDOM -> new WeightedRandomLoadBalancer();
            case LEAST_CONNECTIONS -> new LeastConnectionsLoadBalancer();
        };
    }

    static SlaveLoadBalancer create(String strategy) {
        if (strategy == null || strategy.isBlank()) {
            throw new IllegalArgumentException("Load balance strategy must not be blank");
        }
        return switch (strategy.toUpperCase()) {
            case "ROUND_ROBIN" -> new RoundRobinLoadBalancer();
            case "WEIGHTED_ROUND_ROBIN" -> new WeightedRoundRobinLoadBalancer();
            case "RANDOM" -> new RandomLoadBalancer();
            case "WEIGHTED_RANDOM" -> new WeightedRandomLoadBalancer();
            case "LEAST_CONNECTIONS" -> new LeastConnectionsLoadBalancer();
            default -> throw new IllegalArgumentException("Unknown load balance strategy: " + strategy);
        };
    }

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

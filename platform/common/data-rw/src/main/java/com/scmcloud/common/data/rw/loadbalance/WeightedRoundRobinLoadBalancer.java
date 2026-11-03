package com.scmcloud.common.data.rw.loadbalance;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Weighted round-robin load balancer.
 * <p>
 * Implements smooth weighted round-robin algorithm (same as Nginx).
 * Uses ConcurrentHashMap + AtomicInteger for thread safety.
 *
 * @author Deng
 * @since 2025-12-16
 */
public class WeightedRoundRobinLoadBalancer extends AbstractLoadBalancer {

    private final Map<String, AtomicInteger> currentWeights = new ConcurrentHashMap<>();

    @Override
    protected String doSelect(List<SlaveInfo> available) {
        int totalWeight = available.stream()
                .mapToInt(SlaveInfo::weight)
                .sum();

        SlaveInfo selected = null;
        int maxCurrentWeight = Integer.MIN_VALUE;

        for (SlaveInfo slave : available) {
            AtomicInteger weightAtomic = currentWeights.computeIfAbsent(
                    slave.name(), k -> new AtomicInteger(0));

            int current = weightAtomic.addAndGet(slave.weight());

            if (current > maxCurrentWeight) {
                maxCurrentWeight = current;
                selected = slave;
            }
        }

        if (selected != null) {
            AtomicInteger selectedWeight = currentWeights.get(selected.name());
            if (selectedWeight != null) {
                selectedWeight.addAndGet(-totalWeight);
            }
            return selected.name();
        }

        return getFirstName(available);
    }
}

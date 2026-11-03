package com.scmcloud.common.data.rw.loadbalance;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Weighted random load balancer.
 *
 * @author Deng
 * @since 2025-12-16
 */
public class WeightedRandomLoadBalancer extends AbstractLoadBalancer {

    @Override
    protected String doSelect(List<SlaveInfo> available) {
        int totalWeight = available.stream()
                .mapToInt(SlaveInfo::weight)
                .sum();

        int random = ThreadLocalRandom.current().nextInt(totalWeight);

        int sum = 0;
        for (SlaveInfo slave : available) {
            sum += slave.weight();
            if (random < sum) {
                return slave.name();
            }
        }

        return getFirstName(available);
    }
}

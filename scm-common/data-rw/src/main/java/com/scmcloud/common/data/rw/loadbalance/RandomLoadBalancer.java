package com.scmcloud.common.data.rw.loadbalance;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Random load balancer.
 *
 * @author Deng
 * @since 2025-12-16
 */
public class RandomLoadBalancer extends AbstractLoadBalancer {

    @Override
    protected String doSelect(List<SlaveInfo> available) {
        int index = ThreadLocalRandom.current().nextInt(available.size());
        return available.get(index).name();
    }
}

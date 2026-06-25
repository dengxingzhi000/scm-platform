package com.scmcloud.common.data.rw.loadbalance;

import lombok.Setter;

import java.util.List;

/**
 * Load balancer abstract base class.
 * <p>
 * Provides common null check and available node filtering logic.
 * Supports slave warmup mechanism.
 *
 * @author Deng
 * @since 2025-12-16
 */
public abstract class AbstractLoadBalancer implements SlaveLoadBalancer {

    @Setter
    protected SlaveWarmupManager warmupManager;

    @Override
    public final String select(List<SlaveInfo> slaves) {
        if (slaves == null || slaves.isEmpty()) {
            return null;
        }

        List<SlaveInfo> available = slaves.stream()
                .filter(SlaveInfo::available)
                .toList();

        if (available.isEmpty()) {
            return null;
        }

        if (warmupManager != null && hasWarmingUpSlaves(available)) {
            available = applyWarmupWeight(available);
        }

        return doSelect(available);
    }

    private boolean hasWarmingUpSlaves(List<SlaveInfo> slaves) {
        for (SlaveInfo slave : slaves) {
            if (warmupManager.isWarmingUp(slave.name())) {
                return true;
            }
        }
        return false;
    }

    private List<SlaveInfo> applyWarmupWeight(List<SlaveInfo> slaves) {
        return slaves.stream()
                .map(slave -> {
                    int effectiveWeight = warmupManager.getEffectiveWeight(slave.name(), slave.weight());
                    if (effectiveWeight != slave.weight()) {
                        return new SlaveInfo(slave.name(), effectiveWeight,
                                slave.activeConnections(), slave.available());
                    }
                    return slave;
                })
                .toList();
    }

    protected abstract String doSelect(List<SlaveInfo> available);

    protected String getFirstName(List<SlaveInfo> available) {
        return available.getFirst().name();
    }
}

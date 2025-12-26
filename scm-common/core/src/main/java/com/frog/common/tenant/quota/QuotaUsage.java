package com.frog.common.tenant.quota;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 配额使用情况
 *
 * @author Claude Code
 * @since 2025-01-24
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuotaUsage {

    /**
     * 配额类型
     */
    private QuotaType quotaType;

    /**
     * 当前使用量
     */
    private int currentUsage;

    /**
     * 最大限额
     */
    private int maxQuota;

    /**
     * 可用配额
     */
    private int availableQuota;

    /**
     * 使用率（百分比）
     */
    private double usagePercent;

    /**
     * 是否已超限
     */
    private boolean exceeded;

    /**
     * 计算可用配额
     */
    public void calculateAvailable() {
        this.availableQuota = Math.max(0, maxQuota - currentUsage);
        this.usagePercent = maxQuota > 0 ? (double) currentUsage / maxQuota * 100 : 0;
        this.exceeded = currentUsage >= maxQuota;
    }
}
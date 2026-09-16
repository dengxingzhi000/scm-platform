package com.scmcloud.common.tenant.quota;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 閰嶉浣跨敤鎯呭喌
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
     * 閰嶉绫诲瀷
     */
    private QuotaType quotaType;

    /**
     * 褰撳墠浣跨敤锟?
     */
    private int currentUsage;

    /**
     * 鏈€澶ч檺锟?
     */
    private int maxQuota;

    /**
     * 鍙敤閰嶉
     */
    private int availableQuota;

    /**
     * 浣跨敤鐜囷紙鐧惧垎姣旓級
     */
    private double usagePercent;

    /**
     * 鏄惁宸茶秴锟?
     */
    private boolean exceeded;

    /**
     * 璁$畻鍙敤閰嶉
     */
    public void calculateAvailable() {
        this.availableQuota = Math.max(0, maxQuota - currentUsage);
        this.usagePercent = maxQuota > 0 ? (double) currentUsage / maxQuota * 100 : 0;
        this.exceeded = currentUsage >= maxQuota;
    }
}
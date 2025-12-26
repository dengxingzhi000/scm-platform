package com.frog.common.tenant.quota;

import java.util.UUID;

/**
 * 配额服务接口
 *
 * @author Claude Code
 * @since 2025-01-24
 */
public interface QuotaService {

    /**
     * 检查配额是否充足
     *
     * @param tenantId 租户ID
     * @param quotaType 配额类型
     * @param increment 需要消耗的配额数量
     * @return true=配额充足, false=配额不足
     */
    boolean checkQuota(UUID tenantId, QuotaType quotaType, int increment);

    /**
     * 检查并消耗配额（原子操作）
     *
     * @param tenantId 租户ID
     * @param quotaType 配额类型
     * @param increment 需要消耗的配额数量
     * @return true=检查通过且已消耗, false=配额不足
     */
    boolean checkAndConsumeQuota(UUID tenantId, QuotaType quotaType, int increment);

    /**
     * 释放配额（回滚操作）
     *
     * @param tenantId 租户ID
     * @param quotaType 配额类型
     * @param decrement 释放的配额数量
     */
    void releaseQuota(UUID tenantId, QuotaType quotaType, int decrement);

    /**
     * 获取配额使用情况
     *
     * @param tenantId 租户ID
     * @param quotaType 配额类型
     * @return 配额使用情况
     */
    QuotaUsage getQuotaUsage(UUID tenantId, QuotaType quotaType);

    /**
     * 重置每日配额（订单、API调用）
     * 由定时任务每日凌晨调用
     *
     * @param tenantId 租户ID（null表示重置所有租户）
     */
    void resetDailyQuota(UUID tenantId);
}
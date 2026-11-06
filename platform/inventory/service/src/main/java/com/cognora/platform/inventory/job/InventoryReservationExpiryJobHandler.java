package com.cognora.platform.inventory.job;

import com.cognora.platform.inventory.service.IInvReservationService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 过期预占释放任务
 *
 * <p>扫描 {@code inv_reservation_record} 中已过期但仍是 RESERVED 的记录，
 * 按状态机把记录推进到 EXPIRED，并把锁定库存归还可用量。
 *
 * <p>之所以必须有这个任务：旧实现的预占只存在 Redis，TTL 到期后记录消失，
 * locked_stock 永久泄漏；{@code /reservation/release-expired} 接口是空实现。
 *
 * <p>建议执行频率: 每分钟一次（cron: 0 * * * * ?）
 *
 * @author SCM Platform Team
 * @since 2026-11-04
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryReservationExpiryJobHandler {

    private final IInvReservationService reservationService;

    @XxlJob("inventoryReservationExpiryJobHandler")
    public void execute() {
        long startTime = System.currentTimeMillis();
        XxlJobHelper.log("[库存预占] 开始扫描过期预占");

        try {
            int released = reservationService.releaseExpiredReservations();
            long duration = System.currentTimeMillis() - startTime;
            XxlJobHelper.log("[库存预占] 释放完成: released={}, 耗时={}ms", released, duration);
            XxlJobHelper.handleSuccess(
                    String.format("释放过期预占 %d 条, 耗时=%dms", released, duration));
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            XxlJobHelper.log("[库存预占] 任务异常: error={}, 耗时={}ms", e.getMessage(), duration);
            log.error("过期预占释放任务执行失败", e);
            XxlJobHelper.handleFail("任务执行失败: " + e.getMessage());
        }
    }
}

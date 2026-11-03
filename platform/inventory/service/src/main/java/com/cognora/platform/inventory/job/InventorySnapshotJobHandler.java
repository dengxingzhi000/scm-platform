package com.cognora.platform.inventory.job;

import com.cognora.platform.inventory.dto.InvSnapshotGenerationResult;
import com.cognora.platform.inventory.service.command.InvSnapshotCommandService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * 库存快照定时同步任务
 *
 * <p>定时生成库存快照，用于数据分析和审计
 *
 * <p>执行频率: 每天凌晨 1 点执行
 *
 * @author SCM Platform Team
 * @since 2025-12-26
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InventorySnapshotJobHandler {
    private final InvSnapshotCommandService snapshotCommandService;

    /**
     * 库存快照同步任务
     */
    @XxlJob("inventorySnapshotJobHandler")
    public void execute() {
        long startTime = System.currentTimeMillis();
        XxlJobHelper.log("📸 [库存快照] 开始执行任务");

        try {
            LocalDate snapshotDate = LocalDate.now();
            InvSnapshotGenerationResult result = snapshotCommandService.generateDailySnapshots(snapshotDate);

            long duration = System.currentTimeMillis() - startTime;
            XxlJobHelper.log("🎉 [库存快照] 任务完成: 日期={}, 总数={}, 成功={}, 跳过={}, 失败={}, 耗时={}ms",
                    snapshotDate, result.getScannedInventories(), result.getCreated(),
                    result.getSkipped(), result.getFailed(), duration);

            if (result.getFailed() > 0) {
                XxlJobHelper.handleFail(String.format("部分快照生成失败: 总数=%d, 成功=%d, 失败=%d",
                        result.getScannedInventories(), result.getCreated(), result.getFailed()));
            } else {
                XxlJobHelper.handleSuccess(String.format("快照生成完成 成功=%d, 跳过=%d, 耗时=%dms",
                        result.getCreated(), result.getSkipped(), duration));
            }
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            XxlJobHelper.log("❌[库存快照] 任务异常: error={}, 耗时={}ms",
                    e.getMessage(), duration);
            log.error("库存快照任务执行失败", e);
            XxlJobHelper.handleFail("任务执行失败: " + e.getMessage());
        }
    }
}

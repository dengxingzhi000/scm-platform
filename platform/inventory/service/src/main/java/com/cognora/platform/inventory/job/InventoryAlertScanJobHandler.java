package com.cognora.platform.inventory.job;

import com.cognora.platform.inventory.dto.InvAlertScanResult;
import com.cognora.platform.inventory.service.command.InvAlertCommandService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 库存告警扫描任务
 *
 * <p>按可用库存与安全库存生成、刷新或自动解除库存水位告警。
 *
 * <p>建议执行频率: 每小时一次（cron: 0 0 * * * ?）
 *
 * @author SCM Platform Team
 * @since 2026-10-29
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryAlertScanJobHandler {
    private final InvAlertCommandService alertCommandService;

    @XxlJob("inventoryAlertScanJobHandler")
    public void execute() {
        long startTime = System.currentTimeMillis();
        XxlJobHelper.log("🔔 [库存告警] 开始执行扫描任务");

        try {
            InvAlertScanResult result = alertCommandService.scanAndGenerate();
            long duration = System.currentTimeMillis() - startTime;
            XxlJobHelper.log("🎉 [库存告警] 扫描完成: 库存={}, 新建={}, 刷新={}, 解除={}, 耗时={}ms",
                    result.getScannedInventories(), result.getCreated(), result.getUpdated(),
                    result.getResolved(), duration);
            XxlJobHelper.handleSuccess(String.format("扫描完成 新建=%d, 刷新=%d, 解除=%d, 耗时=%dms",
                    result.getCreated(), result.getUpdated(), result.getResolved(), duration));
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            XxlJobHelper.log("❌[库存告警] 任务异常: error={}, 耗时={}ms", e.getMessage(), duration);
            log.error("库存告警扫描任务执行失败", e);
            XxlJobHelper.handleFail("任务执行失败: " + e.getMessage());
        }
    }
}

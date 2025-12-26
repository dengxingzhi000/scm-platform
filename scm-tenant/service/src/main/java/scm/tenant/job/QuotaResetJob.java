package scm.tenant.job;

import com.frog.common.tenant.quota.QuotaService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 租户配额重置定时任务
 *
 * 执行时间：每日凌晨 00:05（cron: 0 5 0 * * ?）
 *
 * 功能：
 * 1. 重置所有租户的每日配额计数器
 * 2. 重置字段：current_orders_today = 0, current_api_calls_today = 0
 * 3. 支持参数化重置单个租户（用于手动触发）
 *
 * XXL-Job 配置示例：
 * - 执行器：scm-tenant-executor
 * - JobHandler：quotaResetJob
 * - Cron：0 5 0 * * ?
 * - 运行模式：BEAN
 * - 阻塞处理策略：单机串行
 * - 路由策略：轮询
 *
 * @author Claude Code
 * @since 2025-01-24
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QuotaResetJob {

    private final QuotaService quotaService;

    /**
     * 执行每日配额重置
     *
     * 任务参数（可选）：
     * - tenantId: 指定租户ID（UUID格式），不传则重置所有租户
     *
     * 示例：
     * - 重置所有租户：不传参数
     * - 重置单个租户：传参 "123e4567-e89b-12d3-a456-426614174000"
     */
    @XxlJob("quotaResetJob")
    public void execute() {
        long startTime = System.currentTimeMillis();
        String param = XxlJobHelper.getJobParam();

        try {
            UUID tenantId = null;

            // 如果有参数，解析租户ID
            if (param != null && !param.trim().isEmpty()) {
                try {
                    tenantId = UUID.fromString(param.trim());
                    log.info("开始重置租户配额，租户ID: {}", tenantId);
                } catch (IllegalArgumentException e) {
                    String errorMsg = "无效的租户ID参数: " + param;
                    log.error(errorMsg, e);
                    XxlJobHelper.handleFail(errorMsg);
                    return;
                }
            } else {
                log.info("开始重置所有租户的每日配额");
            }

            // 执行配额重置
            quotaService.resetDailyQuota(tenantId);

            long duration = System.currentTimeMillis() - startTime;
            String successMsg = tenantId == null
                ? String.format("配额重置完成，耗时: %d ms", duration)
                : String.format("租户 %s 配额重置完成，耗时: %d ms", tenantId, duration);

            log.info(successMsg);
            XxlJobHelper.handleSuccess(successMsg);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            String errorMsg = String.format("配额重置失败，耗时: %d ms", duration);
            log.error(errorMsg, e);
            XxlJobHelper.handleFail(errorMsg + ": " + e.getMessage());
        }
    }
}
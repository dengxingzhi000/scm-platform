package com.scmcloud.tenant.job;

import com.scmcloud.tenant.service.IPlatformFeeService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 平台服务费计算定时任务
 *
 * 执行时间：每月 1 日凌晨 02:00（cron: 0 0 2 1 * ?）
 *
 * 功能：
 * 1. 计算上月所有租户的平台服务费
 * 2. 收费项目包括：
 *    - 订单佣金（按订单金额百分比）
 *    - 存储费用（按实际使用GB计费）
 *    - API调用费用（超出套餐部分按次计费）
 *    - 增值服务费（额外功能模块费用）
 * 3. 生成 platform_service_fee 记录
 * 4. 更新租户应收账款 (tenant_accounts_receivable)
 * 5. 发送账单通知邮件
 *
 * XXL-Job 配置示例：
 * - 执行器：scm-tenant-executor
 * - JobHandler：platformFeeCalculationJob
 * - Cron：0 0 2 1 * ?
 * - 运行模式：BEAN
 * - 阻塞处理策略：单机串行
 * - 路由策略：故障转移
 *
 * @author Claude Code
 * @since 2025-01-24
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PlatformFeeCalculationJob {

    private final IPlatformFeeService platformFeeService;

    /**
     * 执行平台服务费计算
     *
     * 任务参数（可选）：
     * - yearMonth: 指定计算月份（格式：yyyy-MM），不传则计算上月
     * - tenantId: 指定租户ID（UUID格式），不传则计算所有租户
     *
     * 示例：
     * - 计算上月所有租户：不传参数
     * - 计算指定月份：传入 "2025-01"
     * - 计算单个租户：传入 "tenantId=123e4567-e89b-12d3-a456-426614174000"
     * - 计算指定月份和租户：传参 "2025-01,tenantId=123e4567-e89b-12d3-a456-426614174000"
     */
    @XxlJob("platformFeeCalculationJob")
    public void execute() {
        long startTime = System.currentTimeMillis();
        String param = XxlJobHelper.getJobParam();

        try {
            // 解析参数
            YearMonth targetMonth = YearMonth.now().minusMonths(1); // 默认上月
            UUID tenantId = null;

            if (param != null && !param.trim().isEmpty()) {
                String[] parts = param.split(",");
                for (String part : parts) {
                    part = part.trim();
                    if (part.startsWith("tenantId=")) {
                        try {
                            tenantId = UUID.fromString(part.substring(9));
                        } catch (IllegalArgumentException e) {
                            String errorMsg = "Invalid tenant ID parameter: " + part;
                            log.error(errorMsg, e);
                            XxlJobHelper.handleFail(errorMsg);
                            return;
                        }
                    } else {
                        // 尝试解析为年月
                        try {
                            targetMonth = YearMonth.parse(part, DateTimeFormatter.ofPattern("yyyy-MM"));
                        } catch (Exception e) {
                            log.warn("Unable to parse month parameter: {}, using default", part);
                        }
                    }
                }
            }

            String scope = tenantId == null ? "All tenants" : "Tenant " + tenantId;
            log.info("Starting platform fee calculation for {} {}", targetMonth, scope);

            // 执行收费计算
            int calculatedCount = platformFeeService.calculateMonthlyFees(targetMonth, tenantId);

            long duration = System.currentTimeMillis() - startTime;
            String successMsg = String.format(
                "Platform fee calculation completed, month: %s, scope: %s, calculated tenants: %d, duration: %d ms",
                targetMonth,
                scope,
                calculatedCount,
                duration
            );

            log.info(successMsg);
            XxlJobHelper.handleSuccess(successMsg);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            String errorMsg = String.format("Platform fee calculation failed, duration: %d ms", duration);
            log.error(errorMsg, e);
            XxlJobHelper.handleFail(errorMsg + ": " + e.getMessage());
        }
    }
}
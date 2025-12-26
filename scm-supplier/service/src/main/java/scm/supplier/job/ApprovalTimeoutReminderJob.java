package scm.supplier.job;

import com.frog.supplier.service.PurchaseApprovalService;
import com.frog.supplier.service.NotificationService;
import com.frog.supplier.vo.OverdueApprovalTaskVO;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 采购审批超时提醒定时任务
 *
 * 执行时间：每日上午 10:00 和下午 15:00（cron: 0 0 10,15 * * ?）
 *
 * 功能：
 * 1. 扫描所有租户的待审批任务
 * 2. 超时标准：
 *    - 严重超时：超过审批时限 >= 2倍（例如：时限24小时，已过48小时）
 *    - 一般超时：超过审批时限 >= 1倍（例如：时限24小时，已过24小时）
 *    - 即将超时：距离时限 <= 2小时（例如：时限24小时，已过22小时）
 * 3. 发送提醒通知：
 *    - 站内消息通知审批人
 *    - 邮件提醒（严重超时时抄送上级）
 *    - 企业微信/钉钉提醒（可选）
 * 4. 自动升级：严重超时自动升级到上级审批人
 * 5. 自动审批：如果配置了 auto_approve_enabled，超时后自动通过
 *
 * XXL-Job 配置示例：
 * - 执行器：scm-supplier-executor
 * - JobHandler：approvalTimeoutReminderJob
 * - Cron：0 0 10,15 * * ?
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
public class ApprovalTimeoutReminderJob {

    private final PurchaseApprovalService purchaseApprovalService;
    private final NotificationService notificationService;

    /**
     * 执行审批超时提醒
     *
     * 任务参数（可选）：
     * - tenantId: 指定租户ID（UUID格式），不传则扫描所有租户
     * - autoEscalate: 是否自动升级（true/false），默认true
     * - autoApprove: 是否自动审批（true/false），默认false
     *
     * 示例：
     * - 扫描所有租户：不传参数
     * - 扫描单个租户：传参 "123e4567-e89b-12d3-a456-426614174000"
     * - 禁用自动升级：传参 "autoEscalate=false"
     * - 启用自动审批：传参 "autoApprove=true"
     */
    @XxlJob("approvalTimeoutReminderJob")
    public void execute() {
        long startTime = System.currentTimeMillis();
        String param = XxlJobHelper.getJobParam();

        try {
            // 解析参数
            UUID tenantId = null;
            boolean autoEscalate = true;
            boolean autoApprove = false;

            if (param != null && !param.trim().isEmpty()) {
                String[] parts = param.split(",");
                for (String part : parts) {
                    part = part.trim();
                    if (part.startsWith("autoEscalate=")) {
                        autoEscalate = Boolean.parseBoolean(part.substring(13));
                    } else if (part.startsWith("autoApprove=")) {
                        autoApprove = Boolean.parseBoolean(part.substring(12));
                    } else {
                        // 尝试解析为租户ID
                        try {
                            tenantId = UUID.fromString(part);
                        } catch (IllegalArgumentException e) {
                            log.warn("无法解析租户ID: {}", part);
                        }
                    }
                }
            }

            String scope = tenantId == null ? "所有租户" : "租户 " + tenantId;
            log.info("开始扫描超时审批任务，范围: {}, 自动升级: {}, 自动审批: {}",
                scope, autoEscalate, autoApprove);

            // 查询超时审批任务
            List<OverdueApprovalTaskVO> overdueTasks = purchaseApprovalService.getOverdueApprovalTasks(tenantId);

            if (overdueTasks.isEmpty()) {
                String msg = String.format("未发现超时审批任务，范围: %s", scope);
                log.info(msg);
                XxlJobHelper.handleSuccess(msg);
                return;
            }

            // 按租户分组
            Map<UUID, List<OverdueApprovalTaskVO>> groupedByTenant = overdueTasks.stream()
                .collect(Collectors.groupingBy(OverdueApprovalTaskVO::getTenantId));

            int totalTasks = 0;
            int reminderSent = 0;
            int escalated = 0;
            int autoApproved = 0;
            int failCount = 0;

            // 按租户处理
            for (Map.Entry<UUID, List<OverdueApprovalTaskVO>> entry : groupedByTenant.entrySet()) {
                UUID currentTenantId = entry.getKey();
                List<OverdueApprovalTaskVO> tasks = entry.getValue();
                totalTasks += tasks.size();

                try {
                    for (OverdueApprovalTaskVO task : tasks) {
                        try {
                            // 1. 发送提醒通知
                            notificationService.sendApprovalReminder(task);
                            reminderSent++;

                            // 2. 严重超时 - 自动升级
                            if (autoEscalate && task.isSeverelyOverdue()) {
                                boolean escalateSuccess = purchaseApprovalService.escalateApproval(task.getTaskId());
                                if (escalateSuccess) {
                                    log.info("审批任务 {} 已自动升级到上级审批人", task.getTaskId());
                                    escalated++;
                                }
                            }

                            // 3. 启用自动审批 - 自动通过
                            if (autoApprove && task.isAutoApproveEnabled() && task.isSeverelyOverdue()) {
                                boolean approveSuccess = purchaseApprovalService.autoApprove(task.getTaskId());
                                if (approveSuccess) {
                                    log.info("审批任务 {} 已自动通过（超时自动审批）", task.getTaskId());
                                    autoApproved++;
                                }
                            }

                        } catch (Exception e) {
                            log.error("处理审批任务 {} 失败", task.getTaskId(), e);
                            failCount++;
                        }
                    }

                    log.info("租户 {} 超时审批任务处理完成，共 {} 条（严重超时: {}, 一般超时: {}, 即将超时: {}）",
                        currentTenantId,
                        tasks.size(),
                        tasks.stream().filter(OverdueApprovalTaskVO::isSeverelyOverdue).count(),
                        tasks.stream().filter(t -> t.isOverdue() && !t.isSeverelyOverdue()).count(),
                        tasks.stream().filter(OverdueApprovalTaskVO::isAboutToTimeout).count()
                    );

                } catch (Exception e) {
                    log.error("租户 {} 超时审批任务处理失败", currentTenantId, e);
                    failCount += tasks.size();
                }
            }

            long duration = System.currentTimeMillis() - startTime;
            String successMsg = String.format(
                "审批超时提醒完成，范围: %s, 总任务数: %d, 已发送提醒: %d, 已升级: %d, 已自动审批: %d, 失败: %d, 耗时: %d ms",
                scope,
                totalTasks,
                reminderSent,
                escalated,
                autoApproved,
                failCount,
                duration
            );

            log.info(successMsg);

            if (failCount > 0) {
                XxlJobHelper.handleFail(successMsg);
            } else {
                XxlJobHelper.handleSuccess(successMsg);
            }

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            String errorMsg = String.format("审批超时提醒失败，耗时: %d ms", duration);
            log.error(errorMsg, e);
            XxlJobHelper.handleFail(errorMsg + ": " + e.getMessage());
        }
    }
}
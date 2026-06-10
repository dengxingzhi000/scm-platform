package com.scmcloud.tenant.job;

import com.scmcloud.common.tenant.quota.QuotaService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 绉熸埛閰嶉閲嶇疆瀹氭椂浠诲姟
 *
 * 鎵ц鏃堕棿锛氭瘡鏃ュ噷锟?0:05锛坈ron: 0 5 0 * * ?锟?
 *
 * 鍔熻兘锟?
 * 1. 閲嶇疆鎵€鏈夌鎴风殑姣忔棩閰嶉璁℃暟锟?
 * 2. 閲嶇疆瀛楁锛歝urrent_orders_today = 0, current_api_calls_today = 0
 * 3. 鏀寔鍙傛暟鍖栭噸缃崟涓鎴凤紙鐢ㄤ簬鎵嬪姩瑙﹀彂锟?
 *
 * XXL-Job 閰嶇疆绀轰緥锟?
 * - 鎵ц鍣細scm-tenant-executor
 * - JobHandler锛歲uotaResetJob
 * - Cron锟?5 0 * * ?
 * - 杩愯妯″紡锛欱EAN
 * - 闃诲澶勭悊绛栫暐锛氬崟鏈轰覆锟?
 * - 璺敱绛栫暐锛氳疆锟?
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
     * 鎵ц姣忔棩閰嶉閲嶇疆
     *
     * 浠诲姟鍙傛暟锛堝彲閫夛級锟?
     * - tenantId: 鎸囧畾绉熸埛ID锛圲UID鏍煎紡锛夛紝涓嶄紶鍒欓噸缃墍鏈夌锟?
     *
     * 绀轰緥锟?
     * - 閲嶇疆鎵€鏈夌鎴凤細涓嶄紶鍙傛暟
     * - 閲嶇疆鍗曚釜绉熸埛锛氫紶锟?123e4567-e89b-12d3-a456-426614174000"
     */
    @XxlJob("quotaResetJob")
    public void execute() {
        long startTime = System.currentTimeMillis();
        String param = XxlJobHelper.getJobParam();

        try {
            UUID tenantId = null;

            // 濡傛灉鏈夊弬鏁帮紝瑙ｆ瀽绉熸埛ID
            if (param != null && !param.trim().isEmpty()) {
                try {
                    tenantId = UUID.fromString(param.trim());
                    log.info("寮€濮嬮噸缃鎴烽厤棰濓紝绉熸埛ID: {}", tenantId);
                } catch (IllegalArgumentException e) {
                    String errorMsg = "Invalid tenant ID parameter: " + param;
                    log.error(errorMsg, e);
                    XxlJobHelper.handleFail(errorMsg);
                    return;
                }
            } else {
                log.info("寮€濮嬮噸缃墍鏈夌鎴风殑姣忔棩閰嶉");
            }

            // 鎵ц閰嶉閲嶇疆
            quotaService.resetDailyQuota(tenantId);

            long duration = System.currentTimeMillis() - startTime;
            String successMsg = tenantId == null
                ? String.format("Quota reset completed, duration: %d ms", duration)
                : String.format("Tenant %s quota reset completed, duration: %d ms", tenantId, duration);

            log.info(successMsg);
            XxlJobHelper.handleSuccess(successMsg);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            String errorMsg = String.format("Quota reset failed, duration: %d ms", duration);
            log.error(errorMsg, e);
            XxlJobHelper.handleFail(errorMsg + ": " + e.getMessage());
        }
    }
}
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
 * 骞冲彴鏈嶅姟璐硅绠楀畾鏃朵换锟?
 *
 * 鎵ц鏃堕棿锛氭瘡锟? 鏃ュ噷锟?2:00锛坈ron: 0 0 2 1 * ?锟?
 *
 * 鍔熻兘锟?
 * 1. 璁＄畻涓婃湀鎵€鏈夌鎴风殑骞冲彴鏈嶅姟锟?
 * 2. 璁¤垂椤瑰寘鎷細
 *    - 璁㈠崟浣ｉ噾锛堟寜璁㈠崟閲戦鐧惧垎姣旓級
 *    - 瀛樺偍璐圭敤锛堟寜瀹為檯浣跨敤GB璁¤垂锟?
 *    - API璋冪敤璐圭敤锛堣秴鍑哄椁愰儴鍒嗘寜娆¤璐癸級
 *    - 澧炲€兼湇鍔¤垂锛堥澶栧姛鑳芥ā鍧楄垂鐢級
 * 3. 鐢熸垚 platform_service_fee 璁板綍
 * 4. 鏇存柊绉熸埛搴旀敹璐︽ (tenant_accounts_receivable)
 * 5. 鍙戦€佽处鍗曢€氱煡閭欢
 *
 * XXL-Job 閰嶇疆绀轰緥锟?
 * - 鎵ц鍣細scm-tenant-executor
 * - JobHandler锛歱latformFeeCalculationJob
 * - Cron锟?0 2 1 * ?
 * - 杩愯妯″紡锛欱EAN
 * - 闃诲澶勭悊绛栫暐锛氬崟鏈轰覆锟?
 * - 璺敱绛栫暐锛氱涓€锟?
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
     * 鎵ц骞冲彴鏈嶅姟璐硅锟?
     *
     * 浠诲姟鍙傛暟锛堝彲閫夛級锟?
     * - yearMonth: 鎸囧畾璁＄畻鏈堜唤锛堟牸寮忥細yyyy-MM锛夛紝涓嶄紶鍒欒绠椾笂锟?
     * - tenantId: 鎸囧畾绉熸埛ID锛圲UID鏍煎紡锛夛紝涓嶄紶鍒欒绠楁墍鏈夌锟?
     *
     * 绀轰緥锟?
     * - 璁＄畻涓婃湀鎵€鏈夌鎴凤細涓嶄紶鍙傛暟
     * - 璁＄畻鎸囧畾鏈堜唤锛氫紶锟?2025-01"
     * - 璁＄畻鍗曚釜绉熸埛锛氫紶锟?tenantId=123e4567-e89b-12d3-a456-426614174000"
     * - 璁＄畻鎸囧畾鏈堜唤鍜岀鎴凤細浼犲弬 "2025-01,tenantId=123e4567-e89b-12d3-a456-426614174000"
     */
    @XxlJob("platformFeeCalculationJob")
    public void execute() {
        long startTime = System.currentTimeMillis();
        String param = XxlJobHelper.getJobParam();

        try {
            // 瑙ｆ瀽鍙傛暟
            YearMonth targetMonth = YearMonth.now().minusMonths(1); // 榛樿涓婃湀
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
                        // 灏濊瘯瑙ｆ瀽涓哄勾锟?
                        try {
                            targetMonth = YearMonth.parse(part, DateTimeFormatter.ofPattern("yyyy-MM"));
                        } catch (Exception e) {
                            log.warn("Unable to parse month parameter: {}, using default", part);
                        }
                    }
                }
            }

            String scope = tenantId == null ? "All tenants" : "Tenant " + tenantId;
            log.info("寮€濮嬭绠梴} {} 骞冲彴鏈嶅姟璐?, targetMonth, scope);

            // 鎵ц璁¤垂璁＄畻
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
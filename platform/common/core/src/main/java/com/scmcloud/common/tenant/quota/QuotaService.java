package com.scmcloud.common.tenant.quota;

import java.util.UUID;

/**
 * 閰嶉鏈嶅姟鎺ュ彛
 *
 * @author Claude Code
 * @since 2025-01-24
 */
public interface QuotaService {

    /**
     * 妫€鏌ラ厤棰濇槸鍚﹀厖锟?
     *
     * @param tenantId 绉熸埛 ID
     * @param quotaType 閰嶉绫诲瀷
     * @param increment 闇€瑕佹秷鑰楃殑閰嶉鏁伴噺
     * @return true=閰嶉鍏呰冻, false=閰嶉涓嶈冻
     */
    boolean checkQuota(UUID tenantId, QuotaType quotaType, int increment);

    /**
     * 妫€鏌ュ苟娑堣€楅厤棰濓紙鍘熷瓙鎿嶄綔锟?
     *
     * @param tenantId 绉熸埛 ID
     * @param quotaType 閰嶉绫诲瀷
     * @param increment 闇€瑕佹秷鑰楃殑閰嶉鏁伴噺
     * @return true=妫€鏌ラ€氳繃涓斿凡娑堬拷 false=閰嶉涓嶈冻
     */
    boolean checkAndConsumeQuota(UUID tenantId, QuotaType quotaType, int increment);

    /**
     * 閲婃斁閰嶉锛堝洖婊氭搷浣滐級
     *
     * @param tenantId 绉熸埛 ID
     * @param quotaType 閰嶉绫诲瀷
     * @param decrement 閲婃斁鐨勯厤棰濇暟锟?
     */
    void releaseQuota(UUID tenantId, QuotaType quotaType, int decrement);

    /**
     * 鑾峰彇閰嶉浣跨敤鎯呭喌
     *
     * @param tenantId 绉熸埛 ID
     * @param quotaType 閰嶉绫诲瀷
     * @return 閰嶉浣跨敤鎯呭喌
     */
    QuotaUsage getQuotaUsage(UUID tenantId, QuotaType quotaType);

    /**
     * 閲嶇疆姣忔棩閰嶉锛堣鍗曘€丄PI璋冪敤锟?
     * 鐢卞畾鏃朵换鍔℃瘡鏃ュ噷鏅ㄨ皟锟?
     *
     * @param tenantId 绉熸埛ID锛坣ull琛ㄧず閲嶇疆鎵€鏈夌鎴凤級
     */
    void resetDailyQuota(UUID tenantId);
}
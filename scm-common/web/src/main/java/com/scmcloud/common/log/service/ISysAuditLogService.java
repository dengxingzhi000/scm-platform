package com.scmcloud.common.log.service;

import java.util.UUID;

/**
 * <p>
 * 鎿嶄綔瀹¤鏃ュ織锟芥湇鍔★拷 * </p>
 *
 * @author author
 * @since 2025-10-14
 */
public interface ISysAuditLogService {

    /**
     * 璁板綍鐢ㄦ埛鐧诲綍鎿嶄綔鏃ュ織
     *
     * @param userId 鐢ㄦ埛鍞竴鏍囪瘑锟?
     * @param username 鐢ㄦ埛锟?
     * @param ipAddress 鐧诲綍 IP鍦板潃
     * @param success 鐧诲綍鏄惁鎴愬姛
     * @param remark 澶囨敞淇℃伅
     */
    void recordLogin(UUID userId, String username, String ipAddress, boolean success, String remark);

    /**
     * 璁板綍鐢ㄦ埛鐧诲綍澶辫触鎿嶄綔鏃ュ織
     *
     * @param username 鐢ㄦ埛锟?
     * @param ipAddress 鐧诲綍 IP鍦板潃
     * @param reason 鐧诲綍澶辫触鍘熷洜
     */
    void recordLoginFailure(String username, String ipAddress, String reason);

    /**
     * 璁板綍鐢ㄦ埛鐧诲嚭鎿嶄綔鏃ュ織
     *
     * @param userId 鐢ㄦ埛鍞竴鏍囪瘑锟?
     * @param remark 澶囨敞淇℃伅
     */
    void recordLogout(UUID userId, String remark);

    /**
     * 璁板綍瀹夊叏浜嬩欢鎿嶄綔鏃ュ織
     *
     * @param eventType 浜嬩欢绫诲瀷
     * @param riskLevel 椋庨櫓绛夌骇
     * @param userId 鐢ㄦ埛鍞竴鏍囪瘑锟?
     * @param username 鐢ㄦ埛锟?
     * @param ipAddress 鎿嶄綔 IP鍦板潃
     * @param resource 鎿嶄綔璧勬簮
     * @param success 鎿嶄綔鏄惁鎴愬姛
     * @param details 璇︾粏淇℃伅
     */
    void recordSecurityEvent(String eventType, Integer riskLevel, UUID userId, String username, String ipAddress,
                             String resource, boolean success, String details);
}

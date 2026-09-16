package com.scmcloud.audit.api;

import com.scmcloud.audit.api.dto.AuditQueryResult;
import com.scmcloud.audit.api.request.AuditLogRequest;
import com.scmcloud.audit.api.request.AuditQueryRequest;

/**
 * 瀹¤鏈嶅姟 Dubbo 鎺ュ彛
 *
 * <p>鎻愪緵鎿嶄綔鏃ュ織璁板綍銆佹棩蹇楁煡璇㈢瓑鏍稿績鍔熻兘锛屼緵鍏朵粬寰湇鍔¢€氳繃 RPC 璋冪敤锟?
 *
 * @author SCM Platform Team
 * @since 2025-12-26
 */
public interface AuditDubboService {

    /**
     * 璁板綍鎿嶄綔鏃ュ織
     *
     * @param request 瀹¤鏃ュ織璇锋眰
     */
    void logOperation(AuditLogRequest request);

    /**
     * 鏌ヨ瀹¤鏃ュ織
     *
     * @param request 鏌ヨ璇锋眰
     * @return 鏌ヨ缁撴灉
     */
    AuditQueryResult queryAuditLogs(AuditQueryRequest request);
}

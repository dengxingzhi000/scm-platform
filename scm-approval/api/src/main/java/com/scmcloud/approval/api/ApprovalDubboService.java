package com.scmcloud.approval.api;

import com.scmcloud.approval.api.dto.ApprovalVO;
import com.scmcloud.approval.api.request.ApprovalRequest;

/**
 * 瀹℃壒鏈嶅姟 Dubbo 鎺ュ彛
 *
 * <p>鎻愪緵瀹℃壒鎻愪氦銆佸鎵归€氳繃/椹冲洖銆佺姸鎬佹煡璇㈢瓑鏍稿績鍔熻兘锛屼緵鍏朵粬寰湇鍔¢€氳繃 RPC 璋冪敤锟?
 *
 * @author SCM Platform Team
 * @since 2025-12-26
 */
public interface ApprovalDubboService {

    /**
     * 鎻愪氦瀹℃壒
     *
     * @param request 瀹℃壒璇锋眰
     * @return 瀹℃壒淇℃伅
     */
    ApprovalVO submitApproval(ApprovalRequest request);

    /**
     * 瀹℃壒閫氳繃
     *
     * @param approvalId 瀹℃壒 ID
     * @param userId 瀹℃壒锟絀D
     */
    void approve(Long approvalId, Long userId);

    /**
     * 瀹℃壒椹冲洖
     *
     * @param approvalId 瀹℃壒 ID
     * @param userId 瀹℃壒锟絀D
     * @param reason 椹冲洖鍘熷洜
     */
    void reject(Long approvalId, Long userId, String reason);

    /**
     * 鏌ヨ瀹℃壒鐘讹拷
     *
     * @param approvalId 瀹℃壒 ID
     * @return 瀹℃壒淇℃伅锛屼笉瀛樺湪鏃惰繑锟絥ull
     */
    ApprovalVO getApprovalStatus(Long approvalId);
}

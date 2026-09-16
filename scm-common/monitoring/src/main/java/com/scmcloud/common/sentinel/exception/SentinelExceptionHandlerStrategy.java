package com.scmcloud.common.sentinel.exception;

import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.scmcloud.common.response.ApiResponse;

/**
 * Sentinel 寮傚父澶勭悊鍣ㄧ瓥鐣ユ帴锟?
 *
 * @author Deng
 * createData 2025/10/20 10:39
 * @version 1.0
 */
public interface SentinelExceptionHandlerStrategy {
    /**
     * 鍒ゆ柇鏄惁鏀寔澶勭悊璇ュ紓锟?
     * @param ex 寮傚父
     * @return 鏄惁鏀寔
     */
    boolean supports(BlockException ex);
    
    /**
     * 澶勭悊寮傚父
     * @param ex 寮傚父
     * @return 鍝嶅簲缁撴灉
     */
    ApiResponse<Void> handle(BlockException ex);
}
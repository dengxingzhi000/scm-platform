package com.scmcloud.common.sentinel.exception.Impl;

import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.flow.FlowException;
import com.scmcloud.common.response.ApiResponse;
import com.scmcloud.common.sentinel.exception.SentinelExceptionHandlerStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 *
 *
 * @author Deng
 * createData 2025/10/20 10:45
 * @version 1.0
 */
@Slf4j
@Component
public class FlowExceptionHandler implements SentinelExceptionHandlerStrategy {

    @Override
    public boolean supports(BlockException e) {
        return e instanceof FlowException;
    }

    @Override
    public ApiResponse<Void> handle(BlockException e) {
        log.debug("Sentinel flow control triggered: {}", e.getRule() != null ? e.getRule().getResource() : "unknown");
        return ApiResponse.fail(429, "请求过于频繁，请稍后再试");
    }
}
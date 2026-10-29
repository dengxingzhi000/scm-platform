package com.scmcloud.common.sentinel.exception.Impl;

import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeException;
import com.scmcloud.common.sentinel.exception.SentinelExceptionHandlerStrategy;
import com.scmcloud.common.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 *
 *
 * @author Deng
 * createData 2025/10/20 10:46
 * @version 1.0
 */
@Slf4j
@Component
public class DegradeExceptionHandler implements SentinelExceptionHandlerStrategy {

    @Override
    public boolean supports(BlockException e) {
        return e instanceof DegradeException;
    }

    @Override
    public ApiResponse<Void> handle(BlockException e) {
        log.debug("Sentinel circuit breaker triggered: {}", e.getRule() != null ? e.getRule().getResource() : "unknown");
        return ApiResponse.fail(503, "服务暂时不可用，请稍后再试");
    }
}
package com.cognora.lms.logistics.domain;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * 运单状态迁移非法（例如已签收后再改状态、待揽件直接签收）。
 *
 * <p>由全局异常处理映射为 HTTP 400，调用方可通过状态码区分「非法操作」与「服务异常」。</p>
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class IllegalStatusTransitionException extends RuntimeException {

    public IllegalStatusTransitionException(String message) {
        super(message);
    }
}

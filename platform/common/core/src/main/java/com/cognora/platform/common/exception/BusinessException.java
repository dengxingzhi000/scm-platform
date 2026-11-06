package com.cognora.platform.common.exception;

import com.cognora.platform.common.response.ResultCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 业务异常
 *
 * <p>业务异常表示可预期的业务失败（参数不合法、无权限、资源不存在、状态冲突等），
 * 因此必须携带 HTTP 状态码，由 {@link GlobalExceptionHandler} 原样返回。状态码的推导顺序：
 * {@link ErrorCode} 自带状态 → 与 {@link ResultCode} 数值码匹配的状态 → 数值本身是
 * 4xx/5xx 标准状态时直接采用 → 兜底 500（仅用于无法判定的遗留调用点）。</p>
 *
 * @author Deng
 * @since 2025-10-15
 */
@Getter
public class BusinessException extends RuntimeException {

    private static final int DEFAULT_CODE = 500;

    private final Integer code;
    private final HttpStatus httpStatus;

    public BusinessException(String message) {
        this(DEFAULT_CODE, HttpStatus.INTERNAL_SERVER_ERROR, message);
    }

    public BusinessException(Integer code, String message) {
        this(code, resolveHttpStatus(code), message);
    }

    public BusinessException(Integer code, HttpStatus httpStatus, String message) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus != null ? httpStatus : HttpStatus.INTERNAL_SERVER_ERROR;
    }

    public BusinessException(ErrorCode errorCode) {
        this(errorCode.getCode(), errorCode.getHttpStatus(), errorCode.getI18nKey());
    }

    public BusinessException(ErrorCode errorCode, String message) {
        this(errorCode.getCode(), errorCode.getHttpStatus(), message);
    }

    private static HttpStatus resolveHttpStatus(Integer code) {
        if (code == null) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        ErrorCode errorCode = ErrorCode.fromCode(code);
        if (errorCode != null) {
            return errorCode.getHttpStatus();
        }
        ResultCode resultCode = ResultCode.fromCode(code);
        if (resultCode != null) {
            return resultCode.getHttpStatus();
        }
        HttpStatus status = HttpStatus.resolve(code);
        if (status != null && (status.is4xxClientError() || status.is5xxServerError())) {
            return status;
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }
}

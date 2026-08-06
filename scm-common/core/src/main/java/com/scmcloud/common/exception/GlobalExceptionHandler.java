package com.scmcloud.common.exception;

import com.scmcloud.common.response.ApiResponse;
import com.scmcloud.common.response.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.util.HtmlUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 全局异常处理
 *
 * @author Deng
 * @since 2025-10-11
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<ApiResponse<Void>> handleServiceException(ServiceException e, HttpServletRequest request) {
        String traceId = resolveTraceId(request);
        log.error("Service exception at {}, traceId={}: {}", request.getRequestURI(), traceId, e.getMessage());
        HttpStatus status = HttpStatus.resolve(e.getCode()) != null ? HttpStatus.valueOf(e.getCode()) : HttpStatus.INTERNAL_SERVER_ERROR;
        return ResponseEntity.status(status)
                .body(ApiResponse.fail(e.getCode(), "Service error: " + escapeHtml(e.getMessage()) + " (traceId=" + traceId + ")"));
    }

    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiResponse<Void> handleBadCredentialsException(BadCredentialsException e) {
        log.warn("Bad credentials");
        return ApiResponse.fail(ResultCode.UNAUTHORIZED.getCode(), "Invalid username or password");
    }

    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiResponse<Void> handleAuthenticationException(AuthenticationException e, HttpServletRequest request) {
        String traceId = resolveTraceId(request);
        log.warn("Authentication failed: {}, URI: {}, traceId={}", e.getMessage(), request.getRequestURI(), traceId);
        return ApiResponse.fail(ResultCode.UNAUTHORIZED.getCode(), "Authentication failed (traceId=" + traceId + ")");
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiResponse<Void> handleAccessDeniedException(AccessDeniedException e, HttpServletRequest request) {
        String traceId = resolveTraceId(request);
        log.warn("Access denied: {}, URI: {}, traceId={}", e.getMessage(), request.getRequestURI(), traceId);
        return ApiResponse.fail(ResultCode.FORBIDDEN.getCode(), "Access denied (traceId=" + traceId + ")");
    }

    @ExceptionHandler(LockedException.class)
    @ResponseStatus(HttpStatus.LOCKED)
    public ApiResponse<Void> handleLockedException(LockedException e) {
        log.warn("Account locked");
        return ApiResponse.fail(ResultCode.USER_LOCKED.getCode(), "Account locked");
    }

    @ExceptionHandler(RateLimitException.class)
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    public ApiResponse<Void> handleRateLimitException(RateLimitException e) {
        log.warn("Rate limit exceeded");
        return ApiResponse.fail(ResultCode.TOO_MANY_REQUESTS.getCode(), "Too many requests");
    }

    @ExceptionHandler(UnauthorizedException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiResponse<Void> handleUnauthorizedException(UnauthorizedException e) {
        log.warn("Unauthorized");
        return ApiResponse.fail(ResultCode.UNAUTHORIZED.getCode(), "Unauthorized");
    }

    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleBusinessException(BusinessException e) {
        log.error("Business exception: {}", e.getMessage());
        return ApiResponse.fail(e.getCode(), "Business error: " + escapeHtml(e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Map<String, String>> handleValidationException(MethodArgumentNotValidException e) {
        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        log.warn("Validation failed: {}", errors);
        return new ApiResponse<>(ResultCode.BAD_REQUEST.getCode(), "Validation failed", errors,
                System.currentTimeMillis());
    }

    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Map<String, String>> handleBindException(BindException e) {
        Map<String, String> errors = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        error -> error.getDefaultMessage() != null ? error.getDefaultMessage() : "",
                        (existing, replacement) -> existing
                ));

        log.warn("Bind exception: {}", errors);
        return new ApiResponse<>(ResultCode.BAD_REQUEST.getCode(), "Parameter binding failed", errors,
                System.currentTimeMillis());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("Illegal argument: {}", e.getMessage());
        return ApiResponse.fail(ResultCode.BAD_REQUEST.getCode(), "Invalid argument");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleException(Exception e, HttpServletRequest request) {
        String traceId = resolveTraceId(request);
        log.error("Unexpected exception at {}, traceId={}: {}", request.getRequestURI(), traceId, e.getMessage(), e);
        return ApiResponse.fail(ResultCode.INTERNAL_SERVER_ERROR.getCode(), "Internal server error (traceId=" + traceId + ")");
    }

    private String resolveTraceId(HttpServletRequest request) {
        String id = request.getHeader("X-Request-ID");
        if (StringUtils.hasText(id)) {
            return id;
        }
        id = request.getHeader("traceparent");
        if (StringUtils.hasText(id)) {
            return id;
        }
        return UUID.randomUUID().toString();
    }

    private String escapeHtml(String input) {
        if (input == null) {
            return "";
        }
        return HtmlUtils.htmlEscape(input);
    }
}

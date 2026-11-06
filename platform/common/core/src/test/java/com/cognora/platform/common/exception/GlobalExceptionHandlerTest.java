package com.cognora.platform.common.exception;

import com.cognora.platform.common.response.ApiResponse;
import com.cognora.platform.common.response.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final HttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");

    @Test
    void serviceException_usesHttpStatusFromException() {
        var ex = new ServiceException(ErrorCode.ORDER_NOT_FOUND);
        ResponseEntity<ApiResponse<Void>> response = handler.handleServiceException(ex, request);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void serviceException_defaultHttpStatusIsInternalServerError() {
        var ex = new ServiceException("boom");
        ResponseEntity<ApiResponse<Void>> response = handler.handleServiceException(ex, request);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void serviceException_carriesCode() {
        var ex = new ServiceException(40000, "bad");
        ResponseEntity<ApiResponse<Void>> response = handler.handleServiceException(ex, request);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo(40000);
    }

    @Test
    void badCredentials_returns401() {
        ApiResponse<Void> response = handler.handleBadCredentialsException();
        assertThat(response.code()).isEqualTo(401);
    }

    @Test
    void accessDenied_returns403() {
        ApiResponse<Void> response = handler.handleAccessDeniedException(new AccessDeniedException("nope"), request);
        assertThat(response.code()).isEqualTo(403);
    }

    @Test
    void rateLimit_returns429() {
        ApiResponse<Void> response = handler.handleRateLimitException();
        assertThat(response.code()).isEqualTo(429);
    }

    @Test
    void businessException_returnsCode() {
        var ex = new BusinessException(40010, "biz error");
        ResponseEntity<ApiResponse<Void>> response = handler.handleBusinessException(ex, request);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo(40010);
        assertThat(response.getBody().message()).contains("biz error");
    }

    @Test
    void businessException_forbiddenBusinessCodeReturns403() {
        var ex = new BusinessException(ResultCode.FORBIDDEN.getCode(), "only platform admin can write platform config");
        ResponseEntity<ApiResponse<Void>> response = handler.handleBusinessException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo(ResultCode.FORBIDDEN.getCode());
        assertThat(response.getHeaders().getFirst("X-Request-ID")).isNotBlank();
    }

    @Test
    void businessException_errorCodePermissionDeniedReturns403() {
        var ex = new BusinessException(ErrorCode.PERMISSION_DENIED, "no access");
        ResponseEntity<ApiResponse<Void>> response = handler.handleBusinessException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo(ErrorCode.PERMISSION_DENIED.getCode());
    }

    @Test
    void businessException_errorCodeNotFoundReturns404() {
        var ex = new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        ResponseEntity<ApiResponse<Void>> response = handler.handleBusinessException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo(ErrorCode.ORDER_NOT_FOUND.getCode());
    }

    @Test
    void businessException_errorCodeConflictReturns409() {
        var ex = new BusinessException(ErrorCode.CONFLICT);
        ResponseEntity<ApiResponse<Void>> response = handler.handleBusinessException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo(ErrorCode.CONFLICT.getCode());
    }

    @Test
    void businessException_paramValidationErrorReturns400() {
        var ex = new BusinessException(ErrorCode.PARAM_VALIDATION_ERROR);
        ResponseEntity<ApiResponse<Void>> response = handler.handleBusinessException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo(ErrorCode.PARAM_VALIDATION_ERROR.getCode());
    }

    @Test
    void businessException_unresolvedCodeStillFallsBackTo500() {
        var ex = new BusinessException(9999, "legacy code");
        ResponseEntity<ApiResponse<Void>> response = handler.handleBusinessException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo(9999);
    }

    @Test
    void businessException_plainMessageStillFallsBackTo500() {
        var ex = new BusinessException("unexpected business failure");
        ResponseEntity<ApiResponse<Void>> response = handler.handleBusinessException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo(500);
    }

    @Test
    void illegalArgument_returns400() {
        ApiResponse<Void> response = handler.handleIllegalArgumentException(
                new IllegalArgumentException("bad arg"));
        assertThat(response.code()).isEqualTo(400);
    }

    @Test
    void validationException_returnsFieldErrors() {
        var bindingResult = new BeanPropertyBindingResult(new Object(), "target");
        bindingResult.addError(new FieldError("target", "field1", "must not be blank"));
        var ex = new MethodArgumentNotValidException((org.springframework.core.MethodParameter) null, bindingResult);
        ApiResponse<Map<String, String>> response = handler.handleValidationException(ex);
        assertThat(response.code()).isEqualTo(400);
        assertThat(response.data()).containsKey("field1");
    }

    @Test
    void unexpectedFailure_hidesInternalsButKeepsTraceCorrelation() {
        var ex = new IllegalStateException(
                "java.util.ServiceConfigurationError: io.jsonwebtoken.io.Serializer: Error accessing configuration file");

        ResponseEntity<ApiResponse<Void>> response = handler.handleException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).doesNotContain("Serializer", "ServiceConfigurationError");
        assertThat(response.getHeaders().getFirst("X-Request-ID")).isNotBlank();
        assertThat(response.getBody().message()).contains("traceId=");
    }

    @Test
    void frameworkRequestFailure_keepsItsOwnStatusInsteadOfFiveHundred() {
        ResponseEntity<ApiResponse<Void>> response = handler.handleException(
                new org.springframework.web.HttpRequestMethodNotSupportedException("POST"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo(405);
    }
}

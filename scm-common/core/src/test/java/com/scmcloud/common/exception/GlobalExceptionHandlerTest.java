package com.scmcloud.common.exception;

import com.scmcloud.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
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
        ApiResponse<Void> response = handler.handleBadCredentialsException(new BadCredentialsException("bad"));
        assertThat(response.code()).isEqualTo(401);
    }

    @Test
    void accessDenied_returns403() {
        ApiResponse<Void> response = handler.handleAccessDeniedException(new AccessDeniedException("nope"), request);
        assertThat(response.code()).isEqualTo(403);
    }

    @Test
    void rateLimit_returns429() {
        ApiResponse<Void> response = handler.handleRateLimitException(
                new com.scmcloud.common.exception.RateLimitException("slow down"));
        assertThat(response.code()).isEqualTo(429);
    }

    @Test
    void businessException_returnsCode() {
        var ex = new com.scmcloud.common.exception.BusinessException(40010, "biz error");
        ApiResponse<Void> response = handler.handleBusinessException(ex);
        assertThat(response.code()).isEqualTo(40010);
        assertThat(response.message()).contains("biz error");
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
}

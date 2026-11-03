package com.scmcloud.common.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class ServiceExceptionTest {

    @Test
    void defaultConstructor_shouldUseInternalServerError() {
        var ex = new ServiceException("boom");
        assertThat(ex.getCode()).isEqualTo(500);
        assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void codeConstructor_shouldCarryCodeAndDefault500() {
        var ex = new ServiceException(40000, "bad request");
        assertThat(ex.getCode()).isEqualTo(40000);
        assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void statusConstructor_shouldCarryHttpStatus() {
        var ex = new ServiceException(40000, HttpStatus.BAD_REQUEST, "bad");
        assertThat(ex.getCode()).isEqualTo(40000);
        assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void errorCodeConstructor_shouldMapToHttpStatusFromEnum() {
        var ex = new ServiceException(ErrorCode.ORDER_NOT_FOUND);
        assertThat(ex.getCode()).isEqualTo(ErrorCode.ORDER_NOT_FOUND.getCode());
        assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void errorCodeWithMessage_shouldKeepMessageAndUseMappedStatus() {
        var ex = new ServiceException(ErrorCode.PERMISSION_DENIED, "no access");
        assertThat(ex.getCode()).isEqualTo(ErrorCode.PERMISSION_DENIED.getCode());
        assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(ex.getMessage()).isEqualTo("no access");
    }
}

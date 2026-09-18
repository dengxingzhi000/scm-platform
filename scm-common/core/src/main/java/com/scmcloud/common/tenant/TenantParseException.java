package com.scmcloud.common.tenant;

public class TenantParseException extends RuntimeException {
    public TenantParseException(String message) {
        super(message);
    }

    public TenantParseException(String message, Throwable cause) {
        super(message, cause);
    }
}

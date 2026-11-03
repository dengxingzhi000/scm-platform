package com.scmcloud.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Business error codes for the SCM Platform.
 * 
 * Error code ranges:
 * - 400xx: Client errors (bad request, validation)
 * - 401xx: Authentication errors
 * - 403xx: Authorization errors
 * - 404xx: Resource not found
 * - 409xx: Conflict errors (state transitions, duplicates)
 * - 429xx: Rate limiting
 * - 500xx: Server errors
 *
 * @author SCM Platform
 * @since 2026-06-04
 */
@Getter
public enum ErrorCode {

    // ─── General ──────────────────────────────────────────────────
    SYSTEM_ERROR(50000, HttpStatus.INTERNAL_SERVER_ERROR, "system.error"),
    SYSTEM_BUSY(50001, HttpStatus.SERVICE_UNAVAILABLE, "system.busy"),
    PARAM_VALIDATION_ERROR(40000, HttpStatus.BAD_REQUEST, "param.validation_error"),
    PARAM_TYPE_ERROR(40001, HttpStatus.BAD_REQUEST, "param.type_error"),

    // ─── Authentication ───────────────────────────────────────────
    AUTH_REQUIRED(40100, HttpStatus.UNAUTHORIZED, "auth.required"),
    AUTH_TOKEN_EXPIRED(40101, HttpStatus.UNAUTHORIZED, "auth.token_expired"),
    AUTH_TOKEN_INVALID(40102, HttpStatus.UNAUTHORIZED, "auth.token_invalid"),
    AUTH_LOGIN_FAILED(40103, HttpStatus.UNAUTHORIZED, "auth.login_failed"),
    AUTH_MFA_REQUIRED(40104, HttpStatus.UNAUTHORIZED, "auth.mfa_required"),

    // ─── Authorization ────────────────────────────────────────────
    FORBIDDEN(40300, HttpStatus.FORBIDDEN, "forbidden"),
    TENANT_SUSPENDED(40301, HttpStatus.FORBIDDEN, "tenant.suspended"),
    TENANT_NOT_FOUND(40302, HttpStatus.FORBIDDEN, "tenant.not_found"),
    PERMISSION_DENIED(40303, HttpStatus.FORBIDDEN, "permission.denied"),

    // ─── Resource Not Found ───────────────────────────────────────
    NOT_FOUND(40400, HttpStatus.NOT_FOUND, "not_found"),
    ORDER_NOT_FOUND(40401, HttpStatus.NOT_FOUND, "order.not_found"),
    PRODUCT_NOT_FOUND(40402, HttpStatus.NOT_FOUND, "product.not_found"),
    INVENTORY_NOT_FOUND(40403, HttpStatus.NOT_FOUND, "inventory.not_found"),
    USER_NOT_FOUND(40404, HttpStatus.NOT_FOUND, "user.not_found"),
    TENANT_RESOURCE_NOT_FOUND(40405, HttpStatus.NOT_FOUND, "tenant.resource_not_found"),

    // ─── Conflict / State Errors ──────────────────────────────────
    CONFLICT(40900, HttpStatus.CONFLICT, "conflict"),
    ORDER_NOT_PAYABLE(40901, HttpStatus.CONFLICT, "order.not_payable"),
    ORDER_NOT_CANCELLABLE(40902, HttpStatus.CONFLICT, "order.not_cancellable"),
    ORDER_INVALID_TRANSITION(40903, HttpStatus.CONFLICT, "order.invalid_transition"),
    DUPLICATE_ORDER(40904, HttpStatus.CONFLICT, "order.duplicate"),
    INSUFFICIENT_STOCK(40910, HttpStatus.CONFLICT, "inventory.insufficient_stock"),
    STOCK_RESERVATION_EXPIRED(40911, HttpStatus.GONE, "inventory.reservation_expired"),
    DUPLICATE_PRODUCT_CODE(40920, HttpStatus.CONFLICT, "product.duplicate_code"),
    DUPLICATE_USER_EMAIL(40930, HttpStatus.CONFLICT, "user.duplicate_email"),
    DUPLICATE_TENANT_CODE(40940, HttpStatus.CONFLICT, "tenant.duplicate_code"),

    // ─── Rate Limiting ────────────────────────────────────────────
    RATE_LIMITED(42900, HttpStatus.TOO_MANY_REQUESTS, "rate_limited"),
    TENANT_QUOTA_EXCEEDED(42901, HttpStatus.TOO_MANY_REQUESTS, "tenant.quota_exceeded"),

    // ─── Business Logic ───────────────────────────────────────────
    BUSINESS_ERROR(40010, HttpStatus.BAD_REQUEST, "business.error"),
    INVALID_STATE_TRANSITION(40011, HttpStatus.BAD_REQUEST, "state.invalid_transition"),
    LOCK_ACQUISITION_FAILED(40012, HttpStatus.CONFLICT, "lock.acquisition_failed"),
    IDEMPOTENT_REPLAY(40013, HttpStatus.CONFLICT, "idempotent.replay");

    private final int code;
    private final HttpStatus httpStatus;
    private final String i18nKey;

    ErrorCode(int code, HttpStatus httpStatus, String i18nKey) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.i18nKey = i18nKey;
    }

    /**
     * Get the HTTP status code.
     */
    public int getHttpStatusCode() {
        return httpStatus.value();
    }
}

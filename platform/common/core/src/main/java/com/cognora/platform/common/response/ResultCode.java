package com.cognora.platform.common.response;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 缁熶竴鐘舵€佺爜鏋氫妇
 *
 * @author Deng
 * createData 2025/10/11 14:31
 * @version 1.0
 */
@Getter
public enum ResultCode {
    BAD_REQUEST(400, HttpStatus.BAD_REQUEST, "Bad Request"),
    UNAUTHORIZED(401, HttpStatus.UNAUTHORIZED, "Unauthorized"),
    FORBIDDEN(403, HttpStatus.FORBIDDEN, "Forbidden"),
    NOT_FOUND(404, HttpStatus.NOT_FOUND, "Not Found"),
    VALIDATION_FAILED(422, HttpStatus.UNPROCESSABLE_ENTITY, "Validation Failed"),
    SERVER_ERROR(500, HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error"),

    // 瀹㈡埛绔敊锟?xx
    METHOD_NOT_ALLOWED(405, HttpStatus.METHOD_NOT_ALLOWED, "Method Not Allowed"),
    CONFLICT(409, HttpStatus.CONFLICT, "Resource Conflict"),
    TOO_MANY_REQUESTS(429, HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests"),

    // 鏈嶅姟绔敊锟?xx
    INTERNAL_SERVER_ERROR(500, HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error"),
    SERVICE_UNAVAILABLE(503, HttpStatus.SERVICE_UNAVAILABLE, "Service Unavailable"),
    GATEWAY_TIMEOUT(504, HttpStatus.GATEWAY_TIMEOUT, "Gateway Timeout"),

    // 涓氬姟閿欒 1xxx
    USER_NOT_FOUND(1001, HttpStatus.NOT_FOUND, "User Not Found"),
    USER_PASSWORD_ERROR(1002, HttpStatus.UNAUTHORIZED, "Invalid Username or Password"),
    USER_LOCKED(1003, HttpStatus.LOCKED, "Account Locked"),
    USER_DISABLED(1004, HttpStatus.FORBIDDEN, "Account Disabled"),
    USER_NOT_ACTIVATED(1005, HttpStatus.FORBIDDEN, "User Not Activated"),
    USER_EXIST(1006, HttpStatus.CONFLICT, "User Already Exists"),
    USER_CANNOT_DELETE_ADMIN(1007, HttpStatus.FORBIDDEN, "Cannot Delete Admin User"),
    USER_CANNOT_DELETE_SELF(1008, HttpStatus.BAD_REQUEST, "Cannot Delete Self"),
    USER_PASSWORD_INCORRECT_OLD(1010, HttpStatus.BAD_REQUEST, "Old Password Incorrect"),
    USER_PASSWORD_SAME_AS_OLD(1011, HttpStatus.BAD_REQUEST, "New Password Must Differ From Old"),
    USER_PASSWORD_POLICY_VIOLATION(1012, HttpStatus.BAD_REQUEST, "Password Does Not Meet Policy"),
    USER_PASSWORD_IN_HISTORY(1013, HttpStatus.CONFLICT, "Password Was Used Recently"),
    USER_NEED_LOGIN(1009, HttpStatus.UNAUTHORIZED, "Login Required"),


    TOKEN_INVALID(1101, HttpStatus.UNAUTHORIZED, "Token Invalid"),
    TOKEN_EXPIRED(1102, HttpStatus.UNAUTHORIZED, "Token Expired"),
    TOKEN_BLACKLISTED(1103, HttpStatus.UNAUTHORIZED, "Token Revoked"),

    PERMISSION_DENIED(1201, HttpStatus.FORBIDDEN, "Permission Denied"),
    ROLE_NOT_FOUND(1202, HttpStatus.NOT_FOUND, "Role Not Found"),
    ROLE_REQUIRED(1203, HttpStatus.FORBIDDEN, "Role Required"),
    ROLE_ASSIGNMENT_DENIED(1204, HttpStatus.FORBIDDEN, "Role Assignment Denied"),
    DATA_ACCESS_DENIED(1205, HttpStatus.FORBIDDEN, "Data Access Denied"),

    // 澶氱鎴烽敊锟?3xx
    TENANT_CONTEXT_MISSING(1301, HttpStatus.BAD_REQUEST, "Tenant Context Missing"),
    DATA_TENANT_MISSING(1302, HttpStatus.BAD_REQUEST, "Data Not Associated with Tenant"),
    TENANT_DATA_ACCESS_DENIED(1303, HttpStatus.FORBIDDEN, "Cannot Access Other Tenant's Data"),
    TENANT_ROLE_ACCESS_DENIED(1304, HttpStatus.FORBIDDEN, "Cannot Access Other Tenant's Roles"),
    TENANT_PERMISSION_ACCESS_DENIED(1305, HttpStatus.FORBIDDEN, "Cannot Access Other Tenant's Permissions"),
    PLATFORM_RESOURCE_ACCESS_DENIED(1306, HttpStatus.FORBIDDEN, "Only Platform Admin Can Create Platform Resources"),
    DEPT_TENANT_MISSING(1307, HttpStatus.BAD_REQUEST, "Department Not Associated with Tenant"),
    TENANT_DEPT_ACCESS_DENIED(1308, HttpStatus.FORBIDDEN, "Cannot Access Other Tenant's Departments"),

    // 鏈嶅姟闂磋皟鐢ㄩ敊锟?xxx
    FEIGN_ERROR(2001, HttpStatus.BAD_GATEWAY, "Service Call Failed"),
    FEIGN_TIMEOUT(2002, HttpStatus.GATEWAY_TIMEOUT, "Service Call Timeout");

    private final int code;
    private final HttpStatus httpStatus;
    private final String message;

    ResultCode(int code, HttpStatus httpStatus, String message) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.message = message;
    }

    /**
     * Look up a result code by its numeric business code, or {@code null} when unknown.
     */
    public static ResultCode fromCode(int code) {
        for (ResultCode resultCode : values()) {
            if (resultCode.code == code) {
                return resultCode;
            }
        }
        return null;
    }
}

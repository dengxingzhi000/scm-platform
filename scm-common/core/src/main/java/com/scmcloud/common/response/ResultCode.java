package com.scmcloud.common.response;

import lombok.Getter;

/**
 * 缁熶竴鐘舵€佺爜鏋氫妇
 *
 * @author Deng
 * createData 2025/10/11 14:31
 * @version 1.0
 */
@Getter
public enum ResultCode {
    BAD_REQUEST(400, "Bad Request"),
    UNAUTHORIZED(401, "Unauthorized"),
    FORBIDDEN(403, "Forbidden"),
    NOT_FOUND(404, "Not Found"),
    VALIDATION_FAILED(422, "Validation Failed"),
    SERVER_ERROR(500, "Internal Server Error"),

    // 瀹㈡埛绔敊锟?xx
    METHOD_NOT_ALLOWED(405, "Method Not Allowed"),
    CONFLICT(409, "Resource Conflict"),
    TOO_MANY_REQUESTS(429, "Too Many Requests"),

    // 鏈嶅姟绔敊锟?xx
    INTERNAL_SERVER_ERROR(500, "Internal Server Error"),
    SERVICE_UNAVAILABLE(503, "Service Unavailable"),
    GATEWAY_TIMEOUT(504, "Gateway Timeout"),

    // 涓氬姟閿欒 1xxx
    USER_NOT_FOUND(1001, "User Not Found"),
    USER_PASSWORD_ERROR(1002, "Invalid Username or Password"),
    USER_LOCKED(1003, "Account Locked"),
    USER_DISABLED(1004, "Account Disabled"),
    USER_NOT_ACTIVATED(1005, "User Not Activated"),
    USER_EXIST(1006, "User Already Exists"),
    USER_CANNOT_DELETE_ADMIN(1007, "Cannot Delete Admin User"),
    USER_CANNOT_DELETE_SELF(1008, "Cannot Delete Self"),
    USER_NEED_LOGIN(1009, "Login Required"),


    TOKEN_INVALID(1101, "Token Invalid"),
    TOKEN_EXPIRED(1102, "Token Expired"),
    TOKEN_BLACKLISTED(1103, "Token Revoked"),

    PERMISSION_DENIED(1201, "Permission Denied"),
    ROLE_NOT_FOUND(1202, "Role Not Found"),
    ROLE_REQUIRED(1203, "Role Required"),
    ROLE_ASSIGNMENT_DENIED(1204, "Role Assignment Denied"),
    DATA_ACCESS_DENIED(1205, "Data Access Denied"),

    // 澶氱鎴烽敊锟?3xx
    TENANT_CONTEXT_MISSING(1301, "Tenant Context Missing"),
    DATA_TENANT_MISSING(1302, "Data Not Associated with Tenant"),
    TENANT_DATA_ACCESS_DENIED(1303, "Cannot Access Other Tenant's Data"),
    TENANT_ROLE_ACCESS_DENIED(1304, "Cannot Access Other Tenant's Roles"),
    TENANT_PERMISSION_ACCESS_DENIED(1305, "Cannot Access Other Tenant's Permissions"),
    PLATFORM_RESOURCE_ACCESS_DENIED(1306, "Only Platform Admin Can Create Platform Resources"),
    DEPT_TENANT_MISSING(1307, "Department Not Associated with Tenant"),
    TENANT_DEPT_ACCESS_DENIED(1308, "Cannot Access Other Tenant's Departments"),

    // 鏈嶅姟闂磋皟鐢ㄩ敊锟?xxx
    FEIGN_ERROR(2001, "Service Call Failed"),
    FEIGN_TIMEOUT(2002, "Service Call Timeout");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
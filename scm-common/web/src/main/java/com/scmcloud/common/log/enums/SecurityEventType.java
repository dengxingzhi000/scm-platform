package com.scmcloud.common.log.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 *
 *
 * @author Deng
 * createData 2025/10/24 16:14
 * @version 1.0
 */
@Getter
@AllArgsConstructor
public enum SecurityEventType {

    LOGIN_SUCCESS("Login Success", 1),
    LOGIN_FAILED("Login Failed", 2),
    LOGOUT("Logout", 1),

    PASSWORD_CHANGED("Password Changed", 2),
    PASSWORD_RESET("Password Reset", 3),

    PERMISSION_GRANTED("Permission Granted", 3),
    PERMISSION_REVOKED("Permission Revoked", 3),

    DATA_EXPORT("Data Export", 4),
    DATA_DELETE("Data Deletion", 4),

    API_ABUSE("API Abuse", 4),
    SQL_INJECTION_ATTEMPT("SQL Injection Attempt", 5),
    XSS_ATTEMPT("XSS Attack Attempt", 5),
    UNAUTHORIZED_ACCESS("Unauthorized Access", 4),

    IP_BLACKLISTED("IP Blacklisted", 3),
    ACCOUNT_LOCKED("Account Locked", 3);

    private final String description;
    private final Integer riskLevel; // 1-锟?-锟?-锟?-涓ラ噸 5-绱э拷
}

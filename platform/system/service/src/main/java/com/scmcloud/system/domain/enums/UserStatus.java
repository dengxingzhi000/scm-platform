package com.scmcloud.system.domain.enums;

/**
 * User account status code mapping.
 *
 * <p>Replaces the ad-hoc int/0/1/2 literals scattered across the user service.</p>
 */
public enum UserStatus {

    INACTIVE(0),
    ACTIVE(1),
    LOCKED(2);

    private final int code;

    UserStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static UserStatus fromCode(int code) {
        for (UserStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown user status code: " + code);
    }
}

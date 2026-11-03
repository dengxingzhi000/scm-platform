package com.scmcloud.common.security.util;

/**
 * Carries the caller's bearer token and device id for internal RestClient calls.
 *
 * <p>Without the token and the device header, scm-system rejects the permission lookup performed by
 * {@code ApiAccessControlFilter} and the check fails closed, so every business API answers 401.
 * The values are set by {@code JwtAuthenticationFilter} and cleared when the request completes.
 */
public final class AuthTokenContext {

    private static final ThreadLocal<AuthToken> CURRENT = new ThreadLocal<>();

    private AuthTokenContext() {
    }

    public static void set(String token, String deviceId) {
        CURRENT.set(new AuthToken(token, deviceId));
    }

    public static String getToken() {
        AuthToken current = CURRENT.get();
        return current != null ? current.token() : null;
    }

    public static String getDeviceId() {
        AuthToken current = CURRENT.get();
        return current != null ? current.deviceId() : null;
    }

    public static void clear() {
        CURRENT.remove();
    }

    private record AuthToken(String token, String deviceId) {
    }
}

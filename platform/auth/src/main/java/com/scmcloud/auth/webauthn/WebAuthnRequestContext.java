package com.scmcloud.auth.webauthn;

import com.scmcloud.common.security.util.HttpServletRequestUtils;
import com.scmcloud.common.security.util.IpUtils;
import com.scmcloud.common.web.domain.SecurityUser;
import jakarta.servlet.http.HttpServletRequest;

import java.util.UUID;

/**
 * Immutable per-request WebAuthn context extracted from {@link SecurityUser} and {@link HttpServletRequest}.
 * <p>
 * Encapsulates the userId / username / deviceId / ipAddress / userAgent tuple that every WebAuthn
 * endpoint needs, eliminating the repeated extraction boilerplate that previously lived in each
 * controller method.
 */
public record WebAuthnRequestContext(
        UUID userId,
        String username,
        String deviceId,
        String ipAddress,
        String userAgent
) {

    /**
     * Build a context from the authenticated user and HTTP request.
     *
     * @param user    current authenticated principal (never null)
     * @param request current HTTP request (never null)
     * @param utils   request utilities used to resolve the device id
     * @return immutable request context
     */
    public static WebAuthnRequestContext of(
            SecurityUser user, HttpServletRequest request, HttpServletRequestUtils utils) {
        return new WebAuthnRequestContext(
                user.getUserId(),
                user.getUsername(),
                utils.getDeviceId(request),
                IpUtils.getClientIp(request),
                request.getHeader("User-Agent")
        );
    }
}
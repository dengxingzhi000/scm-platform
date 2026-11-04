package com.cognora.platform.common.web.ownership;

import com.cognora.platform.common.web.util.SecurityUtils;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.util.StringUtils;

import java.util.UUID;

/**
 * Resource-ownership checks for self-service endpoints.
 *
 * <p>The identity always comes from the authenticated principal, never from a request parameter.
 * A caller may only act on its own resources; platform super administrators are deliberately not
 * exempt. Callers should fetch the entity first and pass its owner id, so the check is an explicit
 * branch instead of a silent {@code WHERE user_id = ?} filter.
 */
public final class OwnershipGuard {

    private OwnershipGuard() {
    }

    /**
     * Require the authenticated user to be {@code targetUserId}.
     *
     * @throws AccessDeniedException when there is no authenticated user or the ids differ
     */
    public static void requireSelf(UUID targetUserId) {
        if (targetUserId == null) {
            throw deny();
        }
        requireSelfOrThrow(targetUserId.toString());
    }

    /**
     * Require the authenticated user to equal {@code targetUserId} (UUID text, with or without
     * dashes).
     *
     * @throws AccessDeniedException when the target is blank/invalid or does not match
     */
    public static void requireSelfOrThrow(String targetUserId) {
        if (!StringUtils.hasText(targetUserId)) {
            throw deny();
        }
        String current = SecurityUtils.getCurrentUserUuid()
                .map(UUID::toString)
                .orElse(null);
        if (current == null) {
            throw deny();
        }
        String expected = current.replace("-", "");
        String actual = targetUserId.trim().replace("-", "");
        if (!expected.equalsIgnoreCase(actual)) {
            throw deny();
        }
    }

    private static AccessDeniedException deny() {
        return new AccessDeniedException(
                "Access denied: resource does not belong to the current user");
    }
}

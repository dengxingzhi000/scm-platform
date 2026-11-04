package com.cognora.platform.common.web.ownership;

import com.cognora.platform.common.web.domain.SecurityUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OwnershipGuardTest {

    private static final UUID ALICE = UUID.fromString("019a0aee-0000-7000-8000-000000000001");
    private static final UUID BOB = UUID.fromString("019a0aee-0000-7000-8000-000000000002");

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void allowsTheOwner() {
        authenticate(ALICE);

        assertThatCode(() -> OwnershipGuard.requireSelf(ALICE)).doesNotThrowAnyException();
        assertThatCode(() -> OwnershipGuard.requireSelfOrThrow(ALICE.toString())).doesNotThrowAnyException();
    }

    @Test
    void rejectsAnotherUsersUuid() {
        authenticate(ALICE);

        assertThatThrownBy(() -> OwnershipGuard.requireSelf(BOB))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> OwnershipGuard.requireSelfOrThrow(BOB.toString()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void rejectsWithoutAnAuthenticatedPrincipal() {
        assertThatThrownBy(() -> OwnershipGuard.requireSelf(ALICE))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> OwnershipGuard.requireSelfOrThrow(ALICE.toString()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void rejectsBlankInvalidOrNullTargets() {
        authenticate(ALICE);

        assertThatThrownBy(() -> OwnershipGuard.requireSelfOrThrow(null))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> OwnershipGuard.requireSelfOrThrow("   "))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> OwnershipGuard.requireSelfOrThrow("not-a-uuid"))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> OwnershipGuard.requireSelf(null))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void acceptsTheDashedAndCompactFormsOfTheSameUuid() {
        authenticate(ALICE);

        assertThatCode(() -> OwnershipGuard.requireSelfOrThrow(ALICE.toString().replace("-", "")))
                .doesNotThrowAnyException();
    }

    private static void authenticate(UUID userId) {
        SecurityUser principal = SecurityUser.builder()
                .userId(userId)
                .username("member")
                .roles(Set.of())
                .permissions(Set.of())
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }
}

package com.cognora.ecom.member.controller;

import com.cognora.ecom.member.service.IMemberPointsService;
import com.cognora.platform.common.web.domain.SecurityUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class MemberPointsControllerOwnershipTest {

    private static final UUID ALICE = UUID.fromString("019a0aee-0000-7000-8000-000000000001");
    private static final UUID BOB = UUID.fromString("019a0aee-0000-7000-8000-000000000002");

    @Mock
    private IMemberPointsService pointsService;

    private MemberPointsController controller;

    @BeforeEach
    void setUp() {
        controller = new MemberPointsController(pointsService);
        authenticate(ALICE);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void anotherUsersPointsBalanceIsForbidden() {
        assertThatThrownBy(() -> controller.getBalance(BOB.toString()))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(pointsService);
    }

    @Test
    void anotherUsersPointsLogIsForbidden() {
        assertThatThrownBy(() -> controller.getLog(BOB.toString()))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(pointsService);
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

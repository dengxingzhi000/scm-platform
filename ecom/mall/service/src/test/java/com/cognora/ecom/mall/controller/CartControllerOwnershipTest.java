package com.cognora.ecom.mall.controller;

import com.cognora.ecom.mall.domain.entity.Cart;
import com.cognora.ecom.mall.service.ICartService;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartControllerOwnershipTest {

    private static final UUID ALICE = UUID.fromString("019a0aee-0000-7000-8000-000000000001");
    private static final UUID BOB = UUID.fromString("019a0aee-0000-7000-8000-000000000002");

    @Mock
    private ICartService cartService;

    private CartController controller;

    @BeforeEach
    void setUp() {
        controller = new CartController(cartService);
        authenticate(ALICE);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void anotherUsersCartItemCannotBeUpdated() {
        when(cartService.getById(10L)).thenReturn(new Cart().setId(10L).setUserId(BOB.toString()));
        CartController.UpdateQuantityRequest request = new CartController.UpdateQuantityRequest();
        request.setQuantity(5);

        assertThatThrownBy(() -> controller.updateQuantity(10L, request))
                .isInstanceOf(AccessDeniedException.class);
        verify(cartService, never()).updateQuantity(anyString(), anyLong(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void anotherUsersCartItemCannotBeDeleted() {
        when(cartService.getById(10L)).thenReturn(new Cart().setId(10L).setUserId(BOB.toString()));

        assertThatThrownBy(() -> controller.removeFromCart(10L, BOB.toString()))
                .isInstanceOf(AccessDeniedException.class);
        verify(cartService, never()).removeFromCart(anyString(), anyLong());
    }

    @Test
    void ownCartItemCanBeUpdatedUsingTheAuthenticatedUser() {
        when(cartService.getById(10L)).thenReturn(new Cart().setId(10L).setUserId(ALICE.toString()));
        CartController.UpdateQuantityRequest request = new CartController.UpdateQuantityRequest();
        request.setQuantity(2);

        controller.updateQuantity(10L, request);

        verify(cartService).updateQuantity(ALICE.toString(), 10L, 2);
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

package com.cognora.ecom.mall.controller;

import com.cognora.ecom.mall.domain.entity.Cart;
import com.cognora.ecom.mall.service.ICartService;
import com.cognora.platform.common.web.ownership.OwnershipGuard;
import com.cognora.platform.common.web.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/api/v1/mall/cart")
@Tag(name = "购物车", description = "用户购物车的查询、加购、改数量、删除与清空接口。")
public class CartController {

    private final ICartService cartService;

    @Operation(summary = "查询购物车", description = "获取当前登录用户的购物车商品列表。")
    @GetMapping
    public List<Cart> getCart(@Parameter(description = "用户标识（已废弃，以登录态为准）") @RequestParam(required = false) String userId) {
        String currentUserId = currentUserId();
        log.info("[API] Get cart: userId={}", (currentUserId + "").replace("\r", "_").replace("\n", "_"));
        return cartService.getCart(currentUserId);
    }

    @Operation(summary = "加入购物车", description = "将指定商品 SKU 按数量添加至当前登录用户的购物车。")
    @PostMapping
    public boolean addToCart(@RequestBody AddToCartRequest request) {
        String currentUserId = currentUserId();
        log.info("[API] Add to cart: userId={}, productId={}, skuId={}, quantity={}",
                (currentUserId + "").replace("\r", "_").replace("\n", "_"), (request.getProductId() + "").replace("\r", "_").replace("\n", "_"), (request.getSkuId() + "").replace("\r", "_").replace("\n", "_"), (request.getQuantity() + "").replace("\r", "_").replace("\n", "_"));
        cartService.addToCart(currentUserId, request.getProductId(), request.getSkuId(), request.getQuantity());
        return true;
    }

    @Operation(summary = "修改购物车数量", description = "更新当前登录用户购物车中指定商品行的购买数量。")
    @PutMapping("/{cartItemId}")
    public boolean updateQuantity(@Parameter(description = "购物车条目 ID") @PathVariable Long cartItemId, @RequestBody UpdateQuantityRequest request) {
        String currentUserId = currentUserId();
        requireCartOwner(cartItemId);
        log.info("[API] Update quantity: cartItemId={}, quantity={}", cartItemId, request.getQuantity());
        cartService.updateQuantity(currentUserId, cartItemId, request.getQuantity());
        return true;
    }

    @Operation(summary = "删除购物车商品", description = "从当前登录用户的购物车中移除某条商品记录。")
    @DeleteMapping("/{cartItemId}")
    public boolean removeFromCart(@Parameter(description = "购物车条目 ID") @PathVariable Long cartItemId,
                                  @Parameter(description = "用户标识（已废弃，以登录态为准）") @RequestParam(required = false) String userId) {
        String currentUserId = currentUserId();
        requireCartOwner(cartItemId);
        log.info("[API] Remove from cart: cartItemId={}, userId={}", (cartItemId + "").replace("\r", "_").replace("\n", "_"), (currentUserId + "").replace("\r", "_").replace("\n", "_"));
        cartService.removeFromCart(currentUserId, cartItemId);
        return true;
    }

    @Operation(summary = "清空购物车", description = "删除当前登录用户购物车中的全部商品。")
    @DeleteMapping("/clear")
    public boolean clearCart(@Parameter(description = "用户标识（已废弃，以登录态为准）") @RequestParam(required = false) String userId) {
        String currentUserId = currentUserId();
        log.info("[API] Clear cart: userId={}", (currentUserId + "").replace("\r", "_").replace("\n", "_"));
        cartService.clearCart(currentUserId);
        return true;
    }

    private String currentUserId() {
        return SecurityUtils.requireCurrentUserUuid().toString();
    }

    private void requireCartOwner(Long cartItemId) {
        Cart cart = cartService.getById(cartItemId);
        if (cart == null) {
            throw new AccessDeniedException("Access denied: cart item not found");
        }
        OwnershipGuard.requireSelfOrThrow(cart.getUserId());
    }

    @Data
    public static class AddToCartRequest {
        private String userId;
        private Long productId;
        private Long skuId;
        private Integer quantity;
    }

    @Data
    public static class UpdateQuantityRequest {
        private String userId;
        private Integer quantity;
    }
}

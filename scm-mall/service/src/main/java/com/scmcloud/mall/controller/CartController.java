package com.scmcloud.mall.controller;

import com.scmcloud.mall.domain.entity.Cart;
import com.scmcloud.mall.service.ICartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/api/v1/mall/cart")
@Tag(name = "购物车", description = "用户购物车的查询、加购、改数量、删除与清空接口。")
public class CartController {

    private final ICartService cartService;

    @Operation(summary = "查询购物车", description = "获取指定用户的购物车商品列表。")
    @GetMapping
    public List<Cart> getCart(@Parameter(description = "用户标识") @RequestParam String userId) {
        log.info("[API] Get cart: userId={}", userId);
        return cartService.getCart(userId);
    }

    @Operation(summary = "加入购物车", description = "将指定商品 SKU 按数量添加至用户购物车。")
    @PostMapping
    public boolean addToCart(@RequestBody AddToCartRequest request) {
        log.info("[API] Add to cart: userId={}, productId={}, skuId={}, quantity={}",
                request.getUserId(), request.getProductId(), request.getSkuId(), request.getQuantity());
        cartService.addToCart(request.getUserId(), request.getProductId(), request.getSkuId(), request.getQuantity());
        return true;
    }

    @Operation(summary = "修改购物车数量", description = "更新购物车中指定商品行的购买数量。")
    @PutMapping("/{cartItemId}")
    public boolean updateQuantity(@Parameter(description = "购物车条目 ID") @PathVariable Long cartItemId, @RequestBody UpdateQuantityRequest request) {
        log.info("[API] Update quantity: cartItemId={}, quantity={}", cartItemId, request.getQuantity());
        cartService.updateQuantity(request.getUserId(), cartItemId, request.getQuantity());
        return true;
    }

    @Operation(summary = "删除购物车商品", description = "从指定用户的购物车中移除某条商品记录。")
    @DeleteMapping("/{cartItemId}")
    public boolean removeFromCart(@Parameter(description = "购物车条目 ID") @PathVariable Long cartItemId, @Parameter(description = "用户标识") @RequestParam String userId) {
        log.info("[API] Remove from cart: cartItemId={}, userId={}", cartItemId, userId);
        cartService.removeFromCart(userId, cartItemId);
        return true;
    }

    @Operation(summary = "清空购物车", description = "删除指定用户购物车中的全部商品。")
    @DeleteMapping("/clear")
    public boolean clearCart(@Parameter(description = "用户标识") @RequestParam String userId) {
        log.info("[API] Clear cart: userId={}", userId);
        cartService.clearCart(userId);
        return true;
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

package com.scmcloud.mall.controller;

import com.scmcloud.mall.domain.entity.Cart;
import com.scmcloud.mall.service.ICartService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/api/v1/mall/cart")
public class CartController {

    private final ICartService cartService;

    @GetMapping
    public List<Cart> getCart(@RequestParam String userId) {
        log.info("[API] Get cart: userId={}", userId);
        return cartService.getCart(userId);
    }

    @PostMapping
    public boolean addToCart(@RequestBody AddToCartRequest request) {
        log.info("[API] Add to cart: userId={}, productId={}, skuId={}, quantity={}",
                request.getUserId(), request.getProductId(), request.getSkuId(), request.getQuantity());
        cartService.addToCart(request.getUserId(), request.getProductId(), request.getSkuId(), request.getQuantity());
        return true;
    }

    @PutMapping("/{cartItemId}")
    public boolean updateQuantity(@PathVariable Long cartItemId, @RequestBody UpdateQuantityRequest request) {
        log.info("[API] Update quantity: cartItemId={}, quantity={}", cartItemId, request.getQuantity());
        cartService.updateQuantity(request.getUserId(), cartItemId, request.getQuantity());
        return true;
    }

    @DeleteMapping("/{cartItemId}")
    public boolean removeFromCart(@PathVariable Long cartItemId, @RequestParam String userId) {
        log.info("[API] Remove from cart: cartItemId={}, userId={}", cartItemId, userId);
        cartService.removeFromCart(userId, cartItemId);
        return true;
    }

    @DeleteMapping("/clear")
    public boolean clearCart(@RequestParam String userId) {
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

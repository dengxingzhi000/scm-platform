package com.cognora.ecom.mall.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.cognora.ecom.mall.domain.entity.Cart;

import java.util.List;

public interface ICartService extends IService<Cart> {

    List<Cart> getCart(String userId);

    void addToCart(String userId, Long productId, Long skuId, Integer quantity);

    void updateQuantity(String userId, Long cartItemId, Integer quantity);

    void removeFromCart(String userId, Long cartItemId);

    void clearCart(String userId);
}

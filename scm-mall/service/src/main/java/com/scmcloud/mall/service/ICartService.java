package com.scmcloud.mall.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.scmcloud.mall.domain.entity.Cart;

import java.util.List;

public interface ICartService extends IService<Cart> {

    List<Cart> getCart(String userId);

    void addToCart(String userId, Long productId, Long skuId, Integer quantity);

    void updateQuantity(String userId, Long cartItemId, Integer quantity);

    void removeFromCart(String userId, Long cartItemId);

    void clearCart(String userId);
}

package com.scmcloud.mall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.scmcloud.mall.domain.entity.Cart;
import com.scmcloud.mall.mapper.CartMapper;
import com.scmcloud.mall.service.ICartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
@Service
public class CartServiceImpl extends ServiceImpl<CartMapper, Cart> implements ICartService {

    private static final long MAX_CART_QUANTITY = 1_000_000L;

    @Override
    public List<Cart> getCart(String userId) {
        return list(new LambdaQueryWrapper<Cart>()
                .eq(Cart::getUserId, userId)
                .orderByDesc(Cart::getCreatedAt));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addToCart(String userId, Long productId, Long skuId, Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        log.info("Adding to cart: userId={}, productId={}, skuId={}, quantity={}", (userId + "").replace("\r", "_").replace("\n", "_"), (productId + "").replace("\r", "_").replace("\n", "_"), (skuId + "").replace("\r", "_").replace("\n", "_"), (quantity + "").replace("\r", "_").replace("\n", "_"));

        Cart existing = getOne(new LambdaQueryWrapper<Cart>()
                .eq(Cart::getUserId, userId)
                .eq(Cart::getProductId, productId)
                .eq(Cart::getSkuId, skuId));

        if (existing != null) {
            long mergedQuantity = (long) existing.getQuantity() + quantity;
            if (mergedQuantity > MAX_CART_QUANTITY) {
                throw new IllegalArgumentException("Cart quantity exceeds the supported maximum: " + MAX_CART_QUANTITY);
            }
            existing.setQuantity((int) mergedQuantity);
            existing.setUpdatedAt(LocalDateTime.now());
            updateById(existing);
        } else {
            Cart cart = new Cart();
            cart.setUserId(userId);
            cart.setProductId(productId);
            cart.setSkuId(skuId);
            cart.setQuantity(quantity);
            cart.setSelected(true);
            cart.setCreatedAt(LocalDateTime.now());
            cart.setUpdatedAt(LocalDateTime.now());
            save(cart);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateQuantity(String userId, Long cartItemId, Integer quantity) {
        Cart cart = getById(cartItemId);
        if (cart != null && cart.getUserId().equals(userId)) {
            cart.setQuantity(quantity);
            cart.setUpdatedAt(LocalDateTime.now());
            updateById(cart);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeFromCart(String userId, Long cartItemId) {
        Cart cart = getById(cartItemId);
        if (cart != null && cart.getUserId().equals(userId)) {
            removeById(cartItemId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clearCart(String userId) {
        remove(new LambdaQueryWrapper<Cart>().eq(Cart::getUserId, userId));
    }
}

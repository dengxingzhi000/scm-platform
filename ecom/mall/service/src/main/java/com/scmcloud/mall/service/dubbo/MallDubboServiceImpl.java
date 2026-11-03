package com.scmcloud.mall.service.dubbo;

import com.scmcloud.mall.api.MallDubboService;
import com.scmcloud.mall.domain.entity.Seller;
import com.scmcloud.mall.service.ICartService;
import com.scmcloud.mall.service.ISellerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;

@Slf4j
@DubboService
@RequiredArgsConstructor
public class MallDubboServiceImpl implements MallDubboService {

    private final ICartService cartService;
    private final ISellerService sellerService;

    @Override
    public int getCartItemCount(String userId) {
        return cartService.getCart(userId).stream()
                .mapToInt(cart -> cart.getQuantity() == null ? 0 : cart.getQuantity())
                .sum();
    }

    @Override
    public Long registerSeller(String sellerName, String contactPhone) {
        Seller seller = new Seller()
                .setSellerName(sellerName)
                .setContactPhone(contactPhone);
        return sellerService.registerSeller(seller).getId();
    }
}

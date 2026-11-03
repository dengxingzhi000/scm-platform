package com.cognora.ecom.mall.service.dubbo;

import com.cognora.ecom.mall.api.MallDubboService;
import com.cognora.ecom.mall.domain.entity.Seller;
import com.cognora.ecom.mall.service.ICartService;
import com.cognora.ecom.mall.service.ISellerService;
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

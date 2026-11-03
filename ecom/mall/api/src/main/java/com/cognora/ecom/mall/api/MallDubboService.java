package com.cognora.ecom.mall.api;

public interface MallDubboService {

    int getCartItemCount(String userId);

    Long registerSeller(String sellerName, String contactPhone);
}

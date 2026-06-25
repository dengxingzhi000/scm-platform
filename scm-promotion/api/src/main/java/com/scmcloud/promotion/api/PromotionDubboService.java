package com.scmcloud.promotion.api;

import com.scmcloud.promotion.api.dto.CouponVO;
import com.scmcloud.promotion.api.dto.FlashSaleVO;

import java.math.BigDecimal;
import java.util.List;

public interface PromotionDubboService {

    CouponVO issueCoupon(Long userId, Long templateId);

    CouponVO verifyCoupon(Long userId, String couponCode, BigDecimal orderAmount);

    void useCoupon(String couponCode, String orderNo);

    List<CouponVO> getUserCoupons(Long userId);

    boolean checkFlashSaleStock(Long flashSaleId, Long skuId);

    boolean deductFlashSaleStock(Long flashSaleId, Long skuId, Integer quantity);
}

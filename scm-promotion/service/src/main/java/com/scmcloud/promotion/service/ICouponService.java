package com.scmcloud.promotion.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.scmcloud.promotion.domain.entity.Coupon;

import java.math.BigDecimal;
import java.util.List;

public interface ICouponService extends IService<Coupon> {

    Coupon issueCoupon(String userId, Long templateId);

    Coupon verifyCoupon(String userId, String couponCode, BigDecimal orderAmount);

    void useCoupon(String couponCode, String orderNo);

    List<Coupon> getByUserId(String userId);
}

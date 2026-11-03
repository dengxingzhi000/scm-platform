package com.cognora.ecom.promotion.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.IService;
import com.cognora.ecom.promotion.domain.entity.Coupon;

import java.math.BigDecimal;
import java.util.List;

public interface ICouponService extends IService<Coupon> {

    Coupon issueCoupon(String userId, Long templateId);

    Coupon verifyCoupon(String userId, String couponCode, BigDecimal orderAmount);

    void useCoupon(String couponCode, String orderNo);

    List<Coupon> getByUserId(String userId);

    Page<Coupon> pageQuery(int page, int size, String userId, Integer status);
}

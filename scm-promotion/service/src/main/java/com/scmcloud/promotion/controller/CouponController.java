package com.scmcloud.promotion.controller;

import com.scmcloud.promotion.domain.entity.Coupon;
import com.scmcloud.promotion.service.ICouponService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/api/v1/promotions/coupons")
public class CouponController {

    private final ICouponService couponService;

    @PostMapping("/issue")
    public Coupon issueCoupon(@RequestBody IssueCouponRequest request) {
        log.info("[API] Issue coupon: userId={}, templateId={}", request.getUserId(), request.getTemplateId());
        return couponService.issueCoupon(request.getUserId(), request.getTemplateId());
    }

    @PostMapping("/verify")
    public Coupon verifyCoupon(@RequestBody VerifyCouponRequest request) {
        log.info("[API] Verify coupon: userId={}, couponCode={}", request.getUserId(), request.getCouponCode());
        return couponService.verifyCoupon(request.getUserId(), request.getCouponCode(), request.getOrderAmount());
    }

    @PostMapping("/use")
    public boolean useCoupon(@RequestBody UseCouponRequest request) {
        log.info("[API] Use coupon: couponCode={}, orderNo={}", request.getCouponCode(), request.getOrderNo());
        couponService.useCoupon(request.getCouponCode(), request.getOrderNo());
        return true;
    }

    @GetMapping("/user/{userId}")
    public List<Coupon> getByUserId(@PathVariable String userId) {
        log.info("[API] Get user coupons: userId={}", userId);
        return couponService.getByUserId(userId);
    }

    @Data
    public static class IssueCouponRequest {
        private String userId;
        private Long templateId;
    }

    @Data
    public static class VerifyCouponRequest {
        private String userId;
        private String couponCode;
        private BigDecimal orderAmount;
    }

    @Data
    public static class UseCouponRequest {
        private String couponCode;
        private String orderNo;
    }
}

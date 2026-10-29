package com.scmcloud.promotion.controller;

import com.scmcloud.promotion.domain.entity.Coupon;
import com.scmcloud.promotion.service.ICouponService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "优惠券", description = "优惠券的发放、校验、核销及用户券查询接口。")
public class CouponController {

    private final ICouponService couponService;

    @Operation(summary = "发放优惠券", description = "根据优惠券模板向指定用户发放一张优惠券。")
    @PostMapping("/issue")
    public Coupon issueCoupon(@RequestBody IssueCouponRequest request) {
        log.info("[API] Issue coupon: userId={}, templateId={}", (request.getUserId() + "").replace("\r", "_").replace("\n", "_"), (request.getTemplateId() + "").replace("\r", "_").replace("\n", "_"));
        return couponService.issueCoupon(request.getUserId(), request.getTemplateId());
    }

    @Operation(summary = "校验优惠券", description = "校验用户持有的优惠券对指定订单金额是否可用。")
    @PostMapping("/verify")
    public Coupon verifyCoupon(@RequestBody VerifyCouponRequest request) {
        log.info("[API] Verify coupon: userId={}, couponCode={}", (request.getUserId() + "").replace("\r", "_").replace("\n", "_"), (request.getCouponCode() + "").replace("\r", "_").replace("\n", "_"));
        return couponService.verifyCoupon(request.getUserId(), request.getCouponCode(), request.getOrderAmount());
    }

    @Operation(summary = "核销优惠券", description = "将指定优惠券在对应订单上标记为已使用。")
    @PostMapping("/use")
    public boolean useCoupon(@RequestBody UseCouponRequest request) {
        log.info("[API] Use coupon: couponCode={}, orderNo={}", (request.getCouponCode() + "").replace("\r", "_").replace("\n", "_"), (request.getOrderNo() + "").replace("\r", "_").replace("\n", "_"));
        couponService.useCoupon(request.getCouponCode(), request.getOrderNo());
        return true;
    }

    @Operation(summary = "查询用户优惠券", description = "获取指定用户名下的全部优惠券。")
    @GetMapping("/user/{userId}")
    public List<Coupon> getByUserId(@Parameter(description = "用户标识") @PathVariable String userId) {
        log.info("[API] Get user coupons: userId={}", (userId + "").replace("\r", "_").replace("\n", "_"));
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

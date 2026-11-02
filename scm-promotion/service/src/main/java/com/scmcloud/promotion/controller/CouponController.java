package com.scmcloud.promotion.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.promotion.domain.entity.Coupon;
import com.scmcloud.promotion.service.ICouponService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/promotions/coupons")
@Tag(name = "优惠券", description = "优惠券的发放、校验、核销及用户券查询接口。")
public class CouponController {

    private final ICouponService couponService;

    @Operation(summary = "分页查询优惠券", description = "按用户与状态分页查询优惠券。")
    @GetMapping("/page")
    public Page<Coupon> page(
            @Parameter(description = "页码，默认 1") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量，默认 20") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "用户标识") @RequestParam(required = false) String userId,
            @Parameter(description = "状态：1-未使用，2-已使用，3-已过期") @RequestParam(required = false) Integer status) {
        return couponService.pageQuery(page, size, userId, status);
    }

    @Operation(summary = "发放优惠券", description = "根据优惠券模板向指定用户发放一张优惠券；模板库存通过数据库原子扣减。")
    @PostMapping("/issue")
    public Coupon issueCoupon(@Valid @RequestBody IssueCouponRequest request) {
        log.info("[API] Issue coupon: userId={}, templateId={}", (request.getUserId() + "").replace("\r", "_").replace("\n", "_"), (request.getTemplateId() + "").replace("\r", "_").replace("\n", "_"));
        return couponService.issueCoupon(request.getUserId(), request.getTemplateId());
    }

    @Operation(summary = "校验优惠券", description = "校验用户持有的优惠券对指定订单金额是否可用。")
    @PostMapping("/verify")
    public Coupon verifyCoupon(@Valid @RequestBody VerifyCouponRequest request) {
        log.info("[API] Verify coupon: userId={}, couponCode={}", (request.getUserId() + "").replace("\r", "_").replace("\n", "_"), (request.getCouponCode() + "").replace("\r", "_").replace("\n", "_"));
        return couponService.verifyCoupon(request.getUserId(), request.getCouponCode(), request.getOrderAmount());
    }

    @Operation(summary = "核销优惠券", description = "将指定优惠券在对应订单上标记为已使用；同订单重复核销幂等返回。")
    @PostMapping("/use")
    public boolean useCoupon(@Valid @RequestBody UseCouponRequest request) {
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
        @NotBlank(message = "用户标识不能为空")
        private String userId;

        @NotNull(message = "优惠券模板不能为空")
        private Long templateId;
    }

    @Data
    public static class VerifyCouponRequest {
        @NotBlank(message = "用户标识不能为空")
        private String userId;

        @NotBlank(message = "优惠券编码不能为空")
        private String couponCode;

        @NotNull(message = "订单金额不能为空")
        @DecimalMin(value = "0.0", message = "订单金额不能为负数")
        private BigDecimal orderAmount;
    }

    @Data
    public static class UseCouponRequest {
        @NotBlank(message = "优惠券编码不能为空")
        private String couponCode;

        @NotBlank(message = "订单号不能为空")
        private String orderNo;
    }
}

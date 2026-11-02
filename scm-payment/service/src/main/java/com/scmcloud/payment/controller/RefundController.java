package com.scmcloud.payment.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.payment.domain.entity.Refund;
import com.scmcloud.payment.service.IRefundService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RequiredArgsConstructor
@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/payments/refunds")
@Tag(name = "退款管理", description = "提供退款申请、查询、分页、审批通过与驳回接口。")
public class RefundController {

    private final IRefundService refundService;

    @Operation(summary = "分页查询退款", description = "按支付单号、订单号、用户与状态分页查询退款申请。")
    @GetMapping("/page")
    public Page<Refund> page(
            @Parameter(description = "页码，默认 1") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量，默认 20") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "支付单号（模糊）") @RequestParam(required = false) String paymentNo,
            @Parameter(description = "订单号（模糊）") @RequestParam(required = false) String orderNo,
            @Parameter(description = "用户 ID") @RequestParam(required = false) String userId,
            @Parameter(description = "状态：1-待审批，2-退款成功，3-已驳回") @RequestParam(required = false) Integer status) {
        return refundService.pageQuery(page, size, paymentNo, orderNo, userId, status);
    }

    @Operation(summary = "创建退款", description = "根据支付单号、订单号与退款金额创建退款申请。")
    @PostMapping
    public Refund createRefund(@Valid @RequestBody CreateRefundRequest request) {
        log.info("[API] Create refund: paymentNo={}, amount={}",
                (request.getPaymentNo() + "").replace("\r", "_").replace("\n", "_"), (request.getAmount() + "").replace("\r", "_").replace("\n", "_"));
        return refundService.createRefund(request.getPaymentNo(), request.getOrderNo(),
                request.getUserId(), request.getAmount(), request.getReason());
    }

    @Operation(summary = "查询退款", description = "根据退款单号查询退款详情。")
    @GetMapping("/{refundNo}")
    public Refund queryRefund(@Parameter(description = "退款单号") @PathVariable String refundNo) {
        return refundService.queryRefund(refundNo);
    }

    @Operation(summary = "审批通过退款", description = "审批通过指定退款申请并执行退款。")
    @PostMapping("/{refundNo}/approve")
    public Refund approveRefund(@Parameter(description = "退款单号") @PathVariable String refundNo) {
        log.info("[API] Approve refund: refundNo={}", (refundNo + "").replace("\r", "_").replace("\n", "_"));
        return refundService.approveRefund(refundNo);
    }

    @Operation(summary = "驳回退款", description = "驳回指定退款申请并记录驳回原因。")
    @PostMapping("/{refundNo}/reject")
    public Refund rejectRefund(@Parameter(description = "退款单号") @PathVariable String refundNo,
                               @RequestBody(required = false) RejectRequest request) {
        log.info("[API] Reject refund: refundNo={}", (refundNo + "").replace("\r", "_").replace("\n", "_"));
        return refundService.rejectRefund(refundNo, request == null ? null : request.getReason());
    }

    @Data
    public static class CreateRefundRequest {
        @NotBlank(message = "支付单号不能为空")
        private String paymentNo;

        @NotBlank(message = "订单号不能为空")
        private String orderNo;

        @NotBlank(message = "用户 ID 不能为空")
        private String userId;

        @NotNull(message = "退款金额不能为空")
        @DecimalMin(value = "0.01", message = "退款金额必须大于 0")
        private BigDecimal amount;

        private String reason;
    }

    @Data
    public static class RejectRequest {
        private String reason;
    }
}

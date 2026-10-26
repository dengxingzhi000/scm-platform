package com.scmcloud.payment.controller;

import com.scmcloud.payment.domain.entity.Refund;
import com.scmcloud.payment.service.IRefundService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/api/v1/payments/refunds")
@Tag(name = "退款管理", description = "提供退款申请、查询、审批通过与驳回接口。")
public class RefundController {

    private final IRefundService refundService;

    @Operation(summary = "创建退款", description = "根据支付单号、订单号与退款金额创建退款申请。")
    @PostMapping
    public Refund createRefund(@RequestBody CreateRefundRequest request) {
        log.info("[API] Create refund: paymentNo={}, amount={}",
                request.getPaymentNo(), request.getAmount());
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
        log.info("[API] Approve refund: refundNo={}", refundNo);
        return refundService.approveRefund(refundNo);
    }

    @Operation(summary = "驳回退款", description = "驳回指定退款申请并记录驳回原因。")
    @PostMapping("/{refundNo}/reject")
    public Refund rejectRefund(@Parameter(description = "退款单号") @PathVariable String refundNo,
                               @RequestBody(required = false) RejectRequest request) {
        log.info("[API] Reject refund: refundNo={}", refundNo);
        return refundService.rejectRefund(refundNo, request == null ? null : request.getReason());
    }

    @Data
    public static class CreateRefundRequest {
        private String paymentNo;
        private String orderNo;
        private String userId;
        private BigDecimal amount;
        private String reason;
    }

    @Data
    public static class RejectRequest {
        private String reason;
    }
}

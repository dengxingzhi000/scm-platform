package com.scmcloud.payment.controller;

import com.scmcloud.payment.domain.entity.Refund;
import com.scmcloud.payment.service.IRefundService;
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
public class RefundController {

    private final IRefundService refundService;

    @PostMapping
    public Refund createRefund(@RequestBody CreateRefundRequest request) {
        log.info("[API] Create refund: paymentNo={}, amount={}",
                request.getPaymentNo(), request.getAmount());
        return refundService.createRefund(request.getPaymentNo(), request.getOrderNo(),
                request.getUserId(), request.getAmount(), request.getReason());
    }

    @GetMapping("/{refundNo}")
    public Refund queryRefund(@PathVariable String refundNo) {
        return refundService.queryRefund(refundNo);
    }

    @PostMapping("/{refundNo}/approve")
    public Refund approveRefund(@PathVariable String refundNo) {
        log.info("[API] Approve refund: refundNo={}", refundNo);
        return refundService.approveRefund(refundNo);
    }

    @PostMapping("/{refundNo}/reject")
    public Refund rejectRefund(@PathVariable String refundNo,
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

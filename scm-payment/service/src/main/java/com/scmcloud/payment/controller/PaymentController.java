package com.scmcloud.payment.controller;

import com.scmcloud.payment.domain.entity.PaymentOrder;
import com.scmcloud.payment.service.IPaymentOrderService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final IPaymentOrderService paymentService;

    @PostMapping("/create")
    public PaymentOrder createPayment(@RequestBody CreatePaymentRequest request) {
        log.info("[API] Create payment: orderNo={}, amount={}", request.getOrderNo(), request.getAmount());
        return paymentService.createPayment(request.getOrderNo(), request.getUserId(), request.getAmount(), request.getPaymentChannel());
    }

    @GetMapping("/{paymentNo}")
    public PaymentOrder queryPayment(@PathVariable String paymentNo) {
        log.info("[API] Query payment: paymentNo={}", paymentNo);
        return paymentService.queryPayment(paymentNo);
    }

    @PostMapping("/{paymentNo}/cancel")
    public boolean cancelPayment(@PathVariable String paymentNo) {
        log.info("[API] Cancel payment: paymentNo={}", paymentNo);
        paymentService.cancelPayment(paymentNo);
        return true;
    }

    @PostMapping("/callback/alipay")
    public String alipayCallback(@RequestBody String callbackData) {
        log.info("[API] Alipay callback received");
        return "success";
    }

    @PostMapping("/callback/wechat")
    public String wechatCallback(@RequestBody String callbackData) {
        log.info("[API] WeChat callback received");
        return "success";
    }

    @Data
    public static class CreatePaymentRequest {
        private String orderNo;
        private String userId;
        private BigDecimal amount;
        private String paymentChannel;
    }
}

package com.scmcloud.payment.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scmcloud.payment.domain.entity.PaymentOrder;
import com.scmcloud.payment.service.IPaymentOrderService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final IPaymentOrderService paymentService;
    private final ObjectMapper objectMapper;

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
        Map<String, String> payload = parseCallback(callbackData);
        String paymentNo = firstNonBlank(payload.get("out_trade_no"), payload.get("paymentNo"));
        String transactionId = firstNonBlank(payload.get("trade_no"), payload.get("transaction_id"));
        String status = firstNonBlank(payload.get("trade_status"), payload.get("result_code"));
        boolean success = "TRADE_SUCCESS".equalsIgnoreCase(status)
                || "TRADE_FINISHED".equalsIgnoreCase(status)
                || "SUCCESS".equalsIgnoreCase(status);
        return completeCallback("alipay", paymentNo, transactionId, success);
    }

    @PostMapping("/callback/wechat")
    public String wechatCallback(@RequestBody String callbackData) {
        log.info("[API] WeChat callback received");
        Map<String, String> payload = parseCallback(callbackData);
        String paymentNo = firstNonBlank(payload.get("out_trade_no"), payload.get("paymentNo"));
        String transactionId = firstNonBlank(payload.get("transaction_id"), payload.get("trade_no"));
        String status = firstNonBlank(payload.get("result_code"), payload.get("return_code"));
        boolean success = "SUCCESS".equalsIgnoreCase(status);
        return completeCallback("wechat", paymentNo, transactionId, success);
    }

    private String completeCallback(String channel, String paymentNo,
                                    String transactionId, boolean success) {
        if (paymentNo == null || paymentNo.isBlank()) {
            log.warn("[API] {} callback without payment number", channel);
            return "failure";
        }
        if (!success) {
            log.info("[API] {} callback ignored for {}: non-success status", channel, paymentNo);
            return "success";
        }
        try {
            paymentService.handlePaymentCallback(paymentNo, transactionId);
            return "success";
        } catch (RuntimeException ex) {
            log.error("[API] {} callback failed for {}", channel, paymentNo, ex);
            return "failure";
        }
    }

    private Map<String, String> parseCallback(String raw) {
        Map<String, String> values = new HashMap<>();
        if (raw == null || raw.isBlank()) {
            return values;
        }
        String trimmed = raw.trim();
        if (trimmed.startsWith("{")) {
            try {
                JsonNode node = objectMapper.readTree(trimmed);
                node.fields().forEachRemaining(entry -> {
                    if (entry.getValue() != null && !entry.getValue().isNull()) {
                        values.put(entry.getKey(), entry.getValue().asText());
                    }
                });
                return values;
            } catch (Exception ex) {
                log.warn("[API] Unable to parse JSON payment callback: {}", ex.getMessage());
            }
        }
        for (String pair : trimmed.split("&")) {
            int index = pair.indexOf('=');
            if (index <= 0) {
                continue;
            }
            String key = URLDecoder.decode(pair.substring(0, index), StandardCharsets.UTF_8);
            String value = URLDecoder.decode(pair.substring(index + 1), StandardCharsets.UTF_8);
            values.put(key, value);
        }
        return values;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    @Data
    public static class CreatePaymentRequest {
        private String orderNo;
        private String userId;
        private BigDecimal amount;
        private String paymentChannel;
    }
}

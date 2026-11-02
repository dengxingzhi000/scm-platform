package com.scmcloud.payment.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scmcloud.payment.domain.entity.PaymentOrder;
import com.scmcloud.payment.service.IPaymentOrderService;
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
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/payments")
@Tag(name = "支付管理", description = "提供支付单创建、查询、分页、取消与支付渠道回调处理接口。")
public class PaymentController {

    private final IPaymentOrderService paymentService;
    private final ObjectMapper objectMapper;

    @Operation(summary = "分页查询支付单", description = "按订单号、用户、渠道与状态分页查询支付单。")
    @GetMapping("/page")
    public Page<PaymentOrder> page(
            @Parameter(description = "页码，默认 1") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量，默认 20") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "订单号（模糊）") @RequestParam(required = false) String orderNo,
            @Parameter(description = "用户 ID") @RequestParam(required = false) String userId,
            @Parameter(description = "支付渠道") @RequestParam(required = false) String paymentChannel,
            @Parameter(description = "状态：1-待支付，2-已支付，3-失败，4-已取消") @RequestParam(required = false) Integer status) {
        return paymentService.pageQuery(page, size, orderNo, userId, paymentChannel, status);
    }

    @Operation(summary = "创建支付单", description = "根据订单号、用户、金额与支付渠道创建支付单；同订单同渠道重复请求返回已有待支付单。")
    @PostMapping("/create")
    public PaymentOrder createPayment(@Valid @RequestBody CreatePaymentRequest request) {
        log.info("[API] Create payment: orderNo={}, amount={}", (request.getOrderNo() + "").replace("\r", "_").replace("\n", "_"), (request.getAmount() + "").replace("\r", "_").replace("\n", "_"));
        return paymentService.createPayment(request.getOrderNo(), request.getUserId(), request.getAmount(), request.getPaymentChannel());
    }

    @Operation(summary = "查询支付单", description = "根据支付单号查询支付单详情。")
    @GetMapping("/{paymentNo}")
    public PaymentOrder queryPayment(@Parameter(description = "支付单号") @PathVariable String paymentNo) {
        log.info("[API] Query payment: paymentNo={}", (paymentNo + "").replace("\r", "_").replace("\n", "_"));
        return paymentService.queryPayment(paymentNo);
    }

    @Operation(summary = "取消支付单", description = "根据支付单号取消尚未完成的支付。")
    @PostMapping("/{paymentNo}/cancel")
    public boolean cancelPayment(@Parameter(description = "支付单号") @PathVariable String paymentNo) {
        log.info("[API] Cancel payment: paymentNo={}", (paymentNo + "").replace("\r", "_").replace("\n", "_"));
        paymentService.cancelPayment(paymentNo);
        return true;
    }

    @Operation(summary = "支付宝支付回调", description = "接收支付宝异步通知并完成支付结果处理。")
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

    @Operation(summary = "微信支付回调", description = "接收微信支付异步通知并完成支付结果处理。")
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
            log.info("[API] {} callback ignored for {}: non-success status", (channel + "").replace("\r", "_").replace("\n", "_"), (paymentNo + "").replace("\r", "_").replace("\n", "_"));
            return "success";
        }
        try {
            paymentService.handlePaymentCallback(paymentNo, transactionId);
            return "success";
        } catch (RuntimeException ex) {
            log.error("[API] {} callback failed for {}", (channel + "").replace("\r", "_").replace("\n", "_"), (paymentNo + "").replace("\r", "_").replace("\n", "_"), ex);
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
                node.properties().forEach(entry -> {
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
        @NotBlank(message = "订单号不能为空")
        private String orderNo;

        @NotBlank(message = "用户 ID 不能为空")
        private String userId;

        @NotNull(message = "支付金额不能为空")
        @DecimalMin(value = "0.01", message = "支付金额必须大于 0")
        private BigDecimal amount;

        @NotBlank(message = "支付渠道不能为空")
        private String paymentChannel;
    }
}

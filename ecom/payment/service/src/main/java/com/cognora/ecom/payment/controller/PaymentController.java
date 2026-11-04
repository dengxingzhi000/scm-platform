package com.cognora.ecom.payment.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.cognora.ecom.payment.callback.PaymentCallbackException;
import com.cognora.ecom.payment.callback.PaymentCallbackVerifier;
import com.cognora.ecom.payment.callback.VerifiedPayment;
import com.cognora.ecom.payment.domain.PaymentAmount;
import com.cognora.ecom.payment.domain.entity.PaymentOrder;
import com.cognora.ecom.payment.service.IPaymentOrderService;
import com.cognora.platform.common.web.ownership.OwnershipGuard;
import com.cognora.platform.common.web.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
    private final PaymentCallbackVerifier callbackVerifier;

    @Operation(summary = "分页查询支付单", description = "按订单号、用户、渠道与状态分页查询支付单。")
    @GetMapping("/page")
    public Page<PaymentOrder> page(
            @Parameter(description = "页码，默认 1") @RequestParam(defaultValue = "1") @Min(1) int page,
            @Parameter(description = "每页数量，默认 20，最大 100") @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @Parameter(description = "订单号（模糊）") @RequestParam(required = false) String orderNo,
            @Parameter(description = "用户 ID") @RequestParam(required = false) String userId,
            @Parameter(description = "支付渠道") @RequestParam(required = false) String paymentChannel,
            @Parameter(description = "状态：1-待支付，2-已支付，3-失败，4-已取消") @RequestParam(required = false) Integer status) {
        return paymentService.pageQuery(page, size, orderNo, userId, paymentChannel, status);
    }

    @Operation(summary = "创建支付单", description = "根据订单号、当前登录用户、金额与支付渠道创建支付单；同订单同渠道重复请求返回已有待支付单。")
    @PostMapping("/create")
    public PaymentOrder createPayment(@Valid @RequestBody CreatePaymentRequest request) {
        // The owner always comes from the authenticated principal, never from the request body.
        String currentUserId = SecurityUtils.requireCurrentUserUuid().toString();
        log.info("[API] Create payment: userId={}, orderNo={}, amount={}", (currentUserId + "").replace("\r", "_").replace("\n", "_"),
                (request.getOrderNo() + "").replace("\r", "_").replace("\n", "_"), (request.getAmount() + "").replace("\r", "_").replace("\n", "_"));
        return paymentService.createPayment(request.getOrderNo(), currentUserId, request.getAmount(), request.getPaymentChannel());
    }

    @Operation(summary = "查询支付单", description = "根据支付单号查询当前登录用户的支付单详情。")
    @GetMapping("/{paymentNo}")
    public PaymentOrder queryPayment(@Parameter(description = "支付单号") @PathVariable String paymentNo) {
        log.info("[API] Query payment: paymentNo={}", (paymentNo + "").replace("\r", "_").replace("\n", "_"));
        PaymentOrder payment = paymentService.queryPayment(paymentNo);
        requirePaymentOwner(payment);
        return payment;
    }

    @Operation(summary = "取消支付单", description = "根据支付单号取消当前登录用户尚未完成的支付。")
    @PostMapping("/{paymentNo}/cancel")
    public boolean cancelPayment(@Parameter(description = "支付单号") @PathVariable String paymentNo) {
        log.info("[API] Cancel payment: paymentNo={}", (paymentNo + "").replace("\r", "_").replace("\n", "_"));
        PaymentOrder payment = paymentService.queryPayment(paymentNo);
        requirePaymentOwner(payment);
        paymentService.cancelPayment(paymentNo);
        return true;
    }

    private void requirePaymentOwner(PaymentOrder payment) {
        if (payment == null) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Access denied: payment not found");
        }
        OwnershipGuard.requireSelfOrThrow(payment.getUserId());
    }

    @Operation(summary = "支付宝支付回调", description = "验签支付宝异步通知，校验商户号与本地支付单金额后完成支付。")
    @PostMapping("/callback/alipay")
    public String alipayCallback(@RequestBody(required = false) String callbackData) {
        log.info("[API] Alipay callback received");
        Map<String, String> payload = parseCallback(callbackData);
        try {
            VerifiedPayment verified = callbackVerifier.verifyAlipay(payload);
            return completeCallback("alipay", verified);
        } catch (PaymentCallbackException ex) {
            log.error("[API] Rejecting Alipay callback: {}", ex.getMessage());
            return "failure";
        }
    }

    @Operation(summary = "微信支付回调", description = "验签并解密微信支付 v3 通知，校验商户号与本地支付单金额后完成支付。")
    @PostMapping("/callback/wechat")
    public String wechatCallback(
            @RequestBody(required = false) String callbackData,
            @RequestHeader(value = "Wechatpay-Timestamp", required = false) String timestamp,
            @RequestHeader(value = "Wechatpay-Nonce", required = false) String nonce,
            @RequestHeader(value = "Wechatpay-Signature", required = false) String signature,
            @RequestHeader(value = "Wechatpay-Serial", required = false) String serial) {
        log.info("[API] WeChat callback received");
        try {
            VerifiedPayment verified = callbackVerifier.verifyWechat(
                    callbackData, timestamp, nonce, signature, serial);
            return completeCallback("wechat", verified);
        } catch (PaymentCallbackException ex) {
            log.error("[API] Rejecting WeChat callback: {}", ex.getMessage());
            return "failure";
        }
    }

    private String completeCallback(String channel, VerifiedPayment verified) {
        if (!verified.success()) {
            log.info("[API] {} callback acknowledged for {}: non-success status",
                    channel, safe(verified.paymentNo()));
            return "success";
        }
        try {
            paymentService.handleVerifiedPaymentCallback(
                    verified.paymentNo(), verified.transactionId(),
                    verified.paidAmount(), channel);
            return "success";
        } catch (RuntimeException ex) {
            log.error("[API] {} callback failed for {}", channel, safe(verified.paymentNo()), ex);
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

    private String safe(String value) {
        return (value + "").replace("\r", "_").replace("\n", "_");
    }

    @Data
    public static class CreatePaymentRequest {
        @NotBlank(message = "订单号不能为空")
        private String orderNo;

        /**
         * Retained for backward compatibility; the authenticated user id always wins.
         */
        private String userId;

        @NotNull(message = "支付金额不能为空")
        @DecimalMin(value = PaymentAmount.MIN_AMOUNT, message = "支付金额必须大于 0")
        @DecimalMax(value = PaymentAmount.MAX_AMOUNT, message = "支付金额超出上限")
        private BigDecimal amount;

        @NotBlank(message = "支付渠道不能为空")
        private String paymentChannel;
    }
}

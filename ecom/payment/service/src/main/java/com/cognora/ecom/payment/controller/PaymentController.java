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
@Tag(name = "鏀粯绠＄悊", description = "鎻愪緵鏀粯鍗曞垱寤恒€佹煡璇€佸垎椤点€佸彇娑堜笌鏀粯娓犻亾鍥炶皟澶勭悊鎺ュ彛銆?)
public class PaymentController {

    private final IPaymentOrderService paymentService;
    private final ObjectMapper objectMapper;
    private final PaymentCallbackVerifier callbackVerifier;

    @Operation(summary = "鍒嗛〉鏌ヨ鏀粯鍗?, description = "鎸夎鍗曞彿銆佺敤鎴枫€佹笭閬撲笌鐘舵€佸垎椤垫煡璇㈡敮浠樺崟銆?)
    @GetMapping("/page")
    public Page<PaymentOrder> page(
            @Parameter(description = "椤电爜锛岄粯璁?1") @RequestParam(defaultValue = "1") @Min(1) int page,
            @Parameter(description = "姣忛〉鏁伴噺锛岄粯璁?20锛屾渶澶?100") @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @Parameter(description = "璁㈠崟鍙凤紙妯＄硦锛?) @RequestParam(required = false) String orderNo,
            @Parameter(description = "鐢ㄦ埛 ID") @RequestParam(required = false) String userId,
            @Parameter(description = "鏀粯娓犻亾") @RequestParam(required = false) String paymentChannel,
            @Parameter(description = "鐘舵€侊細1-寰呮敮浠橈紝2-宸叉敮浠橈紝3-澶辫触锛?-宸插彇娑?) @RequestParam(required = false) Integer status) {
        return paymentService.pageQuery(page, size, orderNo, userId, paymentChannel, status);
    }

    @Operation(summary = "鍒涘缓鏀粯鍗?, description = "鏍规嵁璁㈠崟鍙枫€佺敤鎴枫€侀噾棰濅笌鏀粯娓犻亾鍒涘缓鏀粯鍗曪紱鍚岃鍗曞悓娓犻亾閲嶅璇锋眰杩斿洖宸叉湁寰呮敮浠樺崟銆?)
    @PostMapping("/create")
    public PaymentOrder createPayment(@Valid @RequestBody CreatePaymentRequest request) {
        log.info("[API] Create payment: orderNo={}, amount={}", (request.getOrderNo() + "").replace("\r", "_").replace("\n", "_"), (request.getAmount() + "").replace("\r", "_").replace("\n", "_"));
        return paymentService.createPayment(request.getOrderNo(), request.getUserId(), request.getAmount(), request.getPaymentChannel());
    }

    @Operation(summary = "鏌ヨ鏀粯鍗?, description = "鏍规嵁鏀粯鍗曞彿鏌ヨ鏀粯鍗曡鎯呫€?)
    @GetMapping("/{paymentNo}")
    public PaymentOrder queryPayment(@Parameter(description = "鏀粯鍗曞彿") @PathVariable String paymentNo) {
        log.info("[API] Query payment: paymentNo={}", (paymentNo + "").replace("\r", "_").replace("\n", "_"));
        return paymentService.queryPayment(paymentNo);
    }

    @Operation(summary = "鍙栨秷鏀粯鍗?, description = "鏍规嵁鏀粯鍗曞彿鍙栨秷灏氭湭瀹屾垚鐨勬敮浠樸€?)
    @PostMapping("/{paymentNo}/cancel")
    public boolean cancelPayment(@Parameter(description = "鏀粯鍗曞彿") @PathVariable String paymentNo) {
        log.info("[API] Cancel payment: paymentNo={}", (paymentNo + "").replace("\r", "_").replace("\n", "_"));
        paymentService.cancelPayment(paymentNo);
        return true;
    }

    @Operation(summary = "鏀粯瀹濇敮浠樺洖璋?, description = "楠岀鏀粯瀹濆紓姝ラ€氱煡锛屾牎楠屽晢鎴峰彿涓庢湰鍦版敮浠樺崟閲戦鍚庡畬鎴愭敮浠樸€?)
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

    @Operation(summary = "寰俊鏀粯鍥炶皟", description = "楠岀骞惰В瀵嗗井淇℃敮浠?v3 閫氱煡锛屾牎楠屽晢鎴峰彿涓庢湰鍦版敮浠樺崟閲戦鍚庡畬鎴愭敮浠樸€?)
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
        @NotBlank(message = "璁㈠崟鍙蜂笉鑳戒负绌?)
        private String orderNo;

        @NotBlank(message = "鐢ㄦ埛 ID 涓嶈兘涓虹┖")
        private String userId;

        @NotNull(message = "鏀粯閲戦涓嶈兘涓虹┖")
        @DecimalMin(value = PaymentAmount.MIN_AMOUNT, message = "鏀粯閲戦蹇呴』澶т簬 0")
        @DecimalMax(value = PaymentAmount.MAX_AMOUNT, message = "鏀粯閲戦瓒呭嚭涓婇檺")
        private BigDecimal amount;

        @NotBlank(message = "鏀粯娓犻亾涓嶈兘涓虹┖")
        private String paymentChannel;
    }
}

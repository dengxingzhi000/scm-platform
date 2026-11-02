package com.scmcloud.payment.callback;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Authenticates Alipay (RSA2) and WeChat Pay v3 callbacks before their payload
 * is allowed to change a payment. Missing configuration is a hard failure:
 * callbacks are rejected rather than accepted unverified.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentCallbackVerifier {

    private static final Duration MAX_WECHAT_CLOCK_SKEW = Duration.ofMinutes(5);
    private static final String ALIPAY_SIGN_TYPE = "RSA2";
    private static final String WECHAT_TRADE_SUCCESS = "SUCCESS";

    private final PaymentProperties properties;
    private final ObjectMapper objectMapper;

    /**
     * Verifies the RSA2 signature over the form parameters and checks that the
     * notification was issued for this platform's Alipay app.
     */
    public VerifiedPayment verifyAlipay(Map<String, String> params) {
        requireAlipayConfiguration();
        if (!verifyAlipaySignature(params)) {
            throw new PaymentCallbackException("Alipay callback signature verification failed");
        }

        String appId = params.get("app_id");
        if (!equalsIgnoreCase(appId, properties.getAlipay().getAppId())) {
            throw new PaymentCallbackException("Alipay callback app_id does not match configured app");
        }
        String sellerId = properties.getAlipay().getSellerId();
        if (StringUtils.hasText(sellerId) && !equalsIgnoreCase(params.get("seller_id"), sellerId)) {
            throw new PaymentCallbackException("Alipay callback seller_id does not match configured seller");
        }

        String status = firstNonBlank(params.get("trade_status"), params.get("result_code"));
        boolean success = "TRADE_SUCCESS".equalsIgnoreCase(status)
                || "TRADE_FINISHED".equalsIgnoreCase(status)
                || "SUCCESS".equalsIgnoreCase(status);
        String paymentNo = firstNonBlank(params.get("out_trade_no"), params.get("paymentNo"));
        String transactionId = firstNonBlank(params.get("trade_no"), params.get("transaction_id"));

        BigDecimal paidAmount = null;
        if (success) {
            requireValue(paymentNo, "Alipay callback has no out_trade_no");
            requireValue(transactionId, "Alipay callback has no trade_no");
            paidAmount = parseAmount(params.get("total_amount"), "Alipay callback has no valid total_amount");
        }
        return new VerifiedPayment(paymentNo, transactionId, paidAmount, appId, success);
    }

    /**
     * Verifies the Wechatpay-Signature header over the raw body, then decrypts
     * resource.ciphertext and checks the merchant id and amount.
     */
    public VerifiedPayment verifyWechat(String body, String timestamp, String nonce,
                                        String signature, String serial) {
        requireWechatConfiguration();
        requireValue(body, "WeChat callback body is empty");
        requireValue(timestamp, "Wechatpay-Timestamp header is missing");
        requireValue(nonce, "Wechatpay-Nonce header is missing");
        requireValue(signature, "Wechatpay-Signature header is missing");

        long nowSeconds = System.currentTimeMillis() / 1000;
        long callbackSeconds;
        try {
            callbackSeconds = Long.parseLong(timestamp);
        } catch (NumberFormatException e) {
            throw new PaymentCallbackException("Wechatpay-Timestamp header is not a number", e);
        }
        if (Math.abs(nowSeconds - callbackSeconds) > MAX_WECHAT_CLOCK_SKEW.toSeconds()) {
            throw new PaymentCallbackException("WeChat callback timestamp is outside the allowed window");
        }

        String expectedSerial = properties.getWechat().getPlatformCertificateSerial();
        if (StringUtils.hasText(expectedSerial) && !equalsIgnoreCase(serial, expectedSerial)) {
            throw new PaymentCallbackException("Wechatpay-Serial does not match the configured platform certificate");
        }

        PublicKey platformKey = loadWechatPlatformKey();
        String signedContent = timestamp + "\n" + nonce + "\n" + body + "\n";
        if (!rsaVerify(signedContent, signature, platformKey, StandardCharsets.UTF_8)) {
            throw new PaymentCallbackException("WeChat callback signature verification failed");
        }

        JsonNode envelope;
        try {
            envelope = objectMapper.readTree(body);
        } catch (Exception e) {
            throw new PaymentCallbackException("WeChat callback body is not valid JSON", e);
        }
        JsonNode resource = envelope.path("resource");
        if (resource.isMissingNode() || resource.isNull()) {
            throw new PaymentCallbackException("WeChat callback has no resource object");
        }

        JsonNode payload = decryptResource(resource);
        String merchantId = text(payload, "mchid");
        if (!equalsIgnoreCase(merchantId, properties.getWechat().getMchId())) {
            throw new PaymentCallbackException("WeChat callback mchid does not match configured merchant");
        }

        String tradeState = text(payload, "trade_state");
        boolean success = WECHAT_TRADE_SUCCESS.equalsIgnoreCase(tradeState);
        String paymentNo = text(payload, "out_trade_no");
        String transactionId = text(payload, "transaction_id");

        BigDecimal paidAmount = null;
        JsonNode total = payload.path("amount").path("total");
        if (total.isNumber()) {
            paidAmount = BigDecimal.valueOf(total.asLong(), 2);
        }
        if (success) {
            requireValue(paymentNo, "WeChat callback has no out_trade_no");
            requireValue(transactionId, "WeChat callback has no transaction_id");
            if (paidAmount == null) {
                throw new PaymentCallbackException("WeChat callback has no valid amount.total");
            }
        }
        return new VerifiedPayment(paymentNo, transactionId, paidAmount, merchantId, success);
    }

    private void requireAlipayConfiguration() {
        if (!StringUtils.hasText(properties.getAlipay().getPublicKey())
                || !StringUtils.hasText(properties.getAlipay().getAppId())) {
            throw new PaymentCallbackException(
                    "Alipay callback verification is not configured: set payment.alipay.public-key and payment.alipay.app-id");
        }
    }

    private void requireWechatConfiguration() {
        if (!StringUtils.hasText(properties.getWechat().getPlatformCertificate())
                || !StringUtils.hasText(properties.getWechat().getApiV3Key())
                || !StringUtils.hasText(properties.getWechat().getMchId())) {
            throw new PaymentCallbackException(
                    "WeChat callback verification is not configured: set payment.wechat.platform-certificate, "
                            + "payment.wechat.api-v3-key and payment.wechat.mch-id");
        }
    }

    private boolean verifyAlipaySignature(Map<String, String> params) {
        String sign = params.get("sign");
        if (!StringUtils.hasText(sign)) {
            return false;
        }
        String signType = params.get("sign_type");
        if (StringUtils.hasText(signType) && !ALIPAY_SIGN_TYPE.equalsIgnoreCase(signType)) {
            return false;
        }

        String canonical = params.entrySet().stream()
                .filter(entry -> !"sign".equals(entry.getKey()) && !"sign_type".equals(entry.getKey()))
                .filter(entry -> entry.getValue() != null && !entry.getValue().isEmpty())
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("&"));
        if (canonical.isEmpty()) {
            return false;
        }

        String charset = properties.getAlipay().getCharset();
        Charset javaCharset = StringUtils.hasText(charset) ? Charset.forName(charset) : StandardCharsets.UTF_8;
        try {
            return rsaVerify(canonical, sign, loadPublicKey(properties.getAlipay().getPublicKey()), javaCharset);
        } catch (PaymentCallbackException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Unable to verify Alipay callback signature: {}", e.getMessage());
            return false;
        }
    }

    private PublicKey loadWechatPlatformKey() {
        String value = properties.getWechat().getPlatformCertificate();
        if (value.contains("BEGIN CERTIFICATE")) {
            try (ByteArrayInputStream in = new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8))) {
                CertificateFactory factory = CertificateFactory.getInstance("X.509");
                X509Certificate certificate = (X509Certificate) factory.generateCertificate(in);
                return certificate.getPublicKey();
            } catch (Exception e) {
                throw new PaymentCallbackException("WeChat platform certificate cannot be parsed", e);
            }
        }
        return loadPublicKey(value);
    }

    private JsonNode decryptResource(JsonNode resource) {
        String ciphertext = text(resource, "ciphertext");
        String nonce = text(resource, "nonce");
        String associatedData = text(resource, "associated_data");
        if (!StringUtils.hasText(ciphertext) || !StringUtils.hasText(nonce)) {
            throw new PaymentCallbackException("WeChat callback resource is missing ciphertext or nonce");
        }
        byte[] keyBytes = properties.getWechat().getApiV3Key().getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length != 32) {
            throw new PaymentCallbackException("payment.wechat.api-v3-key must be exactly 32 bytes");
        }
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec spec = new GCMParameterSpec(128, nonce.getBytes(StandardCharsets.UTF_8));
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(keyBytes, "AES"), spec);
            if (StringUtils.hasText(associatedData)) {
                cipher.updateAAD(associatedData.getBytes(StandardCharsets.UTF_8));
            }
            byte[] plain = cipher.doFinal(Base64.getDecoder().decode(ciphertext));
            return objectMapper.readTree(new String(plain, StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new PaymentCallbackException("WeChat callback resource cannot be decrypted", e);
        }
    }

    private boolean rsaVerify(String content, String signatureBase64, PublicKey key, Charset charset) {
        try {
            Signature verifier = Signature.getInstance("SHA256withRSA");
            verifier.initVerify(key);
            verifier.update(content.getBytes(charset));
            return verifier.verify(Base64.getDecoder().decode(signatureBase64));
        } catch (Exception e) {
            log.warn("Unable to verify callback RSA signature: {}", e.getMessage());
            return false;
        }
    }

    private PublicKey loadPublicKey(String pem) {
        try {
            String normalized = pem
                    .replaceAll("-----BEGIN (RSA )?PUBLIC KEY-----", "")
                    .replaceAll("-----END (RSA )?PUBLIC KEY-----", "")
                    .replaceAll("\\s", "");
            byte[] der = Base64.getDecoder().decode(normalized);
            return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(der));
        } catch (Exception e) {
            throw new PaymentCallbackException("Configured channel public key cannot be parsed", e);
        }
    }

    private BigDecimal parseAmount(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new PaymentCallbackException(message);
        }
        try {
            BigDecimal amount = new BigDecimal(value.trim());
            if (amount.signum() < 0) {
                throw new PaymentCallbackException(message);
            }
            return amount;
        } catch (NumberFormatException e) {
            throw new PaymentCallbackException(message, e);
        }
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? null : value.asText();
    }

    private void requireValue(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new PaymentCallbackException(message);
        }
    }

    private boolean equalsIgnoreCase(String left, String right) {
        return left != null && right != null && left.equalsIgnoreCase(right);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }
}

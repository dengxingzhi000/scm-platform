package com.cognora.ecom.payment.callback;

import com.fasterxml.jackson.databind.ObjectMapper;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/** Shared RSA/AES fixtures for callback verification tests. */
public final class PaymentCallbackTestSupport {

    public static final String API_V3_KEY = "0123456789abcdef0123456789abcdef";
    public static final String APP_ID = "app-1";
    public static final String SELLER_ID = "seller-1";
    public static final String MCH_ID = "mch-1";
    /** Alipay renders notify_time in the merchant's local time. */
    public static final ZoneId ALIPAY_ZONE = ZoneId.of("Asia/Shanghai");
    public static final DateTimeFormatter ALIPAY_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public static final KeyPair KEY_PAIR = generateKeyPair();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private PaymentCallbackTestSupport() {
    }

    public static PaymentProperties properties() {
        PaymentProperties properties = new PaymentProperties();
        properties.getAlipay().setPublicKey(Base64.getEncoder().encodeToString(
                KEY_PAIR.getPublic().getEncoded()));
        properties.getAlipay().setAppId(APP_ID);
        properties.getAlipay().setSellerId(SELLER_ID);
        properties.getAlipay().setTimezone(ALIPAY_ZONE.getId());
        properties.getWechat().setPlatformCertificate(Base64.getEncoder().encodeToString(
                KEY_PAIR.getPublic().getEncoded()));
        properties.getWechat().setApiV3Key(API_V3_KEY);
        properties.getWechat().setMchId(MCH_ID);
        return properties;
    }

    public static Map<String, String> alipayParams(String paymentNo, String transactionId,
                                                   String amount, String status) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("out_trade_no", paymentNo);
        params.put("trade_no", transactionId);
        params.put("trade_status", status);
        params.put("total_amount", amount);
        params.put("app_id", APP_ID);
        params.put("seller_id", SELLER_ID);
        params.put("sign_type", "RSA2");
        params.put("notify_time", alipayNotifyTime(Instant.now()));
        return params;
    }

    /** Renders an instant the way Alipay renders {@code notify_time}. */
    public static String alipayNotifyTime(Instant instant) {
        return LocalDateTime.ofInstant(instant, ALIPAY_ZONE).format(ALIPAY_TIME_FORMAT);
    }

    public static String signAlipay(Map<String, String> params) {
        String canonical = params.entrySet().stream()
                .filter(entry -> !"sign".equals(entry.getKey()) && !"sign_type".equals(entry.getKey()))
                .filter(entry -> entry.getValue() != null && !entry.getValue().isEmpty())
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("&"));
        return sign(canonical);
    }

    public static String formEncode(Map<String, String> params) {
        return params.entrySet().stream()
                .map(entry -> URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8)
                        + "=" + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
    }

    public static String wechatBody(String paymentNo, String transactionId,
                                    BigDecimal amount, String tradeState) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("mchid", MCH_ID);
            payload.put("out_trade_no", paymentNo);
            payload.put("transaction_id", transactionId);
            payload.put("trade_state", tradeState);
            payload.put("amount", Map.of("total", amount.multiply(new BigDecimal("100")).intValueExact()));
            byte[] plain = MAPPER.writeValueAsBytes(payload);

            String nonce = "0123456789ab";
            String associatedData = "transaction";
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE,
                    new SecretKeySpec(API_V3_KEY.getBytes(StandardCharsets.UTF_8), "AES"),
                    new GCMParameterSpec(128, nonce.getBytes(StandardCharsets.UTF_8)));
            cipher.updateAAD(associatedData.getBytes(StandardCharsets.UTF_8));
            String ciphertext = Base64.getEncoder().encodeToString(cipher.doFinal(plain));

            Map<String, Object> resource = new LinkedHashMap<>();
            resource.put("algorithm", "AEAD_AES_256_GCM");
            resource.put("ciphertext", ciphertext);
            resource.put("nonce", nonce);
            resource.put("associated_data", associatedData);
            return MAPPER.writeValueAsString(Map.of("resource", resource));
        } catch (Exception e) {
            throw new IllegalStateException("Unable to build test WeChat payload", e);
        }
    }

    public static String signWechat(String timestamp, String nonce, String body) {
        return sign(timestamp + "\n" + nonce + "\n" + body + "\n");
    }

    public static String timestamp() {
        return String.valueOf(System.currentTimeMillis() / 1000);
    }

    /** A Wechatpay-Timestamp header value that is the given seconds in the past. */
    public static String timestampSecondsAgo(long seconds) {
        return String.valueOf(System.currentTimeMillis() / 1000 - seconds);
    }

    private static String sign(String content) {
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(KEY_PAIR.getPrivate());
            signature.update(content.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signature.sign());
        } catch (Exception e) {
            throw new IllegalStateException("Unable to sign test payload", e);
        }
    }

    private static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to generate test RSA key pair", e);
        }
    }
}

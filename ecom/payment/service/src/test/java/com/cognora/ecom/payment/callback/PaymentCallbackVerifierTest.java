package com.cognora.ecom.payment.callback;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentCallbackVerifierTest {

    private final PaymentCallbackVerifier verifier =
            new PaymentCallbackVerifier(PaymentCallbackTestSupport.properties(), new ObjectMapper());

    @Test
    void acceptsAlipayCallbackSignedWithRsa2() {
        Map<String, String> params = PaymentCallbackTestSupport.alipayParams(
                "PAY-1", "ALI-9", "10.00", "TRADE_SUCCESS");
        params.put("sign", PaymentCallbackTestSupport.signAlipay(params));

        VerifiedPayment verified = verifier.verifyAlipay(params);

        assertTrue(verified.success());
        assertEquals("PAY-1", verified.paymentNo());
        assertEquals("ALI-9", verified.transactionId());
        assertEquals(new BigDecimal("10.00"), verified.paidAmount());
        assertEquals(PaymentCallbackTestSupport.APP_ID, verified.merchantId());
    }

    @Test
    void rejectsAlipayCallbackWithTamperedAmount() {
        Map<String, String> params = PaymentCallbackTestSupport.alipayParams(
                "PAY-1", "ALI-9", "10.00", "TRADE_SUCCESS");
        params.put("sign", PaymentCallbackTestSupport.signAlipay(params));
        params.put("total_amount", "9.99");

        PaymentCallbackException exception = assertThrows(PaymentCallbackException.class,
                () -> verifier.verifyAlipay(params));

        assertTrue(exception.getMessage().contains("signature"));
    }

    @Test
    void rejectsAlipayCallbackForAnotherMerchant() {
        Map<String, String> params = PaymentCallbackTestSupport.alipayParams(
                "PAY-1", "ALI-9", "10.00", "TRADE_SUCCESS");
        params.put("app_id", "other-app");
        params.put("sign", PaymentCallbackTestSupport.signAlipay(params));

        assertThrows(PaymentCallbackException.class, () -> verifier.verifyAlipay(params));
    }

    @Test
    void rejectsAlipayCallbackWhenVerificationIsNotConfigured() {
        PaymentProperties properties = PaymentCallbackTestSupport.properties();
        properties.getAlipay().setPublicKey(null);
        PaymentCallbackVerifier unconfigured = new PaymentCallbackVerifier(properties, new ObjectMapper());

        PaymentCallbackException exception = assertThrows(PaymentCallbackException.class,
                () -> unconfigured.verifyAlipay(Map.of("out_trade_no", "PAY-1")));

        assertTrue(exception.getMessage().contains("payment.alipay.public-key"));
    }

    @Test
    void acceptsWechatV3CallbackWithPlatformSignatureAndEncryptedResource() {
        String body = PaymentCallbackTestSupport.wechatBody(
                "PAY-1", "WX-1", new BigDecimal("10.00"), "SUCCESS");
        String timestamp = PaymentCallbackTestSupport.timestamp();
        String nonce = "nonce-1";
        String signature = PaymentCallbackTestSupport.signWechat(timestamp, nonce, body);

        VerifiedPayment verified = verifier.verifyWechat(body, timestamp, nonce, signature, null);

        assertTrue(verified.success());
        assertEquals("PAY-1", verified.paymentNo());
        assertEquals("WX-1", verified.transactionId());
        assertEquals(new BigDecimal("10.00"), verified.paidAmount());
        assertEquals(PaymentCallbackTestSupport.MCH_ID, verified.merchantId());
    }

    @Test
    void rejectsWechatCallbackWhenBodyChangesAfterSigning() {
        String body = PaymentCallbackTestSupport.wechatBody(
                "PAY-1", "WX-1", new BigDecimal("10.00"), "SUCCESS");
        String timestamp = PaymentCallbackTestSupport.timestamp();
        String nonce = "nonce-1";
        String signature = PaymentCallbackTestSupport.signWechat(timestamp, nonce, body);

        String tampered = body.replace("0123456789ab", "0123456789ac");

        assertThrows(PaymentCallbackException.class,
                () -> verifier.verifyWechat(tampered, timestamp, nonce, signature, null));
    }

    @Test
    void rejectsWechatCallbackWhenVerificationIsNotConfigured() {
        PaymentProperties properties = PaymentCallbackTestSupport.properties();
        properties.getWechat().setApiV3Key(null);
        PaymentCallbackVerifier unconfigured = new PaymentCallbackVerifier(properties, new ObjectMapper());

        PaymentCallbackException exception = assertThrows(PaymentCallbackException.class,
                () -> unconfigured.verifyWechat("{}", "1", "n", "s", null));

        assertTrue(exception.getMessage().contains("payment.wechat.platform-certificate"));
    }

    @Test
    void acknowledgesSignedWechatFailureWithoutAmount() {
        String body = PaymentCallbackTestSupport.wechatBody(
                "PAY-1", "WX-1", new BigDecimal("10.00"), "PAYERROR");
        String timestamp = PaymentCallbackTestSupport.timestamp();
        String nonce = "nonce-1";
        String signature = PaymentCallbackTestSupport.signWechat(timestamp, nonce, body);

        VerifiedPayment verified = verifier.verifyWechat(body, timestamp, nonce, signature, null);

        assertFalse(verified.success());
    }

    @Test
    void rejectsAlipayCallbackWithoutTotalAmount() {
        Map<String, String> params = PaymentCallbackTestSupport.alipayParams(
                "PAY-1", "ALI-9", "10.00", "TRADE_SUCCESS");
        params.remove("total_amount");
        params.put("sign", PaymentCallbackTestSupport.signAlipay(params));

        assertThrows(PaymentCallbackException.class, () -> verifier.verifyAlipay(params));
    }

    @Test
    void acceptsAlipayCallbackIssuedInsideTheReplayWindow() {
        Map<String, String> params = PaymentCallbackTestSupport.alipayParams(
                "PAY-1", "ALI-9", "10.00", "TRADE_SUCCESS");
        params.put("notify_time", PaymentCallbackTestSupport.alipayNotifyTime(Instant.now()));
        params.put("sign", PaymentCallbackTestSupport.signAlipay(params));

        VerifiedPayment verified = verifier.verifyAlipay(params);

        assertTrue(verified.success());
        assertEquals("PAY-1", verified.paymentNo());
    }

    @Test
    void rejectsAlipayCallbackIssuedOutsideTheReplayWindow() {
        Map<String, String> params = PaymentCallbackTestSupport.alipayParams(
                "PAY-1", "ALI-9", "10.00", "TRADE_SUCCESS");
        params.put("notify_time", PaymentCallbackTestSupport.alipayNotifyTime(
                Instant.now().minus(Duration.ofMinutes(10))));
        params.put("sign", PaymentCallbackTestSupport.signAlipay(params));

        PaymentCallbackException exception = assertThrows(PaymentCallbackException.class,
                () -> verifier.verifyAlipay(params));

        assertTrue(exception.getMessage().contains("window"));
    }

    @Test
    void rejectsAlipayCallbackWithoutNotifyTime() {
        Map<String, String> params = PaymentCallbackTestSupport.alipayParams(
                "PAY-1", "ALI-9", "10.00", "TRADE_SUCCESS");
        params.remove("notify_time");
        params.put("sign", PaymentCallbackTestSupport.signAlipay(params));

        PaymentCallbackException exception = assertThrows(PaymentCallbackException.class,
                () -> verifier.verifyAlipay(params));

        assertTrue(exception.getMessage().contains("notify_time"));
    }

    @Test
    void acceptsWechatCallbackIssuedInsideTheReplayWindow() {
        String body = PaymentCallbackTestSupport.wechatBody(
                "PAY-1", "WX-1", new BigDecimal("10.00"), "SUCCESS");
        String timestamp = PaymentCallbackTestSupport.timestamp();
        String nonce = "nonce-1";
        String signature = PaymentCallbackTestSupport.signWechat(timestamp, nonce, body);

        VerifiedPayment verified = verifier.verifyWechat(body, timestamp, nonce, signature, null);

        assertTrue(verified.success());
    }

    @Test
    void rejectsWechatCallbackIssuedOutsideTheReplayWindow() {
        String body = PaymentCallbackTestSupport.wechatBody(
                "PAY-1", "WX-1", new BigDecimal("10.00"), "SUCCESS");
        String timestamp = PaymentCallbackTestSupport.timestampSecondsAgo(600);
        String nonce = "nonce-1";
        String signature = PaymentCallbackTestSupport.signWechat(timestamp, nonce, body);

        PaymentCallbackException exception = assertThrows(PaymentCallbackException.class,
                () -> verifier.verifyWechat(body, timestamp, nonce, signature, null));

        assertTrue(exception.getMessage().contains("window"));
    }
}

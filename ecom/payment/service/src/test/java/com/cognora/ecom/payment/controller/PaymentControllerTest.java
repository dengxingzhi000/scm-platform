package com.cognora.ecom.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.cognora.ecom.payment.callback.PaymentCallbackTestSupport;
import com.cognora.ecom.payment.callback.PaymentCallbackVerifier;
import com.cognora.ecom.payment.callback.PaymentProperties;
import com.cognora.ecom.payment.service.IPaymentOrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    @Mock
    private IPaymentOrderService paymentService;

    private MockMvc mockMvc;
    private PaymentCallbackVerifier verifier;

    @BeforeEach
    void setUp() {
        PaymentProperties properties = PaymentCallbackTestSupport.properties();
        verifier = new PaymentCallbackVerifier(properties, new ObjectMapper());
        PaymentController controller = new PaymentController(
                paymentService, new ObjectMapper(), verifier);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void alipaySignedSuccessCallbackMarksPaymentPaid() throws Exception {
        Map<String, String> params = PaymentCallbackTestSupport.alipayParams(
                "PAY-1", "ALI-9", "10.00", "TRADE_SUCCESS");
        params.put("sign", PaymentCallbackTestSupport.signAlipay(params));

        mockMvc.perform(post("/api/v1/payments/callback/alipay")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .content(PaymentCallbackTestSupport.formEncode(params)))
                .andExpect(status().isOk())
                .andExpect(content().string("success"));

        verify(paymentService).handleVerifiedPaymentCallback(
                org.mockito.ArgumentMatchers.eq("PAY-1"),
                org.mockito.ArgumentMatchers.eq("ALI-9"),
                argThat(amount -> amount != null && amount.compareTo(new BigDecimal("10.00")) == 0),
                org.mockito.ArgumentMatchers.eq("alipay"));
    }

    @Test
    void forgedAlipayCallbackIsRejectedWithoutTouchingThePayment() throws Exception {
        Map<String, String> params = PaymentCallbackTestSupport.alipayParams(
                "PAY-1", "ALI-9", "10.00", "TRADE_SUCCESS");
        params.put("sign", "not-a-signature");

        mockMvc.perform(post("/api/v1/payments/callback/alipay")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .content(PaymentCallbackTestSupport.formEncode(params)))
                .andExpect(status().isOk())
                .andExpect(content().string("failure"));

        verify(paymentService, never()).handleVerifiedPaymentCallback(
                anyString(), anyString(), any(), anyString());
    }

    @Test
    void alipayCallbackForAnotherMerchantIsRejected() throws Exception {
        Map<String, String> params = PaymentCallbackTestSupport.alipayParams(
                "PAY-1", "ALI-9", "10.00", "TRADE_SUCCESS");
        params.put("app_id", "other-app");
        params.put("sign", PaymentCallbackTestSupport.signAlipay(params));

        mockMvc.perform(post("/api/v1/payments/callback/alipay")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .content(PaymentCallbackTestSupport.formEncode(params)))
                .andExpect(status().isOk())
                .andExpect(content().string("failure"));

        verify(paymentService, never()).handleVerifiedPaymentCallback(
                anyString(), anyString(), any(), anyString());
    }

    @Test
    void alipayAmountMismatchIsRejectedByTheService() throws Exception {
        Map<String, String> params = PaymentCallbackTestSupport.alipayParams(
                "PAY-1", "ALI-9", "9.99", "TRADE_SUCCESS");
        params.put("sign", PaymentCallbackTestSupport.signAlipay(params));
        doThrow(new IllegalStateException("callback amount does not match"))
                .when(paymentService).handleVerifiedPaymentCallback(anyString(), anyString(), any(), anyString());

        mockMvc.perform(post("/api/v1/payments/callback/alipay")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .content(PaymentCallbackTestSupport.formEncode(params)))
                .andExpect(status().isOk())
                .andExpect(content().string("failure"));
    }

    @Test
    void alipayCallbackIsRejectedWhenVerificationIsNotConfigured() throws Exception {
        PaymentProperties properties = PaymentCallbackTestSupport.properties();
        properties.getAlipay().setPublicKey("");
        PaymentCallbackVerifier unconfigured = new PaymentCallbackVerifier(properties, new ObjectMapper());
        MockMvc unconfiguredMvc = MockMvcBuilders.standaloneSetup(
                new PaymentController(paymentService, new ObjectMapper(), unconfigured)).build();

        Map<String, String> params = PaymentCallbackTestSupport.alipayParams(
                "PAY-1", "ALI-9", "10.00", "TRADE_SUCCESS");
        params.put("sign", PaymentCallbackTestSupport.signAlipay(params));

        unconfiguredMvc.perform(post("/api/v1/payments/callback/alipay")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .content(PaymentCallbackTestSupport.formEncode(params)))
                .andExpect(status().isOk())
                .andExpect(content().string("failure"));

        verify(paymentService, never()).handleVerifiedPaymentCallback(
                anyString(), anyString(), any(), anyString());
    }

    @Test
    void alipayNonSuccessNotificationIsAcknowledgedWithoutMarkingPaid() throws Exception {
        Map<String, String> params = PaymentCallbackTestSupport.alipayParams(
                "PAY-1", "ALI-9", "10.00", "TRADE_CLOSED");
        params.put("sign", PaymentCallbackTestSupport.signAlipay(params));

        mockMvc.perform(post("/api/v1/payments/callback/alipay")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .content(PaymentCallbackTestSupport.formEncode(params)))
                .andExpect(status().isOk())
                .andExpect(content().string("success"));

        verify(paymentService, never()).handleVerifiedPaymentCallback(
                anyString(), anyString(), any(), anyString());
    }

    @Test
    void signedWechatCallbackMarksPaymentPaid() throws Exception {
        String body = PaymentCallbackTestSupport.wechatBody(
                "PAY-2", "WX-2", new BigDecimal("10.00"), "SUCCESS");
        String timestamp = PaymentCallbackTestSupport.timestamp();
        String nonce = "nonce-1";
        String signature = PaymentCallbackTestSupport.signWechat(timestamp, nonce, body);

        mockMvc.perform(post("/api/v1/payments/callback/wechat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Wechatpay-Timestamp", timestamp)
                        .header("Wechatpay-Nonce", nonce)
                        .header("Wechatpay-Signature", signature)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().string("success"));

        verify(paymentService).handleVerifiedPaymentCallback(
                org.mockito.ArgumentMatchers.eq("PAY-2"),
                org.mockito.ArgumentMatchers.eq("WX-2"),
                argThat(amount -> amount != null && amount.compareTo(new BigDecimal("10.00")) == 0),
                org.mockito.ArgumentMatchers.eq("wechat"));
    }

    @Test
    void forgedWechatCallbackIsRejected() throws Exception {
        String body = PaymentCallbackTestSupport.wechatBody(
                "PAY-2", "WX-2", new BigDecimal("10.00"), "SUCCESS");
        String timestamp = PaymentCallbackTestSupport.timestamp();
        String nonce = "nonce-1";

        mockMvc.perform(post("/api/v1/payments/callback/wechat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Wechatpay-Timestamp", timestamp)
                        .header("Wechatpay-Nonce", nonce)
                        .header("Wechatpay-Signature", "forged")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().string("failure"));

        verify(paymentService, never()).handleVerifiedPaymentCallback(
                anyString(), anyString(), any(), anyString());
    }

    @Test
    void callbackWithoutSignatureHeadersIsRejected() throws Exception {
        mockMvc.perform(post("/api/v1/payments/callback/wechat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resource\":{}}"))
                .andExpect(status().isOk())
                .andExpect(content().string("failure"));

        verify(paymentService, never()).handleVerifiedPaymentCallback(
                anyString(), anyString(), any(), anyString());
    }
}

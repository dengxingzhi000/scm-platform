package com.cognora.lms.logistics.controller;

import com.cognora.lms.logistics.domain.entity.TmsTracking;
import com.cognora.lms.logistics.service.ITmsTrackingService;
import com.cognora.lms.logistics.support.webhook.WebhookSignatureVerifier;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 批次 3：承运商回调验签入口用例。
 * 覆盖验签成功落库、签名错误/超时间窗/缺头 401 且不落库。
 */
class TmsTrackingControllerCallbackTest {

    private static final String SECRET = "unit-test-secret";
    private static final String BODY =
            "{\"eventId\":\"evt-1\",\"waybillNo\":\"WB-1\",\"trackStatus\":\"DELIVERED\"}";

    private ITmsTrackingService trackingService;
    private WebhookSignatureVerifier verifier;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        trackingService = mock(ITmsTrackingService.class);
        verifier = new WebhookSignatureVerifier(SECRET, WebhookSignatureVerifier.DEFAULT_WINDOW_SECONDS);
        mvc = MockMvcBuilders.standaloneSetup(
                new TmsTrackingController(trackingService, verifier, new ObjectMapper())).build();
    }

    @Test
    void signedCallbackShouldPersistTracking() throws Exception {
        long timestamp = System.currentTimeMillis() / 1000;
        String nonce = "nonce-ok";
        String signature = verifier.computeSignature(String.valueOf(timestamp), nonce, BODY);
        when(trackingService.addTracking(any(TmsTracking.class)))
                .thenReturn(new TmsTracking().setId("t-1").setWaybillNo("WB-1"));

        mvc.perform(post("/api/v1/tms-tracking/callback/SF")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Lms-Timestamp", String.valueOf(timestamp))
                        .header("X-Lms-Nonce", nonce)
                        .header("X-Lms-Signature", "sha256=" + signature)
                        .content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(trackingService).addTracking(any(TmsTracking.class));
    }

    @Test
    void tamperedBodyShouldReturn401AndNotPersist() throws Exception {
        long timestamp = System.currentTimeMillis() / 1000;
        String nonce = "nonce-tamper";
        String signature = verifier.computeSignature(String.valueOf(timestamp), nonce, BODY);

        mvc.perform(post("/api/v1/tms-tracking/callback/SF")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Lms-Timestamp", String.valueOf(timestamp))
                        .header("X-Lms-Nonce", nonce)
                        .header("X-Lms-Signature", signature)
                        .content(BODY.replace("WB-1", "WB-2")))
                .andExpect(status().isUnauthorized());

        verify(trackingService, never()).addTracking(any(TmsTracking.class));
    }

    @Test
    void expiredTimestampShouldReturn401AndNotPersist() throws Exception {
        long timestamp = System.currentTimeMillis() / 1000
                - WebhookSignatureVerifier.DEFAULT_WINDOW_SECONDS - 1;
        String nonce = "nonce-expired";
        String signature = verifier.computeSignature(String.valueOf(timestamp), nonce, BODY);

        mvc.perform(post("/api/v1/tms-tracking/callback/SF")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Lms-Timestamp", String.valueOf(timestamp))
                        .header("X-Lms-Nonce", nonce)
                        .header("X-Lms-Signature", signature)
                        .content(BODY))
                .andExpect(status().isUnauthorized());

        verify(trackingService, never()).addTracking(any(TmsTracking.class));
    }

    @Test
    void missingSignatureHeadersShouldReturn401AndNotPersist() throws Exception {
        mvc.perform(post("/api/v1/tms-tracking/callback/SF")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isUnauthorized());

        verify(trackingService, never()).addTracking(any(TmsTracking.class));
    }
}

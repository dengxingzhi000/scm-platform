package com.scmcloud.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scmcloud.payment.service.IPaymentOrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

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

    @BeforeEach
    void setUp() {
        PaymentController controller = new PaymentController(paymentService, new ObjectMapper());
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void alipaySuccessCallbackMarksPaymentPaid() throws Exception {
        mockMvc.perform(post("/api/v1/payments/callback/alipay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"out_trade_no":"PAY-1","trade_no":"ALI-9","trade_status":"TRADE_SUCCESS"}
                                """))
                .andExpect(status().isOk())
                .andExpect(content().string("success"));

        verify(paymentService).handlePaymentCallback("PAY-1", "ALI-9");
    }

    @Test
    void wechatFailureCallbackIsAcknowledgedWithoutMarkingPaid() throws Exception {
        mockMvc.perform(post("/api/v1/payments/callback/wechat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"out_trade_no":"PAY-1","transaction_id":"WX-1","result_code":"FAIL"}
                                """))
                .andExpect(status().isOk())
                .andExpect(content().string("success"));

        verify(paymentService, never()).handlePaymentCallback("PAY-1", "WX-1");
    }

    @Test
    void callbackWithoutPaymentNumberFails() throws Exception {
        mockMvc.perform(post("/api/v1/payments/callback/alipay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"trade_status\":\"TRADE_SUCCESS\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string("failure"));

        verify(paymentService, never()).handlePaymentCallback(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void formEncodedCallbackIsParsed() throws Exception {
        mockMvc.perform(post("/api/v1/payments/callback/wechat")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .content("out_trade_no=PAY-2&transaction_id=WX-2&result_code=SUCCESS"))
                .andExpect(status().isOk())
                .andExpect(content().string("success"));

        verify(paymentService).handlePaymentCallback("PAY-2", "WX-2");
    }
}

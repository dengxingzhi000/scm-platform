package com.cognora.ecom.payment.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.cognora.ecom.payment.callback.PaymentCallbackException;
import com.cognora.ecom.payment.domain.entity.PaymentOrder;
import com.cognora.ecom.payment.mapper.PaymentOrderMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentOrderServiceImplTest {

    @Mock private PaymentOrderMapper paymentOrderMapper;

    private PaymentOrderServiceImpl paymentService;

    @BeforeAll
    static void initTableInfo() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), PaymentOrder.class);
    }

    @BeforeEach
    void setUp() {
        paymentService = new PaymentOrderServiceImpl();
        ReflectionTestUtils.setField(paymentService, "baseMapper", paymentOrderMapper);
    }

    @Test
    void createPaymentReusesExistingPendingPayment() {
        PaymentOrder pending = pendingPayment();
        when(paymentOrderMapper.selectOne(any(Wrapper.class), eq(true))).thenReturn(pending);

        PaymentOrder result = paymentService.createPayment("ORD-1", "user-1",
                new BigDecimal("10.00"), "ALIPAY");

        assertEquals("PAY-1", result.getPaymentNo());
        verify(paymentOrderMapper, never()).insert(any(PaymentOrder.class));
    }

    @Test
    void createPaymentRejectsAlreadyPaidOrder() {
        PaymentOrder paid = pendingPayment().setStatus(2);
        when(paymentOrderMapper.selectOne(any(Wrapper.class), eq(true))).thenReturn(paid);

        assertThrows(IllegalStateException.class, () -> paymentService.createPayment(
                "ORD-1", "user-1", new BigDecimal("10.00"), "ALIPAY"));
    }

    @Test
    void createPaymentRejectsAmountMismatchOnPendingPayment() {
        when(paymentOrderMapper.selectOne(any(Wrapper.class), eq(true))).thenReturn(pendingPayment());

        assertThrows(IllegalStateException.class, () -> paymentService.createPayment(
                "ORD-1", "user-1", new BigDecimal("20.00"), "ALIPAY"));
    }

    @Test
    void callbackIsIdempotentForPaidPayment() {
        when(paymentOrderMapper.selectOne(any(Wrapper.class), eq(true)))
                .thenReturn(pendingPayment().setStatus(2));

        paymentService.handlePaymentCallback("PAY-1", "TXN-2");

        verify(paymentOrderMapper, never()).updateById(any(PaymentOrder.class));
    }

    @Test
    void callbackMarksPendingPaymentPaidAndStoresTransactionId() {
        PaymentOrder pending = pendingPayment();
        when(paymentOrderMapper.selectOne(any(Wrapper.class), eq(true))).thenReturn(pending);
        when(paymentOrderMapper.updateById(any(PaymentOrder.class))).thenReturn(1);

        paymentService.handlePaymentCallback("PAY-1", "TXN-1");

        assertEquals(2, pending.getStatus());
        assertEquals("TXN-1", pending.getTransactionId());
        verify(paymentOrderMapper).updateById(eq(pending));
    }

    @Test
    void callbackRejectsCancelledPayment() {
        when(paymentOrderMapper.selectOne(any(Wrapper.class), eq(true)))
                .thenReturn(pendingPayment().setStatus(4));

        assertThrows(IllegalStateException.class,
                () -> paymentService.handlePaymentCallback("PAY-1", "TXN-1"));
    }

    @Test
    void verifiedCallbackRejectsAmountMismatch() {
        when(paymentOrderMapper.selectOne(any(Wrapper.class), eq(true))).thenReturn(pendingPayment());

        assertThrows(PaymentCallbackException.class, () -> paymentService.handleVerifiedPaymentCallback(
                "PAY-1", "TXN-1", new BigDecimal("9.99"), "alipay"));
        verify(paymentOrderMapper, never()).updateById(any(PaymentOrder.class));
    }

    @Test
    void verifiedCallbackRejectsChannelMismatch() {
        when(paymentOrderMapper.selectOne(any(Wrapper.class), eq(true))).thenReturn(pendingPayment());

        assertThrows(PaymentCallbackException.class, () -> paymentService.handleVerifiedPaymentCallback(
                "PAY-1", "TXN-1", new BigDecimal("10.00"), "wechat"));
        verify(paymentOrderMapper, never()).updateById(any(PaymentOrder.class));
    }

    @Test
    void verifiedCallbackIsIdempotentForPaidPayment() {
        when(paymentOrderMapper.selectOne(any(Wrapper.class), eq(true)))
                .thenReturn(pendingPayment().setStatus(2));

        paymentService.handleVerifiedPaymentCallback(
                "PAY-1", "TXN-2", new BigDecimal("10.00"), "alipay");

        verify(paymentOrderMapper, never()).updateById(any(PaymentOrder.class));
    }

    @Test
    void createPaymentRejectsAmountAboveMaximum() {
        assertThrows(IllegalArgumentException.class, () -> paymentService.createPayment(
                "ORD-1", "user-1", new BigDecimal("100000000.00"), "ALIPAY"));
        verify(paymentOrderMapper, never()).selectOne(any(Wrapper.class), eq(true));
    }

    @Test
    void pageQueryRejectsInvalidPage() {
        assertThrows(IllegalArgumentException.class,
                () -> paymentService.pageQuery(0, 20, null, null, null, null));
    }

    @Test
    void pageQueryRejectsOversizedPageSize() {
        assertThrows(IllegalArgumentException.class,
                () -> paymentService.pageQuery(1, 101, null, null, null, null));
    }

    private PaymentOrder pendingPayment() {
        return new PaymentOrder()
                .setPaymentNo("PAY-1")
                .setOrderNo("ORD-1")
                .setUserId("user-1")
                .setAmount(new BigDecimal("10.00"))
                .setPaymentChannel("ALIPAY")
                .setStatus(1);
    }
}

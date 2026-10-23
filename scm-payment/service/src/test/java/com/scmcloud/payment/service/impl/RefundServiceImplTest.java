package com.scmcloud.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.scmcloud.payment.domain.entity.PaymentOrder;
import com.scmcloud.payment.domain.entity.Refund;
import com.scmcloud.payment.mapper.RefundMapper;
import com.scmcloud.payment.service.IPaymentOrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefundServiceImplTest {

    @Mock private RefundMapper refundMapper;
    @Mock private IPaymentOrderService paymentOrderService;

    private RefundServiceImpl refundService;

    @BeforeEach
    void setUp() {
        refundService = new RefundServiceImpl(paymentOrderService);
        ReflectionTestUtils.setField(refundService, "baseMapper", refundMapper);
    }

    @Test
    void createsPendingRefundForPaidPayment() {
        when(paymentOrderService.queryPayment("PAY-1")).thenReturn(paidPayment());
        when(refundMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        when(refundMapper.insert(any(Refund.class))).thenReturn(1);

        Refund refund = refundService.createRefund("PAY-1", "ORD-1", "user-1",
                new BigDecimal("30.00"), "damaged");

        assertNotNull(refund.getRefundNo());
        assertTrue(refund.getRefundNo().startsWith("RF"));
        assertEquals(1, refund.getStatus());
        assertEquals(new BigDecimal("30.00"), refund.getRefundAmount());
        verify(refundMapper).insert(refund);
    }

    @Test
    void rejectsRefundExceedingRemainingBalance() {
        when(paymentOrderService.queryPayment("PAY-1")).thenReturn(paidPayment());
        Refund previous = new Refund()
                .setRefundAmount(new BigDecimal("80.00"))
                .setStatus(2);
        when(refundMapper.selectList(any(Wrapper.class))).thenReturn(List.of(previous));

        assertThrows(IllegalArgumentException.class, () -> refundService.createRefund(
                "PAY-1", "ORD-1", "user-1", new BigDecimal("30.00"), "x"));
        verify(refundMapper, never()).insert(any(Refund.class));
    }

    @Test
    void rejectsRefundForUnpaidPayment() {
        PaymentOrder payment = paidPayment().setStatus(1);
        when(paymentOrderService.queryPayment("PAY-1")).thenReturn(payment);

        assertThrows(IllegalStateException.class, () -> refundService.createRefund(
                "PAY-1", "ORD-1", "user-1", new BigDecimal("10.00"), "x"));
    }

    @Test
    void approvesPendingRefund() {
        Refund pending = new Refund().setRefundNo("RF-1").setStatus(1);
        when(refundMapper.selectOne(any(Wrapper.class), eq(true))).thenReturn(pending);
        when(refundMapper.updateById(any(Refund.class))).thenReturn(1);

        Refund approved = refundService.approveRefund("RF-1");

        assertEquals(2, approved.getStatus());
        assertNotNull(approved.getRefundedAt());
    }

    @Test
    void rejectsApprovingNonPendingRefund() {
        Refund approved = new Refund().setRefundNo("RF-1").setStatus(2);
        when(refundMapper.selectOne(any(Wrapper.class), eq(true))).thenReturn(approved);

        assertThrows(IllegalStateException.class, () -> refundService.approveRefund("RF-1"));
    }

    private PaymentOrder paidPayment() {
        return new PaymentOrder()
                .setPaymentNo("PAY-1")
                .setOrderNo("ORD-1")
                .setUserId("user-1")
                .setAmount(new BigDecimal("100.00"))
                .setStatus(2);
    }
}

package com.cognora.scm.finance.service.impl;

import com.cognora.platform.common.status.StatusValidator;
import com.cognora.platform.common.tenant.TenantContextHolder;
import com.cognora.scm.finance.domain.dto.TaxRateResolution;
import com.cognora.scm.finance.domain.enums.TaxRateSource;
import com.cognora.scm.finance.service.support.AccountingPeriodGuard;
import com.cognora.scm.finance.service.support.TaxRateResolver;
import com.cognora.platform.common.tenant.TenantParseException;
import com.cognora.scm.finance.domain.entity.SettlementOrder;
import com.cognora.scm.finance.mapper.SettlementOrderMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SettlementOrderServiceImplTest {

    private static final UUID TENANT = UUID.fromString("019a0aee-0000-7000-8000-000000000002");

    @Mock
    private SettlementOrderMapper mapper;

    @Mock
    private StatusValidator statusValidator;

    @Mock
    private AccountingPeriodGuard accountingPeriodGuard;

    private SettlementOrderServiceImpl service;

    @BeforeEach
    void setUp() {
        TaxRateResolver taxRateResolver = org.mockito.Mockito.mock(TaxRateResolver.class,
                invocation -> new TaxRateResolution(
                        new BigDecimal("0.1300"), TaxRateSource.DEFAULT, null, null));
        service = new SettlementOrderServiceImpl(statusValidator, accountingPeriodGuard, taxRateResolver);
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
        TenantContextHolder.setTenantId(TENANT);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void createShouldForceTenantFromContextAndComputePayable() {
        SettlementOrder order = new SettlementOrder();
        order.setTotalAmount(new BigDecimal("100.00"));
        order.setDiscountAmount(new BigDecimal("10.00"));
        order.setAdjustmentAmount(new BigDecimal("-5.00"));
        when(mapper.insert(any(SettlementOrder.class))).thenReturn(1);

        SettlementOrder created = service.createSettlement(order);

        assertEquals(TENANT.toString(), created.getTenantId());
        assertEquals("85.00", created.getActualAmount().toPlainString());
        assertEquals("0.00", created.getPaidAmount().toPlainString());
        assertEquals("85.00", created.getUnpaidAmount().toPlainString());
        assertEquals(0, created.getStatus());
        assertEquals("CNY", created.getCurrency());
        // 未显式传税率时按规则解析并落库，响应回显来源。
        assertEquals("0.1300", created.getTaxRate().toPlainString());
        assertEquals("DEFAULT", created.getTaxRateSource());
        assertFalse(created.getDeleted());
        verify(mapper).insert(created);
    }

    @Test
    void createShouldKeepExplicitTaxRate() {
        SettlementOrder order = new SettlementOrder();
        order.setTotalAmount(new BigDecimal("100.00"));
        order.setTaxRate(new BigDecimal("0.06"));
        when(mapper.insert(any(SettlementOrder.class))).thenReturn(1);

        SettlementOrder created = service.createSettlement(order);

        assertEquals("0.0600", created.getTaxRate().toPlainString());
        assertNull(created.getTaxRateSource());
    }

    @Test
    void createShouldRejectTaxRateAboveOne() {
        SettlementOrder order = new SettlementOrder();
        order.setTotalAmount(new BigDecimal("100.00"));
        order.setTaxRate(new BigDecimal("1.2"));

        assertThrows(IllegalArgumentException.class, () -> service.createSettlement(order));
        verify(mapper, never()).insert(any(SettlementOrder.class));
    }

    @Test
    void createShouldRejectForeignTenantInRequestBody() {
        SettlementOrder order = new SettlementOrder();
        order.setTotalAmount(new BigDecimal("100.00"));
        order.setTenantId("019a0aee-0000-7000-8000-000000000099");

        assertThrows(AccessDeniedException.class, () -> service.createSettlement(order));
        verify(mapper, never()).insert(any(SettlementOrder.class));
    }

    @Test
    void createShouldRejectAmountWithThreeDecimals() {
        SettlementOrder order = new SettlementOrder();
        order.setTotalAmount(new BigDecimal("100.001"));

        assertThrows(IllegalArgumentException.class, () -> service.createSettlement(order));
        verify(mapper, never()).insert(any(SettlementOrder.class));
    }

    @Test
    void createShouldRejectPayableNotMatchingTotalDiscountAdjustment() {
        SettlementOrder order = new SettlementOrder();
        order.setTotalAmount(new BigDecimal("100.00"));
        order.setDiscountAmount(new BigDecimal("10.00"));
        order.setActualAmount(new BigDecimal("95.00"));

        assertThrows(IllegalArgumentException.class, () -> service.createSettlement(order));
        verify(mapper, never()).insert(any(SettlementOrder.class));
    }

    @Test
    void createShouldFailClosedWithoutTenantContext() {
        SettlementOrder order = new SettlementOrder();
        order.setTotalAmount(new BigDecimal("100.00"));
        TenantContextHolder.clear();

        assertThrows(TenantParseException.class, () -> service.createSettlement(order));
        verify(mapper, never()).insert(any(SettlementOrder.class));
    }

    @Test
    void partialPaymentShouldUpdatePaidAndUnpaidAmounts() {
        SettlementOrder existing = settlementOrder(1, "0.00");
        when(mapper.selectById("stl-1")).thenReturn(existing);
        when(mapper.updateById(any(SettlementOrder.class))).thenReturn(1);

        SettlementOrder result = service.recordPayment("stl-1", new BigDecimal("40.00"));

        assertEquals("40.00", result.getPaidAmount().toPlainString());
        assertEquals("60.00", result.getUnpaidAmount().toPlainString());
        assertEquals(3, result.getStatus());
        verify(statusValidator).validateTransition("SETTLEMENT", "CONFIRMED", "PARTIAL_PAID");
    }

    @Test
    void paymentWithThreeDecimalsShouldBeRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> service.recordPayment("stl-1", new BigDecimal("40.005")));
        verify(mapper, never()).selectById("stl-1");
    }

    @Test
    void paymentExceedingPayableShouldBeRejected() {
        SettlementOrder existing = settlementOrder(1, "20.00");
        when(mapper.selectById("stl-1")).thenReturn(existing);

        assertThrows(IllegalArgumentException.class,
                () -> service.recordPayment("stl-1", new BigDecimal("90.00")));
        verify(mapper, never()).updateById(any(SettlementOrder.class));
    }

    @Test
    void updateShouldRejectAmountTampering() {
        SettlementOrder existing = settlementOrder(0, "0.00");
        SettlementOrder request = new SettlementOrder();
        request.setTotalAmount(new BigDecimal("50.00"));
        when(mapper.selectById("stl-1")).thenReturn(existing);

        assertThrows(IllegalArgumentException.class, () -> service.updateSettlement("stl-1", request));
        verify(mapper, never()).updateById(any(SettlementOrder.class));
    }

    @Test
    void updateShouldRejectStatusTampering() {
        SettlementOrder existing = settlementOrder(0, "0.00");
        SettlementOrder request = new SettlementOrder();
        request.setStatus(4);
        when(mapper.selectById("stl-1")).thenReturn(existing);

        assertThrows(IllegalArgumentException.class, () -> service.updateSettlement("stl-1", request));
    }

    @Test
    void updateShouldAllowRemarkOnConfirmedSettlement() {
        SettlementOrder existing = settlementOrder(1, "0.00");
        SettlementOrder request = new SettlementOrder();
        request.setRemark("补充说明");
        when(mapper.selectById("stl-1")).thenReturn(existing);
        when(mapper.updateById(any(SettlementOrder.class))).thenReturn(1);

        SettlementOrder result = service.updateSettlement("stl-1", request);

        assertEquals("补充说明", result.getRemark());
        assertTrue(result.getUpdateTime() != null);
        verify(mapper).updateById(existing);
    }

    @Test
    void updateShouldRejectBusinessChangesAfterConfirmation() {
        SettlementOrder existing = settlementOrder(1, "0.00");
        SettlementOrder request = new SettlementOrder();
        request.setPartnerName("另一个供应商");
        when(mapper.selectById("stl-1")).thenReturn(existing);

        assertThrows(IllegalStateException.class, () -> service.updateSettlement("stl-1", request));
        verify(mapper, never()).updateById(any(SettlementOrder.class));
    }

    private SettlementOrder settlementOrder(int status, String paidAmount) {
        SettlementOrder order = new SettlementOrder();
        order.setId("stl-1");
        order.setTenantId(TENANT.toString());
        order.setStatus(status);
        order.setTotalAmount(new BigDecimal("100.00"));
        order.setDiscountAmount(new BigDecimal("0.00"));
        order.setAdjustmentAmount(new BigDecimal("0.00"));
        order.setActualAmount(new BigDecimal("100.00"));
        order.setPaidAmount(new BigDecimal(paidAmount));
        order.setUnpaidAmount(new BigDecimal("100.00").subtract(new BigDecimal(paidAmount)));
        order.setDeleted(false);
        return order;
    }
}

package com.cognora.scm.finance.service.impl;

import com.cognora.platform.common.tenant.TenantContextHolder;
import com.cognora.scm.finance.domain.entity.FundFlow;
import com.cognora.scm.finance.domain.entity.Invoice;
import com.cognora.scm.finance.domain.entity.SettlementOrder;
import com.cognora.scm.finance.mapper.FundFlowMapper;
import com.cognora.scm.finance.mapper.InvoiceMapper;
import com.cognora.scm.finance.mapper.SettlementOrderMapper;
import com.cognora.scm.finance.service.support.AccountingPeriodGuard;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FundFlowServiceImplTest {

    private static final UUID TENANT = UUID.fromString("019a0aee-0000-7000-8000-000000000002");
    private static final String FOREIGN_TENANT = "019a0aee-0000-7000-8000-000000000099";

    @Mock
    private FundFlowMapper fundFlowMapper;

    @Mock
    private SettlementOrderMapper settlementOrderMapper;

    @Mock
    private InvoiceMapper invoiceMapper;

    private FundFlowServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new FundFlowServiceImpl(fundFlowMapper, settlementOrderMapper, invoiceMapper,
                mock(AccountingPeriodGuard.class));
        TenantContextHolder.setTenantId(TENANT);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void recordShouldForceTenantAndDefaultCurrency() {
        stubSettlement(TENANT.toString());
        when(fundFlowMapper.insert(any(FundFlow.class))).thenReturn(1);

        FundFlow saved = service.record(flow("pay-001", "IN", "40.00"));

        assertEquals(TENANT.toString(), saved.getTenantId());
        assertEquals("CNY", saved.getCurrency());
        assertNotNull(saved.getId());
        assertNotNull(saved.getFlowNo());
        assertEquals("40.00", saved.getAmount().toPlainString());
    }

    @Test
    void recordShouldRejectForeignTenantInRequestBody() {
        FundFlow flow = flow("pay-001", "IN", "40.00");
        flow.setTenantId(FOREIGN_TENANT);

        assertThrows(AccessDeniedException.class, () -> service.record(flow));
        verify(fundFlowMapper, never()).insert(any(FundFlow.class));
    }

    @Test
    void orphanFlowShouldBeRejected() {
        FundFlow flow = flow("pay-001", "IN", "40.00");
        flow.setSettlementId(null);

        assertThrows(IllegalArgumentException.class, () -> service.record(flow));
        verify(fundFlowMapper, never()).insert(any(FundFlow.class));
    }

    @Test
    void settlementPaymentWithoutSettlementShouldBeRejected() {
        FundFlow flow = flow("pay-001", "IN", "40.00");
        flow.setSettlementId(null);
        flow.setInvoiceId("inv-1");
        flow.setSourceType("SETTLEMENT_PAYMENT");

        assertThrows(IllegalArgumentException.class, () -> service.record(flow));
        verify(fundFlowMapper, never()).insert(any(FundFlow.class));
    }

    @Test
    void missingSourceTypeShouldBeRejected() {
        FundFlow flow = flow("pay-001", "IN", "40.00");
        flow.setSourceType(null);

        assertThrows(IllegalArgumentException.class, () -> service.record(flow));
        verify(fundFlowMapper, never()).insert(any(FundFlow.class));
    }

    @Test
    void missingLinkedSettlementShouldBeRejected() {
        when(settlementOrderMapper.selectById("stl-1")).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> service.record(flow("pay-001", "IN", "40.00")));
        verify(fundFlowMapper, never()).insert(any(FundFlow.class));
    }

    @Test
    void crossTenantSettlementShouldBeRejectedWith403() {
        stubSettlement(FOREIGN_TENANT);

        assertThrows(AccessDeniedException.class, () -> service.record(flow("pay-001", "IN", "40.00")));
        verify(fundFlowMapper, never()).insert(any(FundFlow.class));
    }

    @Test
    void invoicePaymentShouldLinkInvoice() {
        Invoice invoice = new Invoice();
        invoice.setId("inv-1");
        invoice.setTenantId(TENANT.toString());
        invoice.setInvoiceNo("INV-1");
        invoice.setDeleted(false);
        when(invoiceMapper.selectById("inv-1")).thenReturn(invoice);
        when(fundFlowMapper.insert(any(FundFlow.class))).thenReturn(1);

        FundFlow flow = flow("pay-inv", "OUT", "20.00");
        flow.setSettlementId(null);
        flow.setInvoiceId("inv-1");
        flow.setSourceType("INVOICE_PAYMENT");

        FundFlow saved = service.record(flow);

        assertEquals("INV-1", saved.getSourceNo());
        verify(invoiceMapper).selectById("inv-1");
    }

    @Test
    void duplicateKeyWithSamePayloadShouldReturnExistingFlow() {
        stubSettlement(TENANT.toString());
        FundFlow existing = flow("pay-001", "IN", "40.00");
        existing.setId("ff-existing");
        when(fundFlowMapper.insert(any(FundFlow.class)))
                .thenThrow(new DuplicateKeyException("duplicate idempotency key"));
        when(fundFlowMapper.selectOne(any())).thenReturn(existing);

        FundFlow saved = service.record(flow("pay-001", "IN", "40.00"));

        assertEquals("ff-existing", saved.getId());
        verify(fundFlowMapper).insert(any(FundFlow.class));
    }

    @Test
    void duplicateKeyWithDifferentAmountShouldBeRejected() {
        stubSettlement(TENANT.toString());
        FundFlow existing = flow("pay-001", "IN", "40.00");
        existing.setId("ff-existing");
        when(fundFlowMapper.insert(any(FundFlow.class)))
                .thenThrow(new DuplicateKeyException("duplicate idempotency key"));
        when(fundFlowMapper.selectOne(any())).thenReturn(existing);

        assertThrows(IllegalStateException.class, () -> service.record(flow("pay-001", "IN", "41.00")));
    }

    @Test
    void missingIdempotencyKeyShouldBeRejected() {
        FundFlow flow = flow("", "IN", "40.00");

        assertThrows(IllegalArgumentException.class, () -> service.record(flow));
        verify(fundFlowMapper, never()).insert(any(FundFlow.class));
    }

    @Test
    void invalidDirectionShouldBeRejected() {
        FundFlow flow = flow("pay-002", "SIDEWAYS", "40.00");

        assertThrows(IllegalArgumentException.class, () -> service.record(flow));
        verify(fundFlowMapper, never()).insert(any(FundFlow.class));
    }

    @Test
    void amountWithThreeDecimalsShouldBeRejected() {
        stubSettlement(TENANT.toString());
        FundFlow flow = flow("pay-003", "IN", "40.005");

        assertThrows(IllegalArgumentException.class, () -> service.record(flow));
        verify(fundFlowMapper, never()).insert(any(FundFlow.class));
    }

    private void stubSettlement(String tenantId) {
        SettlementOrder settlement = new SettlementOrder();
        settlement.setId("stl-1");
        settlement.setTenantId(tenantId);
        settlement.setSettlementNo("STL-1");
        settlement.setDeleted(false);
        when(settlementOrderMapper.selectById("stl-1")).thenReturn(settlement);
    }

    private FundFlow flow(String key, String direction, String amount) {
        FundFlow flow = new FundFlow();
        flow.setIdempotencyKey(key);
        flow.setDirection(direction);
        flow.setAmount(new BigDecimal(amount));
        flow.setSourceType("SETTLEMENT_PAYMENT");
        flow.setSettlementId("stl-1");
        return flow;
    }
}

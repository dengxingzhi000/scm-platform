package com.cognora.scm.finance.service.impl;

import com.cognora.platform.common.tenant.TenantContextHolder;
import com.cognora.scm.finance.domain.entity.FreightRule;
import com.cognora.scm.finance.domain.entity.PlatformServiceFee;
import com.cognora.scm.finance.domain.entity.ReconciliationRecord;
import com.cognora.scm.finance.domain.entity.SettlementItem;
import com.cognora.scm.finance.domain.entity.SettlementOrder;
import com.cognora.scm.finance.mapper.FreightRuleMapper;
import com.cognora.scm.finance.mapper.PlatformServiceFeeMapper;
import com.cognora.scm.finance.mapper.ReconciliationRecordMapper;
import com.cognora.scm.finance.mapper.SettlementItemMapper;
import com.cognora.scm.finance.mapper.SettlementOrderMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FinanceCrudServiceImplTest {

    private static final UUID TENANT = UUID.fromString("019a0aee-0000-7000-8000-000000000002");

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(TENANT);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    private void attach(Object service, Object mapper) {
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
    }

    @Test
    void freightRuleCreateShouldInitializeFields() {
        FreightRuleMapper mapper = mock(FreightRuleMapper.class);
        FreightRuleServiceImpl service = new FreightRuleServiceImpl();
        attach(service, mapper);

        FreightRule rule = new FreightRule();
        service.createRule(rule);

        assertFalse(rule.getDeleted());
        assertNotNull(rule.getCreateTime());
        assertNotNull(rule.getUpdateTime());
        verify(mapper).insert(rule);
    }

    @Test
    void freightRuleUpdateShouldSetIdAndRefreshTime() {
        FreightRuleMapper mapper = mock(FreightRuleMapper.class);
        FreightRuleServiceImpl service = new FreightRuleServiceImpl();
        attach(service, mapper);

        FreightRule rule = new FreightRule();
        service.updateRule("rule-1", rule);

        assertNotNull(rule.getUpdateTime());
        verify(mapper).updateById(rule);
    }

    @Test
    void freightRuleDeleteShouldSoftDeleteExistingRule() {
        FreightRuleMapper mapper = mock(FreightRuleMapper.class);
        FreightRuleServiceImpl service = new FreightRuleServiceImpl();
        attach(service, mapper);
        FreightRule rule = new FreightRule();
        when(mapper.selectById("rule-1")).thenReturn(rule);

        service.deleteRule("rule-1");

        assertTrue(rule.getDeleted());
        verify(mapper).updateById(rule);
    }

    @Test
    void freightRuleDeleteShouldIgnoreMissingRule() {
        FreightRuleMapper mapper = mock(FreightRuleMapper.class);
        FreightRuleServiceImpl service = new FreightRuleServiceImpl();
        attach(service, mapper);

        service.deleteRule("missing");

        verify(mapper, never()).updateById(any(FreightRule.class));
    }

    @Test
    void settlementOrderUpdateAndDeleteShouldKeepOrchestrationInService() {
        SettlementOrderMapper mapper = mock(SettlementOrderMapper.class);
        SettlementOrderServiceImpl service = new SettlementOrderServiceImpl(null);
        attach(service, mapper);

        SettlementOrder existingOrder = new SettlementOrder();
        existingOrder.setId("order-1");
        existingOrder.setTenantId(TENANT.toString());
        existingOrder.setStatus(0);
        existingOrder.setTotalAmount(new BigDecimal("100.00"));
        existingOrder.setDiscountAmount(new BigDecimal("0.00"));
        existingOrder.setAdjustmentAmount(new BigDecimal("0.00"));
        existingOrder.setActualAmount(new BigDecimal("100.00"));
        existingOrder.setPaidAmount(new BigDecimal("0.00"));
        existingOrder.setUnpaidAmount(new BigDecimal("100.00"));
        existingOrder.setDeleted(false);
        when(mapper.selectById("order-1")).thenReturn(existingOrder);
        when(mapper.updateById(any(SettlementOrder.class))).thenReturn(1);

        service.updateSettlement("order-1", new SettlementOrder());

        assertNotNull(existingOrder.getUpdateTime());
        verify(mapper).updateById(existingOrder);

        SettlementOrder existing = new SettlementOrder();
        when(mapper.selectById("order-2")).thenReturn(existing);
        service.deleteSettlement("order-2");

        assertTrue(existing.getDeleted());
        verify(mapper).updateById(existing);
    }

    @Test
    void reconciliationUpdateAndDeleteShouldKeepOrchestrationInService() {
        ReconciliationRecordMapper mapper = mock(ReconciliationRecordMapper.class);
        ReconciliationRecordServiceImpl service = new ReconciliationRecordServiceImpl();
        attach(service, mapper);

        ReconciliationRecord existingRecord = new ReconciliationRecord();
        existingRecord.setId("rec-1");
        existingRecord.setStatus(0);
        existingRecord.setDeleted(false);
        when(mapper.selectById("rec-1")).thenReturn(existingRecord);
        when(mapper.updateById(any(ReconciliationRecord.class))).thenReturn(1);

        service.updateReconciliation("rec-1", new ReconciliationRecord());

        assertNotNull(existingRecord.getUpdateTime());
        verify(mapper).updateById(existingRecord);

        ReconciliationRecord existing = new ReconciliationRecord();
        when(mapper.selectById("rec-2")).thenReturn(existing);
        service.deleteReconciliation("rec-2");

        assertTrue(existing.getDeleted());
        verify(mapper).updateById(existing);
    }

    @Test
    void platformServiceFeeCreateShouldInitializeIdStatusAndTimes() {
        PlatformServiceFeeMapper mapper = mock(PlatformServiceFeeMapper.class);
        PlatformServiceFeeServiceImpl service = new PlatformServiceFeeServiceImpl();
        attach(service, mapper);

        PlatformServiceFee fee = new PlatformServiceFee();
        fee.setTotalFee(new BigDecimal("10.00"));
        fee.setTenantId("019a0aee-0000-7000-8000-000000000099");
        service.createFee(fee);

        assertNotNull(fee.getId());
        assertNotNull(fee.getCreateTime());
        assertNotNull(fee.getUpdateTime());
        assertEquals(TENANT.toString(), fee.getTenantId());
        assertEquals("10.00", fee.getFinalFee().toPlainString());
        verify(mapper).insert(fee);
    }

    @Test
    void reconciliationCreateShouldForceTenantAndNormalizeDiff() {
        ReconciliationRecordMapper mapper = mock(ReconciliationRecordMapper.class);
        ReconciliationRecordServiceImpl service = new ReconciliationRecordServiceImpl();
        attach(service, mapper);
        when(mapper.insert(any(ReconciliationRecord.class))).thenReturn(1);

        ReconciliationRecord record = new ReconciliationRecord();
        record.setTenantId("019a0aee-0000-7000-8000-000000000099");
        record.setOurTotalAmount(new BigDecimal("100.00"));
        record.setTheirTotalAmount(new BigDecimal("90.00"));
        service.createReconciliation(record);

        assertEquals(TENANT.toString(), record.getTenantId());
        assertEquals("10.00", record.getDiffAmount().toPlainString());
        assertTrue(record.getHasDiff());
        assertEquals("0.00", record.getOurPaidAmount().toPlainString());
        verify(mapper).insert(record);
    }

    @Test
    void reconciliationCreateShouldRejectThreeDecimalAmount() {
        ReconciliationRecordMapper mapper = mock(ReconciliationRecordMapper.class);
        ReconciliationRecordServiceImpl service = new ReconciliationRecordServiceImpl();
        attach(service, mapper);

        ReconciliationRecord record = new ReconciliationRecord();
        record.setOurTotalAmount(new BigDecimal("100.005"));

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> service.createReconciliation(record));
        verify(mapper, never()).insert(any(ReconciliationRecord.class));
    }

    @Test
    void platformServiceFeePaymentShouldRejectThreeDecimalAmount() {
        PlatformServiceFeeMapper mapper = mock(PlatformServiceFeeMapper.class);
        PlatformServiceFeeServiceImpl service = new PlatformServiceFeeServiceImpl();
        attach(service, mapper);

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> service.recordPayment("fee-1", new BigDecimal("10.005")));
        verify(mapper, never()).selectById("fee-1");
    }

    @Test
    void settlementItemCreateShouldInitializeIdAndCreateTime() {
        SettlementItemMapper mapper = mock(SettlementItemMapper.class);
        SettlementItemServiceImpl service = new SettlementItemServiceImpl();
        attach(service, mapper);

        SettlementItem item = new SettlementItem();
        service.createItem(item);

        assertNotNull(item.getId());
        assertNotNull(item.getCreateTime());
        verify(mapper).insert(item);
    }
}
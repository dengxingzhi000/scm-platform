package com.scmcloud.finance.service.impl;

import com.scmcloud.finance.domain.entity.FreightRule;
import com.scmcloud.finance.domain.entity.PlatformServiceFee;
import com.scmcloud.finance.domain.entity.ReconciliationRecord;
import com.scmcloud.finance.domain.entity.SettlementItem;
import com.scmcloud.finance.domain.entity.SettlementOrder;
import com.scmcloud.finance.mapper.FreightRuleMapper;
import com.scmcloud.finance.mapper.PlatformServiceFeeMapper;
import com.scmcloud.finance.mapper.ReconciliationRecordMapper;
import com.scmcloud.finance.mapper.SettlementItemMapper;
import com.scmcloud.finance.mapper.SettlementOrderMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FinanceCrudServiceImplTest {

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

        SettlementOrder order = new SettlementOrder();
        service.updateSettlement("order-1", order);

        assertNotNull(order.getUpdateTime());
        verify(mapper).updateById(order);

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

        ReconciliationRecord record = new ReconciliationRecord();
        service.updateReconciliation("rec-1", record);

        assertNotNull(record.getUpdateTime());
        verify(mapper).updateById(record);

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
        service.createFee(fee);

        assertNotNull(fee.getId());
        assertNotNull(fee.getCreateTime());
        assertNotNull(fee.getUpdateTime());
        verify(mapper).insert(fee);
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

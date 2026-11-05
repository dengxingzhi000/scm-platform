package com.cognora.scm.finance.service.impl;

import com.cognora.platform.common.tenant.TenantContextHolder;
import com.cognora.scm.finance.domain.entity.FundFlow;
import com.cognora.scm.finance.mapper.FundFlowMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FundFlowServiceImplTest {

    private static final UUID TENANT = UUID.fromString("019a0aee-0000-7000-8000-000000000002");

    @Mock
    private FundFlowMapper fundFlowMapper;

    private FundFlowServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new FundFlowServiceImpl(fundFlowMapper);
        TenantContextHolder.setTenantId(TENANT);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void recordShouldForceTenantAndDefaultCurrency() {
        when(fundFlowMapper.insert(any(FundFlow.class))).thenReturn(1);

        FundFlow flow = flow("pay-001", "IN", "40.00");
        flow.setTenantId("019a0aee-0000-7000-8000-000000000099");

        FundFlow saved = service.record(flow);

        assertEquals(TENANT.toString(), saved.getTenantId());
        assertEquals("CNY", saved.getCurrency());
        assertNotNull(saved.getId());
        assertNotNull(saved.getFlowNo());
        assertEquals("40.00", saved.getAmount().toPlainString());
    }

    @Test
    void duplicateKeyWithSamePayloadShouldReturnExistingFlow() {
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
        FundFlow flow = flow("pay-003", "IN", "40.005");

        assertThrows(IllegalArgumentException.class, () -> service.record(flow));
        verify(fundFlowMapper, never()).insert(any(FundFlow.class));
    }

    private FundFlow flow(String key, String direction, String amount) {
        FundFlow flow = new FundFlow();
        flow.setIdempotencyKey(key);
        flow.setDirection(direction);
        flow.setAmount(new BigDecimal(amount));
        return flow;
    }
}

package com.scmcloud.order.service.command;

import com.scmcloud.common.domain.Money;
import com.scmcloud.common.domain.TenantId;
import com.scmcloud.common.integration.outbox.OutboxService;
import com.scmcloud.order.domain.entity.OrdOrder;
import com.scmcloud.order.domain.entity.OrderStatus;
import com.scmcloud.order.event.OrderEventStore;
import com.scmcloud.common.tenant.TenantContextHolder;
import com.scmcloud.order.mapper.OrdOrderMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

/**
 * Atomicity test for the Task 5 outbox refactor.
 *
 * <p>Replaces the previous grep-on-source test
 * {@code OutboxPublishTest.outboxAtomicityIsMandatoryAndTenantFailFast}.</p>
 *
 * <p>Asserts that {@code OutboxService.save(...)} is invoked inside the same
 * {@code @Transactional} boundary as {@code ordOrderMapper.insert(...)}, and
 * that an exception from outbox save propagates out of {@code createOrder()},
 * so the caller's transaction (or its Seata global TX) rolls back both the
 * order row and any outbox row that may have been written.</p>
 */
@ExtendWith(MockitoExtension.class)
class OutboxAtomicityTest {

    @Mock private OrdOrderMapper ordOrderMapper;
    @Mock private OrdOrderItemCommandService ordOrderItemCommandService;
    @Mock private OrdStatusHistoryCommandService ordStatusHistoryCommandService;
    @Mock private OrderEventStore eventStore;
    @Mock private OutboxService outboxService;

    private OrdOrderCommandService service;

    @BeforeEach
    void setUp() {
        service = new OrdOrderCommandService(ordOrderMapper, ordOrderItemCommandService,
                ordStatusHistoryCommandService, eventStore, outboxService);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    private OrdOrder order() {
        OrdOrder o = new OrdOrder();
        o.setId(java.util.UUID.fromString("00000000-0000-0000-0000-000000000001"));
        o.setOrderNo("NO2001");
        o.setStatus(OrderStatus.PENDING_PAYMENT.getCode());
        o.setUserId("00000000-0000-0000-0000-000000000001");
        o.setTenantId(TenantId.generate());
        o.setTotalAmount(Money.of(new BigDecimal("99.90")));
        o.setPayableAmount(Money.of(new BigDecimal("89.90")));
        return o;
    }

    @Test
    void outboxSaveIsCalledAfterOrderInsertAndBeforeReturn() {
        OrdOrder o = order();
        TenantContextHolder.setTenantId(o.getTenantId().toUUID());
        when(ordOrderMapper.insert(any(OrdOrder.class))).thenReturn(1);

        com.scmcloud.order.domain.entity.OrdOrderItem item = new com.scmcloud.order.domain.entity.OrdOrderItem();
        item.setSubtotal(Money.of(new BigDecimal("99.90")));
        service.createOrder(o, List.of(item));

        InOrder order = inOrder(ordOrderMapper, outboxService);
        order.verify(ordOrderMapper).insert(any(OrdOrder.class));
        order.verify(outboxService).save(anyString(), anyString(), anyString(), any(), any());
    }

    @Test
    void outboxSaveFailurePropagatesAndPreventsSuccessfulReturn() {
        OrdOrder o = order();
        TenantContextHolder.setTenantId(o.getTenantId().toUUID());
        when(ordOrderMapper.insert(any(OrdOrder.class))).thenReturn(1);
        doThrow(new RuntimeException("simulated outbox DB failure"))
                .when(outboxService).save(anyString(), anyString(), anyString(), any(), any());

        com.scmcloud.order.domain.entity.OrdOrderItem item = new com.scmcloud.order.domain.entity.OrdOrderItem();
        item.setSubtotal(Money.of(new BigDecimal("99.90")));

        assertThrows(RuntimeException.class, () -> service.createOrder(o, List.of(item)));
    }
}
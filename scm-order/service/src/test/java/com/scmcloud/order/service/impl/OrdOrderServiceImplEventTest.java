package com.scmcloud.order.service.impl;

import com.scmcloud.common.domain.Money;
import com.scmcloud.common.domain.TenantId;
import com.scmcloud.order.domain.entity.OrdOrder;
import com.scmcloud.order.domain.entity.OrderStatus;
import com.scmcloud.order.event.OrderCreatedEvent;
import com.scmcloud.order.event.OrderEvent;
import com.scmcloud.order.event.OrderEventStore;
import com.scmcloud.order.event.OrderStatusChangedEvent;
import com.scmcloud.order.mapper.OrdOrderMapper;
import com.scmcloud.order.service.IOrdOrderItemService;
import com.scmcloud.order.service.IOrdStatusHistoryService;
import com.scmcloud.system.api.StatusMachineDubboService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrdOrderServiceImplEventTest {

    @Mock private IOrdOrderItemService orderItemService;
    @Mock private IOrdStatusHistoryService statusHistoryService;
    @Mock private OrderEventStore eventStore;
    @Mock private StatusMachineDubboService statusMachine;
    @Mock private OrdOrderMapper ordOrderMapper;

    private OrdOrderServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new OrdOrderServiceImpl(orderItemService, statusHistoryService, eventStore);
        ReflectionTestUtils.setField(service, "baseMapper", ordOrderMapper);
        ReflectionTestUtils.setField(service, "statusMachine", statusMachine);
    }

    private OrdOrder order(int statusCode) {
        OrdOrder order = new OrdOrder();
        order.setId(1L);
        order.setOrderNo("NO1001");
        order.setStatus(statusCode);
        order.setUserId("00000000-0000-0000-0000-000000000001");
        order.setTenantId(TenantId.generate());
        order.setTotalAmount(Money.of(new BigDecimal("99.90")));
        order.setPayableAmount(Money.of(new BigDecimal("89.90")));
        return order;
    }

    @Test
    void createOrderShouldAppendOrderCreatedEvent() {
        OrdOrder order = order(OrderStatus.PENDING_PAYMENT.getCode());
        when(ordOrderMapper.insert(any(OrdOrder.class))).thenReturn(1);

        com.scmcloud.order.domain.entity.OrdOrderItem item = new com.scmcloud.order.domain.entity.OrdOrderItem();
        item.setSubtotal(Money.of(new BigDecimal("99.90")));
        service.createOrder(order, List.of(item));

        ArgumentCaptor<OrderEvent> captor = ArgumentCaptor.forClass(OrderEvent.class);
        verify(eventStore).append(captor.capture());
        OrderCreatedEvent event = assertInstanceOf(OrderCreatedEvent.class, captor.getValue());
        assertEquals(1L, event.getOrderId());
        assertEquals("NO1001", event.getOrderNo());
        assertEquals(order.getTenantId().toUUID(), event.getTenantId());
        assertEquals("00000000-0000-0000-0000-000000000001", event.getUserId());
    }

    @Test
    void updateOrderStatusShouldAppendStatusChangedEvent() {
        OrdOrder existing = order(OrderStatus.PAID.getCode());
        when(ordOrderMapper.selectById(1L)).thenReturn(existing);
        when(statusMachine.canTransition("ORDER", "PAID", "PENDING_SHIP"))
                .thenReturn(new StatusMachineDubboService.TransitionCheckDTO(
                        true, "ORDER", "PAID", "PENDING_SHIP", null));
        when(ordOrderMapper.updateById(any(OrdOrder.class))).thenReturn(1);

        service.updateOrderStatus(1L, OrderStatus.PENDING_SHIP.getCode());

        ArgumentCaptor<OrderEvent> captor = ArgumentCaptor.forClass(OrderEvent.class);
        verify(eventStore).append(captor.capture());
        OrderStatusChangedEvent event =
                assertInstanceOf(OrderStatusChangedEvent.class, captor.getValue());
        assertEquals(OrderStatus.PAID, event.getFromStatus());
        assertEquals(OrderStatus.PENDING_SHIP, event.getToStatus());
    }

    @Test
    void updateOrderStatusShouldNotAppendWhenUpdateMisses() {
        OrdOrder existing = order(OrderStatus.PAID.getCode());
        when(ordOrderMapper.selectById(1L)).thenReturn(existing);
        when(statusMachine.canTransition("ORDER", "PAID", "PENDING_SHIP"))
                .thenReturn(new StatusMachineDubboService.TransitionCheckDTO(
                        true, "ORDER", "PAID", "PENDING_SHIP", null));
        when(ordOrderMapper.updateById(any(OrdOrder.class))).thenReturn(0);

        boolean updated = service.updateOrderStatus(1L, OrderStatus.PENDING_SHIP.getCode());

        assertEquals(false, updated);
        verify(eventStore, never()).append(any());
    }
}

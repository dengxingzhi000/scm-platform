package com.scmcloud.order.event;

import com.scmcloud.order.domain.entity.OrderStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrderAggregateTest {

    private final UUID tenantId = UUID.randomUUID();

    @Test
    void createShouldApplyCreatedEventAndStartPendingPayment() {
        OrderAggregate aggregate = OrderAggregate.create(
                tenantId, 1L, "NO1001", "u-1", new BigDecimal("99.90"), new BigDecimal("89.90"));

        assertEquals(1L, aggregate.getOrderId());
        assertEquals("NO1001", aggregate.getOrderNo());
        assertEquals(OrderStatus.PENDING_PAYMENT, aggregate.getStatus());
        assertEquals(tenantId, aggregate.getTenantId());
        assertEquals("u-1", aggregate.getUserId());
        assertEquals(1, aggregate.getUncommittedEvents().size());
    }

    @Test
    void changeStatusShouldValidateTransition() {
        OrderAggregate aggregate = OrderAggregate.create(
                tenantId, 1L, "NO1001", "u-1", BigDecimal.TEN, BigDecimal.TEN);

        assertThrows(IllegalStateException.class,
                () -> aggregate.changeStatus(OrderStatus.COMPLETED));
    }

    @Test
    void changeStatusShouldAppendEventOnValidTransition() {
        OrderAggregate aggregate = OrderAggregate.create(
                tenantId, 1L, "NO1001", "u-1", BigDecimal.TEN, BigDecimal.TEN);

        aggregate.changeStatus(OrderStatus.PAID);

        assertEquals(OrderStatus.PAID, aggregate.getStatus());
        assertEquals(2, aggregate.getUncommittedEvents().size());
        OrderStatusChangedEvent event =
                assertInstanceOf(OrderStatusChangedEvent.class,
                        aggregate.getUncommittedEvents().get(1));
        assertEquals(OrderStatus.PENDING_PAYMENT, event.getFromStatus());
        assertEquals(OrderStatus.PAID, event.getToStatus());
    }

    @Test
    void rehydrateShouldRebuildStateFromHistory() {
        List<OrderEvent> history = List.of(
                new OrderCreatedEvent(tenantId, 1L, "NO1001", "u-1",
                        new BigDecimal("99.90"), new BigDecimal("89.90")),
                new OrderStatusChangedEvent(tenantId, 1L, "NO1001",
                        OrderStatus.PENDING_PAYMENT, OrderStatus.PAID),
                new OrderStatusChangedEvent(tenantId, 1L, "NO1001",
                        OrderStatus.PAID, OrderStatus.PENDING_SHIP));

        OrderAggregate aggregate = OrderAggregate.rehydrate(history);

        assertEquals(OrderStatus.PENDING_SHIP, aggregate.getStatus());
        assertEquals("u-1", aggregate.getUserId());
        assertEquals(new BigDecimal("99.90"), aggregate.getTotalAmount());
        assertNull(aggregate.getUncommittedEvents()); // replay must not create uncommitted events
    }

    @Test
    void rehydrateShouldRejectHistoryWithoutCreationEvent() {
        List<OrderEvent> history = List.of(new OrderStatusChangedEvent(
                tenantId, 1L, "NO1001", OrderStatus.PAID, OrderStatus.CANCELLED));

        assertThrows(IllegalStateException.class, () -> OrderAggregate.rehydrate(history));
    }
}

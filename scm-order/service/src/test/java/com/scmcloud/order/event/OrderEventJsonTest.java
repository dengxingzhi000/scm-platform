package com.scmcloud.order.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.scmcloud.order.domain.entity.OrderStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class OrderEventJsonTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .findAndRegisterModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Test
    void shouldRoundTripOrderCreatedEventPolymorphically() throws Exception {
        java.time.Instant ts = java.time.Instant.parse("2026-08-26T00:00:00Z");
        OrderCreatedEvent original = new OrderCreatedEvent(
                UUID.fromString("550e8400-e29b-41d4-a716-446655440000"),
                UUID.fromString("660e8400-e29b-41d4-a716-446655440001"),
                1L, "NO1001", ts, "ORDER_CREATED",
                "u-1", new BigDecimal("99.90"), new BigDecimal("89.90"));

        String json = objectMapper.writeValueAsString(original);
        OrderEvent deserialized = objectMapper.readValue(json, OrderEvent.class);

        OrderCreatedEvent restored = assertInstanceOf(OrderCreatedEvent.class, deserialized);
        assertEquals(original.getEventType(), restored.getEventType());
        assertEquals(original.getTenantId(), restored.getTenantId());
        assertEquals(original.getOrderId(), restored.getOrderId());
        assertEquals(original.getOrderNo(), restored.getOrderNo());
        assertEquals(original.getUserId(), restored.getUserId());
        assertEquals(original.getTotalAmount(), restored.getTotalAmount());
        assertEquals(original.getPayableAmount(), restored.getPayableAmount());
        assertEquals(original.getEventId(), restored.getEventId());
        assertEquals(ts, restored.getTimestamp());
    }

    @Test
    void shouldRoundTripOrderStatusChangedEventPolymorphically() throws Exception {
        java.time.Instant ts = java.time.Instant.parse("2026-08-26T01:00:00Z");
        OrderStatusChangedEvent original = new OrderStatusChangedEvent(
                UUID.fromString("550e8400-e29b-41d4-a716-446655440000"),
                UUID.fromString("660e8400-e29b-41d4-a716-446655440001"),
                2L, "NO1002", ts, "ORDER_STATUS_CHANGED",
                OrderStatus.PAID, OrderStatus.PENDING_SHIP);

        String json = objectMapper.writeValueAsString(original);
        OrderEvent deserialized = objectMapper.readValue(json, OrderEvent.class);

        OrderStatusChangedEvent restored = assertInstanceOf(OrderStatusChangedEvent.class, deserialized);
        assertEquals(OrderStatus.PAID, restored.getFromStatus());
        assertEquals(OrderStatus.PENDING_SHIP, restored.getToStatus());
        assertEquals(2L, restored.getOrderId());
        assertEquals(original.getTenantId(), restored.getTenantId());
        assertEquals("NO1002", restored.getOrderNo());
        assertEquals(original.getEventId(), restored.getEventId());
        assertEquals(ts, restored.getTimestamp());
    }

    @Test
    void equalsShouldBeBasedOnEventId() {
        UUID eventId = UUID.randomUUID();
        UUID tenantA = UUID.randomUUID();
        UUID tenantB = UUID.randomUUID();
        OrderCreatedEvent a = new OrderCreatedEvent(eventId, tenantA, 1L, "NO1001",
                Instant.EPOCH, "ORDER_CREATED", "u-1", BigDecimal.ONE, BigDecimal.ONE);
        OrderCreatedEvent b = new OrderCreatedEvent(eventId, tenantB, 1L, "NO2002",
                Instant.EPOCH, "ORDER_CREATED", "u-2", BigDecimal.TEN, BigDecimal.TEN);
        OrderCreatedEvent c = new OrderCreatedEvent(UUID.randomUUID(), tenantA, 1L, "NO1001",
                Instant.EPOCH, "ORDER_CREATED", "u-1", BigDecimal.ONE, BigDecimal.ONE);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
    }
}

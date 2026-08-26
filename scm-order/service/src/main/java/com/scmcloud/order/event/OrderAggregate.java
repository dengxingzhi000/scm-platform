package com.scmcloud.order.event;

import com.scmcloud.order.domain.entity.OrderStatus;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class OrderAggregate {

    private UUID tenantId;
    private Long orderId;
    private String orderNo;
    private BigDecimal totalAmount;
    private BigDecimal payableAmount;
    private OrderStatus status;
    private List<OrderEvent> uncommittedEvents = new ArrayList<>();

    public static OrderAggregate create(UUID tenantId, Long orderId, String orderNo,
                                        BigDecimal totalAmount, BigDecimal payableAmount) {
        OrderAggregate aggregate = new OrderAggregate();
        OrderCreatedEvent event = new OrderCreatedEvent(
                tenantId, orderId, orderNo, null, totalAmount, payableAmount);
        aggregate.apply(event);
        aggregate.uncommittedEvents.add(event);
        return aggregate;
    }

    public void changeStatus(OrderStatus newStatus) {
        OrderStatusChangedEvent event =
                new OrderStatusChangedEvent(tenantId, orderId, orderNo, this.status, newStatus);
        apply(event);
        uncommittedEvents.add(event);
    }

    public void apply(OrderEvent event) {
        if (event instanceof OrderCreatedEvent e) {
            this.tenantId = e.getTenantId();
            this.orderId = e.getOrderId();
            this.orderNo = e.getOrderNo();
            this.totalAmount = e.getTotalAmount();
            this.payableAmount = e.getPayableAmount();
            this.status = OrderStatus.PENDING_PAYMENT;
        } else if (event instanceof OrderStatusChangedEvent e) {
            this.status = e.getToStatus();
        }
    }

    public List<OrderEvent> getUncommittedEvents() {
        return uncommittedEvents;
    }

    public void clearUncommittedEvents() {
        uncommittedEvents.clear();
    }
}

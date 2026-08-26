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
    private String userId;
    private long version;
    private List<OrderEvent> uncommittedEvents;

    public static OrderAggregate create(UUID tenantId, Long orderId, String orderNo, String userId,
                                        BigDecimal totalAmount, BigDecimal payableAmount) {
        OrderAggregate aggregate = new OrderAggregate();
        OrderCreatedEvent event = new OrderCreatedEvent(
                tenantId, orderId, orderNo, userId, totalAmount, payableAmount);
        aggregate.apply(event);
        aggregate.ensureUncommitted();
        aggregate.uncommittedEvents.add(event);
        return aggregate;
    }

    /**
     * 从历史事件重放重建聚合。首事件必须是 ORDER_CREATED。
     */
    public static OrderAggregate rehydrate(List<OrderEvent> history) {
        if (history == null || history.isEmpty()
                || !(history.get(0) instanceof OrderCreatedEvent)) {
            throw new IllegalStateException(
                    "Cannot rehydrate order aggregate: history must start with ORDER_CREATED");
        }
        OrderAggregate aggregate = new OrderAggregate();
        history.forEach(event -> {
            aggregate.apply(event);
            aggregate.version++;
        });
        return aggregate;
    }

    public void changeStatus(OrderStatus newStatus) {
        if (status != null && !status.canTransitionTo(newStatus)) {
            throw new IllegalStateException(
                    "Invalid order status transition: " + status + " -> " + newStatus);
        }
        OrderStatusChangedEvent event =
                new OrderStatusChangedEvent(tenantId, orderId, orderNo, this.status, newStatus);
        apply(event);
        ensureUncommitted();
        uncommittedEvents.add(event);
    }

    public void apply(OrderEvent event) {
        if (event instanceof OrderCreatedEvent e) {
            this.tenantId = e.getTenantId();
            this.orderId = e.getOrderId();
            this.orderNo = e.getOrderNo();
            this.totalAmount = e.getTotalAmount();
            this.payableAmount = e.getPayableAmount();
            this.userId = e.getUserId();
            this.status = OrderStatus.PENDING_PAYMENT;
        } else if (event instanceof OrderStatusChangedEvent e) {
            this.status = e.getToStatus();
        }
    }

    public List<OrderEvent> getUncommittedEvents() {
        return uncommittedEvents;
    }

    public void clearUncommittedEvents() {
        if (uncommittedEvents != null) {
            uncommittedEvents.clear();
        }
    }

    public UUID getTenantId() { return tenantId; }
    public Long getOrderId() { return orderId; }
    public String getOrderNo() { return orderNo; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public BigDecimal getPayableAmount() { return payableAmount; }
    public String getUserId() { return userId; }
    public OrderStatus getStatus() { return status; }
    public long getVersion() { return version; }

    private void ensureUncommitted() {
        if (uncommittedEvents == null) {
            uncommittedEvents = new ArrayList<>();
        }
    }
}

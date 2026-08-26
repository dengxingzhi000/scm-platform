package com.scmcloud.order.event;

import com.scmcloud.order.domain.entity.OrderStatus;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * 订单聚合根。可变对象，非线程安全，仅限命令处理场景使用。
 */
public class OrderAggregate {

    private UUID tenantId;
    private Long orderId;
    private String orderNo;
    private BigDecimal totalAmount;
    private BigDecimal payableAmount;
    private OrderStatus status;
    private String userId;
    /**
     * 已应用事件总数(命令与重放均计入)。
     */
    private long version;
    private List<OrderEvent> uncommittedEvents;

    private OrderAggregate() {
    }

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
        aggregate.apply(history.get(0));
        for (int i = 1; i < history.size(); i++) {
            OrderEvent event = history.get(i);
            aggregate.validateReplayEvent(event);
            aggregate.apply(event);
        }
        return aggregate;
    }

    private void validateReplayEvent(OrderEvent event) {
        if (event instanceof OrderCreatedEvent) {
            throw new IllegalStateException("duplicate ORDER_CREATED in history");
        }
        if (!Objects.equals(event.getOrderId(), orderId)
                || !Objects.equals(event.getOrderNo(), orderNo)) {
            throw new IllegalStateException(
                    "Replayed event belongs to a different order: expected "
                            + orderId + "/" + orderNo + ", got "
                            + event.getOrderId() + "/" + event.getOrderNo());
        }
        if (event instanceof OrderStatusChangedEvent e) {
            if (e.getFromStatus() != status
                    || !status.canTransitionTo(e.getToStatus())) {
                throw new IllegalStateException(
                        "Invalid status transition in history: current=" + status
                                + ", event=" + e.getFromStatus() + " -> " + e.getToStatus());
            }
        }
    }

    public void changeStatus(OrderStatus newStatus) {
        Objects.requireNonNull(newStatus, "newStatus");
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

    void apply(OrderEvent event) {
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
        version++;
    }

    public List<OrderEvent> getUncommittedEvents() {
        return uncommittedEvents == null ? null : Collections.unmodifiableList(uncommittedEvents);
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
    /** 已应用事件总数(命令与重放均计入)。 */
    public long getVersion() { return version; }

    private void ensureUncommitted() {
        if (uncommittedEvents == null) {
            uncommittedEvents = new ArrayList<>();
        }
    }
}

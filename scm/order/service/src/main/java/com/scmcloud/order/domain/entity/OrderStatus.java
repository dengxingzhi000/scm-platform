package com.scmcloud.order.domain.entity;

import lombok.Getter;

import java.util.Set;
import java.util.UUID;

/**
 * Order status enumeration with transition rules.
 * Replaces magic integers (0, 1, 2, ..., 9) used throughout the codebase.
 *
 * <p>State machine:
 * <pre>
 * PENDING_PAYMENT ──→ PAID ──→ PENDING_SHIP ──→ SHIPPED ──→ IN_TRANSIT ──→ DELIVERED ──→ COMPLETED
 *       │                │
 *       └──→ CANCELLED ←─┘
 *                 REFUNDING ←── PAID..DELIVERED
 *                 REFUNDED  ←── REFUNDING
 * </pre>
 */
@Getter
public enum OrderStatus {

    PENDING_PAYMENT(0, "待支付"),
    PAID(1, "已支付"),
    PENDING_SHIP(2, "待发货"),
    SHIPPED(3, "已发货"),
    IN_TRANSIT(4, "运输中"),
    DELIVERED(5, "已送达"),
    COMPLETED(6, "已完成"),
    CANCELLED(7, "已取消"),
    REFUNDING(8, "退款中"),
    REFUNDED(9, "已退款");

    private final int code;
    private final String description;

    OrderStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public static OrderStatus fromCode(int code) {
        for (OrderStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown order status code: " + code);
    }

    /**
     * Returns the set of valid next statuses from this status.
     */
    public Set<OrderStatus> validNextStatuses() {
        return switch (this) {
            case PENDING_PAYMENT -> Set.of(PAID, CANCELLED);
            case PAID -> Set.of(PENDING_SHIP, CANCELLED, REFUNDING);
            case PENDING_SHIP -> Set.of(SHIPPED, REFUNDING);
            case SHIPPED -> Set.of(IN_TRANSIT, REFUNDING);
            case IN_TRANSIT -> Set.of(DELIVERED, REFUNDING);
            case DELIVERED -> Set.of(COMPLETED, REFUNDING);
            case COMPLETED -> Set.of(); // terminal
            case CANCELLED -> Set.of(); // terminal
            case REFUNDING -> Set.of(REFUNDED);
            case REFUNDED -> Set.of(); // terminal
        };
    }

    /**
     * Checks if a transition from this status to the target is valid.
     */
    public boolean canTransitionTo(OrderStatus target) {
        return validNextStatuses().contains(target);
    }

    /**
     * Returns true if this is a terminal status (no further transitions).
     */
    public boolean isTerminal() {
        return validNextStatuses().isEmpty();
    }

    /**
     * Returns true if the order can be cancelled from this status.
     */
    public boolean isCancellable() {
        return this == PENDING_PAYMENT || this == PAID;
    }

    /**
     * Returns true if the order is in a paid-or-later state.
     */
    public boolean isPaid() {
        return this == PAID || this == PENDING_SHIP || this == SHIPPED
                || this == IN_TRANSIT || this == DELIVERED || this == COMPLETED;
    }
}

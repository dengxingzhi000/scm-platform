package com.scmcloud.order.domain.entity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.Version;
import com.scmcloud.common.domain.Money;
import com.scmcloud.common.domain.Quantity;
import com.scmcloud.common.domain.TenantId;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 订单表（分区表）
 * </p>
 *
 * @author deng
 * @since 2025-12-25
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("ord_order")
public class OrdOrder {

    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    private TenantId tenantId;

    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private UUID id;
    @TableField("order_no")
    private String orderNo;
    @TableField("user_id")
    private String userId;
    @TableField("username")
    private String username;
    @TableField("order_type")
    private Integer orderType;
    @TableField("order_source")
    private String orderSource;
    @TableField("status")
    private Integer status;
    @Version
    @TableField("version")
    private Integer version;
    @TableField("total_amount")
    private Money totalAmount;
    @TableField("discount_amount")
    private Money discountAmount;
    @TableField("freight_amount")
    private Money freightAmount;
    @TableField("payable_amount")
    private Money payableAmount;
    @TableField("payment_method")
    private Integer paymentMethod;
    @TableField("payment_no")
    private String paymentNo;
    @TableField("paid_amount")
    private Money paidAmount;
    @TableField("paid_at")
    private LocalDateTime paidAt;
    @TableField("shipping_address")
    private String shippingAddress;
    @TableField("invoice_required")
    private Boolean invoiceRequired;
    @TableField("invoice_info")
    private String invoiceInfo;
    @TableField("warehouse_id")
    private String warehouseId;
    @TableField("sku_id")
    private String skuId;
    @TableField("quantity")
    private Quantity quantity;
    @TableField("waybill_no")
    private String waybillNo;
    @TableField("carrier")
    private String carrier;
    @TableField("shipped_at")
    private LocalDateTime shippedAt;
    @TableField("estimated_arrival")
    private LocalDateTime estimatedArrival;
    @TableField("payment_deadline")
    private LocalDateTime paymentDeadline;
    @TableField("auto_cancel_at")
    private LocalDateTime autoCancelAt;
    @TableField("auto_complete_at")
    private LocalDateTime autoCompleteAt;
    @TableField("completed_at")
    private LocalDateTime completedAt;
    @TableField("cancelled_at")
    private LocalDateTime cancelledAt;
    @TableField("cancel_reason")
    private String cancelReason;
    @TableField("reservation_id")
    private String reservationId;
    @TableField("buyer_message")
    private String buyerMessage;
    @TableField("seller_message")
    private String sellerMessage;
    @TableField("tags")
    private String tags;
    @TableField("extra_data")
    private String extraData;
    @TableField("create_time")
    private LocalDateTime createTime;
    @TableField("create_by")
    private String createBy;
    @TableField("update_time")
    private LocalDateTime updateTime;
    @TableField("update_by")
    private String updateBy;
    @TableField("deleted")
    private Boolean deleted;
    @TableField("remark")
    private String remark;

    // ─── Domain Events (transient, not persisted) ─────────────────

    @TableField(exist = false)
    private List<OrdOrderItem> items;

    // ─── Domain Behavior ─────────────────────────────────────────

    /**
     * Returns the current status as an OrderStatus enum.
     */
    public OrderStatus getStatusEnum() {
        return OrderStatus.fromCode(this.status);
    }

    /**
     * Sets the status from an OrderStatus enum.
     */
    public void setStatusEnum(OrderStatus orderStatus) {
        this.status = orderStatus.getCode();
    }

    /**
     * Checks if a transition to the target status is valid.
     */
    public boolean canTransitionTo(OrderStatus target) {
        return getStatusEnum().canTransitionTo(target);
    }

    /**
     * Transition to a new status with validation. Throws if the transition is invalid.
     */
    public OrdOrder transitionTo(OrderStatus target) {
        OrderStatus current = getStatusEnum();
        if (!current.canTransitionTo(target)) {
            throw new IllegalStateException(
                    "Invalid order status transition: " + current + " -> " + target
                            + " for order " + orderNo);
        }
        this.status = target.getCode();
        return this;
    }

    /**
     * Cancel the order. Only PENDING_PAYMENT or PAID orders can be cancelled.
     */
    public OrdOrder cancel(String reason) {
        OrderStatus current = getStatusEnum();
        if (!current.isCancellable()) {
            throw new IllegalStateException(
                    "Cannot cancel order " + orderNo + " in status " + current);
        }
        transitionTo(OrderStatus.CANCELLED);
        this.cancelledAt = LocalDateTime.now();
        this.cancelReason = reason;
        return this;
    }

    /**
     * Mark the order as paid. Only PENDING_PAYMENT orders can be paid.
     */
    public OrdOrder pay(Money amount, String paymentNo) {
        transitionTo(OrderStatus.PAID);
        this.paidAmount = amount;
        this.paidAt = LocalDateTime.now();
        this.paymentNo = paymentNo;
        return this;
    }

    /**
     * Confirm payment (alias for pay, used by payment callback).
     */
    public OrdOrder confirmPayment(Money amount, String paymentNo) {
        return pay(amount, paymentNo);
    }

    /**
     * Ship the order. Transitions from PENDING_SHIP to SHIPPED.
     */
    public OrdOrder ship(String waybillNo, String carrier) {
        transitionTo(OrderStatus.SHIPPED);
        this.waybillNo = waybillNo;
        this.carrier = carrier;
        this.shippedAt = LocalDateTime.now();
        return this;
    }

    /**
     * Mark the order as in transit. Transitions from SHIPPED to IN_TRANSIT.
     */
    public OrdOrder inTransit() {
        transitionTo(OrderStatus.IN_TRANSIT);
        return this;
    }

    /**
     * Mark the order as delivered. Transitions from IN_TRANSIT to DELIVERED.
     */
    public OrdOrder deliver() {
        transitionTo(OrderStatus.DELIVERED);
        return this;
    }

    /**
     * Complete the order. Transitions from DELIVERED to COMPLETED.
     */
    public OrdOrder complete() {
        transitionTo(OrderStatus.COMPLETED);
        this.completedAt = LocalDateTime.now();
        return this;
    }

    /**
     * Start a refund. Transitions from PAID..DELIVERED to REFUNDING.
     */
    public OrdOrder startRefund() {
        OrderStatus current = getStatusEnum();
        if (!current.isPaid() || current == OrderStatus.COMPLETED) {
            throw new IllegalStateException(
                    "Cannot refund order " + orderNo + " in status " + current);
        }
        transitionTo(OrderStatus.REFUNDING);
        return this;
    }

    /**
     * Confirm refund. Transitions from REFUNDING to REFUNDED.
     */
    public OrdOrder confirmRefund() {
        transitionTo(OrderStatus.REFUNDED);
        return this;
    }

    /**
     * Returns true if the order is in a terminal state.
     */
    public boolean isTerminal() {
        return getStatusEnum().isTerminal();
    }

    /**
     * Returns true if the order is pending payment.
     */
    public boolean isPendingPayment() {
        return getStatusEnum() == OrderStatus.PENDING_PAYMENT;
    }

    /**
     * Returns true if the order has been paid.
     */
    public boolean isPaid() {
        return getStatusEnum().isPaid();
    }
}

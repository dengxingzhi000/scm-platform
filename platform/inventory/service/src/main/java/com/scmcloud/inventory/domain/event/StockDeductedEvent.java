package com.scmcloud.inventory.domain.event;

import com.scmcloud.common.domain.event.DomainEvent;
import lombok.Getter;

import java.util.UUID;

/**
 * Published when stock is deducted (reserved) for an order.
 * Used for order confirmation, audit logging, and inventory monitoring.
 */
@Getter
public class StockDeductedEvent extends DomainEvent {

    private final String skuId;
    private final String warehouseId;
    private final int quantity;
    private final String orderNo;
    private final String reservationId;

    public StockDeductedEvent(UUID tenantId, String skuId, String warehouseId,
                              int quantity, String orderNo, String reservationId) {
        super(tenantId);
        this.skuId = skuId;
        this.warehouseId = warehouseId;
        this.quantity = quantity;
        this.orderNo = orderNo;
        this.reservationId = reservationId;
    }

    @Override
    public String getEventType() {
        return "stock.deducted";
    }
}

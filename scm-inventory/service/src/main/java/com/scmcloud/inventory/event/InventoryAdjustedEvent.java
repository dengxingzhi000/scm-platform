package com.scmcloud.inventory.event;

import lombok.Getter;

import java.util.UUID;

/**
 * Inventory adjustment event published via the transactional outbox.
 *
 * <p>Carried as the {@code payload} of an outbox row written in the same DB
 * transaction as the inventory mutation. The outbox row is relayed to Kafka
 * topic {@code scm.inventory} by the {@code OutboxPoller} running in the
 * inventory service.</p>
 */
@Getter
public class InventoryAdjustedEvent {
    private final UUID tenantId;
    private final String skuId;
    private final String warehouseId;
    private final int quantity;
    private final int availableStock;

    public InventoryAdjustedEvent(UUID tenantId, String skuId, String warehouseId,
                                  int quantity, int availableStock) {
        this.tenantId = tenantId;
        this.skuId = skuId;
        this.warehouseId = warehouseId;
        this.quantity = quantity;
        this.availableStock = availableStock;
    }
}
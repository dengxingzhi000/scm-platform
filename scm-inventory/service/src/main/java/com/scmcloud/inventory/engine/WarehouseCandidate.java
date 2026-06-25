package com.scmcloud.inventory.engine;

import lombok.Data;

@Data
public class WarehouseCandidate {
    private String warehouseId;
    private String warehouseName;
    private String region;
    private int availableStock;
    private int maxStock;
    private boolean hasColdChain;
}

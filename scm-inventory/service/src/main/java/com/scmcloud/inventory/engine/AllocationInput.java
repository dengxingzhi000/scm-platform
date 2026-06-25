package com.scmcloud.inventory.engine;

import lombok.Data;
import java.util.List;

@Data
public class AllocationInput {
    private String orderId;
    private List<OrderItem> items;
    private String destinationRegion;
    private int maxSplits;
    private boolean requiresColdChain;

    @Data
    public static class OrderItem {
        private String skuId;
        private int quantity;
    }
}

package com.scmcloud.warehouse.engine;

import lombok.Data;
import java.util.List;

@Data
public class WaveInput {
    private String warehouseId;
    private List<PendingOrder> orders;

    @Data
    public static class PendingOrder {
        private String orderId;
        private String carrierId;
        private String region;
        private int priority;
        private List<String> skuIds;
    }
}

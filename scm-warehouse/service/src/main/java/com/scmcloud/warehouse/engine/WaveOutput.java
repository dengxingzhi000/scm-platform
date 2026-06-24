package com.scmcloud.warehouse.engine;

import lombok.Data;
import java.util.List;

@Data
public class WaveOutput {
    private List<Wave> waves;

    @Data
    public static class Wave {
        private String waveId;
        private List<String> orderIds;
        private int orderCount;
        private int totalSkuCount;
        private List<PickStep> pickSequence;
    }

    @Data
    public static class PickStep {
        private String locationCode;
        private String skuId;
        private int quantity;
    }
}

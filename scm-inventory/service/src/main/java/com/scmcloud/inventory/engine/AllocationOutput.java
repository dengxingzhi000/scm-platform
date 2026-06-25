package com.scmcloud.inventory.engine;

import com.scmcloud.decision.engine.ConstraintResult;
import lombok.Data;
import java.util.List;

@Data
public class AllocationOutput {
    private List<Allocation> allocations;
    private int splitCount;
    private List<ConstraintResult> constraintResults;

    @Data
    public static class Allocation {
        private String skuId;
        private String warehouseId;
        private int quantity;
        private double score;
    }
}

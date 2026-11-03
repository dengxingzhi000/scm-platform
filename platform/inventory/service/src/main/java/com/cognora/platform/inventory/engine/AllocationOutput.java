package com.cognora.platform.inventory.engine;

import com.cognora.platform.decision.engine.ConstraintResult;
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

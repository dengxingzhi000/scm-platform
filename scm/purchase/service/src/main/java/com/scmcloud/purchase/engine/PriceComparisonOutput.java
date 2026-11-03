package com.scmcloud.purchase.engine;

import com.scmcloud.decision.engine.ConstraintResult;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class PriceComparisonOutput {
    private List<SupplierRanking> rankings;

    @Data
    public static class SupplierRanking {
        private String supplierId;
        private String supplierName;
        private double totalScore;
        private int rank;
        private Map<String, Double> scores;
        private List<ConstraintResult> constraintResults;
    }
}

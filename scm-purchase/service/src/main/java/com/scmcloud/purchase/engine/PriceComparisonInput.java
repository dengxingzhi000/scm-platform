package com.scmcloud.purchase.engine;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class PriceComparisonInput {
    private String comparisonId;
    private List<SupplierQuote> quotes;

    @Data
    public static class SupplierQuote {
        private String supplierId;
        private String supplierName;
        private String quotationId;
        private BigDecimal unitPrice;
        private String supplierLevel;
        private Double qualityScore;
        private Double deliveryScore;
        private Double serviceScore;
        private String supplierStatus;
    }
}

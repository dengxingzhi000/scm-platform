package com.scmcloud.supplier.dto;

import com.scmcloud.supplier.domain.entity.SupSupplierEvaluation;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 供应商绩效评分卡。
 */
@Data
public class SupplierPerformanceVO {

    private String supplierId;
    private String supplierName;
    private Integer supplierLevel;
    private BigDecimal overallScore;
    private BigDecimal qualityScore;
    private BigDecimal deliveryScore;
    private BigDecimal serviceScore;
    private BigDecimal priceScore;
    private long evaluationCount;
    private String latestPeriod;
    private BigDecimal latestTotalScore;
    private LocalDateTime latestEvaluatedAt;
    private Integer suggestedLevel;
    private List<SupSupplierEvaluation> recentEvaluations;
}

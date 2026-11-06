package com.cognora.lms.logistics.domain.freight;

import java.math.BigDecimal;

/**
 * 运费计算明细。便于对账时逐项解释差异来源。
 */
public record FreightQuote(
        BigDecimal actualWeight,
        BigDecimal volumetricWeight,
        BigDecimal chargeableWeight,
        BigDecimal billedWeight,
        BigDecimal firstWeightFee,
        BigDecimal additionalFee,
        BigDecimal regionSurcharge,
        BigDecimal totalAmount
) {
}

package com.cognora.lms.logistics.domain.freight;

import java.math.BigDecimal;

/**
 * 运费计算明细。便于对账时逐项解释差异来源。
 *
 * <p>{@code minCharge} 为模板最低收费，{@code minChargeApplied} 为 true 表示
 * 首重/续重/区域附加费合计低于最低收费，{@code totalAmount} 已按最低收费兜底。</p>
 */
public record FreightQuote(
        BigDecimal actualWeight,
        BigDecimal volumetricWeight,
        BigDecimal chargeableWeight,
        BigDecimal billedWeight,
        BigDecimal firstWeightFee,
        BigDecimal additionalFee,
        BigDecimal regionSurcharge,
        BigDecimal minCharge,
        boolean minChargeApplied,
        BigDecimal totalAmount
) {
}

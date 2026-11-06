package com.cognora.lms.logistics.domain.freight;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 运费计费规则（批次 2）。
 *
 * <p>金额统一使用 {@link BigDecimal}，计算最终以 HALF_UP 保留 2 位小数；
 * 重量进位使用 CEILING（续重不足一个计费单位按一个单位计）。</p>
 */
@Data
public class FreightRule {

    /** 首重重量（kg），必须大于 0。 */
    private BigDecimal firstWeightKg = BigDecimal.ONE;

    /** 首重费用。 */
    private BigDecimal firstWeightFee = BigDecimal.ZERO;

    /** 续重计费单位（kg），必须大于 0。 */
    private BigDecimal additionalUnitKg = new BigDecimal("0.5");

    /** 每续重单位费用。 */
    private BigDecimal additionalUnitFee = BigDecimal.ZERO;

    /** 体积重除数（cm³/kg），必须大于 0。 */
    private BigDecimal volumeDivisor = new BigDecimal("6000");

    /** 区域附加费（按目的省份配置）。 */
    private BigDecimal regionSurcharge = BigDecimal.ZERO;

    /** 最低收费。 */
    private BigDecimal minCharge = BigDecimal.ZERO;
}

package com.cognora.scm.finance.domain.dto;

import com.cognora.scm.finance.domain.enums.TaxRateSource;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 税率解析结果。
 *
 * <p>{@code source=RULE} 表示命中规则，{@code ruleId} 是命中的规则行；
 * {@code source=DEFAULT} 表示走了租户默认税率或配置默认税率，
 * {@code ruleId} 只有在命中租户默认行时才非空。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaxRateResolution {

    /** 解析出的税率 0..1。 */
    private BigDecimal rate;

    /** 取值来源：RULE 命中规则 / DEFAULT 默认值兜底。 */
    private TaxRateSource source;

    /** 命中的规则 ID；走配置默认值时为 null。 */
    private String ruleId;

    /** 命中规则的生效开始日期；走配置默认值时为 null。 */
    private LocalDate effectiveFrom;
}

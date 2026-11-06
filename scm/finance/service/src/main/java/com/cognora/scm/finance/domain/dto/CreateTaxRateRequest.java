package com.cognora.scm.finance.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CreateTaxRateRequest {

    /** 品类编码；为空表示不限品类。 */
    private String category;

    /** 地区编码；为空表示不限地区。 */
    private String region;

    /** 税率 0..1，例如 0.1300。 */
    @NotNull(message = "税率不能为空")
    private BigDecimal rate;

    /** 生效开始日期，必填。 */
    @NotNull(message = "生效开始日期不能为空")
    private LocalDate effectiveFrom;

    /** 生效结束日期，为空表示长期有效。 */
    private LocalDate effectiveTo;

    /** 是否为租户默认税率。 */
    private Boolean isDefault;

    private String remark;
}

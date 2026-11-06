package com.cognora.scm.finance.domain.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 税率规则局部更新：字段为 null 表示不修改。
 */
@Data
public class UpdateTaxRateRequest {

    private String category;

    private String region;

    private BigDecimal rate;

    private LocalDate effectiveFrom;

    private LocalDate effectiveTo;

    private Boolean isDefault;

    private Boolean enabled;

    private String remark;
}

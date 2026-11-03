package com.scmcloud.finance.api.dto;

import java.math.BigDecimal;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class FreightResult {

    private BigDecimal freightAmount;
    private BigDecimal insuranceAmount;
    private BigDecimal totalAmount;
    private String carrier;
    private Integer estimatedDays;
}

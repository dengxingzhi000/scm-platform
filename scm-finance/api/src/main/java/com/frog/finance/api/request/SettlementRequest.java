package com.frog.finance.api.request;

import java.math.BigDecimal;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class SettlementRequest {

    private Long orderId;
    private Long supplierId;
    private BigDecimal amount;
    private String settlementType;
    private Long applicantId;
    private String remark;
}

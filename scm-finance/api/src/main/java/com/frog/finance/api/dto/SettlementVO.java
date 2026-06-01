package com.frog.finance.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class SettlementVO {

    private Long id;
    private String settlementNo;
    private Long orderId;
    private Long supplierId;
    private BigDecimal amount;
    private String status;
    private LocalDateTime createdAt;
}

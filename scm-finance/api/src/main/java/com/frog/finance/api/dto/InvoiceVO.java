package com.frog.finance.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class InvoiceVO {

    private Long id;
    private String invoiceNo;
    private Long orderId;
    private Long supplierId;
    private BigDecimal amount;
    private BigDecimal taxAmount;
    private String invoiceType;
    private String status;
    private LocalDateTime issuedAt;
}

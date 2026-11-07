package com.cognora.scm.finance.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 结账预览中的一条纳入单据。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CloseIncludedDocument {

    /** 纳入类别：SETTLEMENT_ITEM / INVOICE / SHIPPED_UNBILLED。 */
    private String category;

    /** 单据类型（结算明细的 document_type 或发票类型）。 */
    private String documentType;

    /** 单据编号。 */
    private String documentNo;

    /** 归属本期的业务日期。 */
    private LocalDate businessDate;

    /** 单据金额（结算明细取实收金额，发票取价税合计）。 */
    private BigDecimal amount;
}

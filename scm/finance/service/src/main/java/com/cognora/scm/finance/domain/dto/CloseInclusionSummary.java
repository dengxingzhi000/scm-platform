package com.cognora.scm.finance.domain.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 结账「本期纳入单据」预览结果。
 *
 * <p>各类别金额分开列示，不跨类别求和：发票与结算明细可能描述同一笔业务，
 * 合并求和会重复计量。</p>
 */
@Data
public class CloseInclusionSummary {

    private String periodId;

    private String periodCode;

    private LocalDate periodStart;

    private LocalDate periodEnd;

    /** 实际生效的日期口径：BUSINESS_DATE / CREATE_TIME。 */
    private String dateBasis;

    /** 是否纳入已发货未开票明细。 */
    private boolean includeShippedUnbilled;

    /** 是否排除已驳回/作废发票。 */
    private boolean excludeRejected;

    private int settlementItemCount;

    private BigDecimal settlementItemAmount;

    private int invoiceCount;

    private BigDecimal invoiceAmount;

    private int shippedUnbilledCount;

    private BigDecimal shippedUnbilledAmount;

    private List<CloseIncludedDocument> documents;
}

package com.cognora.scm.finance.domain.enums;

/**
 * 结账归属期的日期口径。
 */
public enum CloseDateBasis {

    /** 按业务日期归属（默认）：结算明细取 document_date，发票取 invoice_date。 */
    BUSINESS_DATE,

    /** 按创建时间归属：使用 create_time（历史系统迁移时可能用到）。 */
    CREATE_TIME
}

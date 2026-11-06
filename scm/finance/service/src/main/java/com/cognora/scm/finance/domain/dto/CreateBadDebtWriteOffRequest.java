package com.cognora.scm.finance.domain.dto;

import java.math.BigDecimal;

import lombok.Data;

/**
 * 坏账核销申请。
 *
 * <p>{@code settlementId} 与 {@code invoiceId} 至少要有一个，核销金额与
 * {@code periodCode}（YYYY-MM）绑定。</p>
 */
@Data
public class CreateBadDebtWriteOffRequest {

    private String settlementId;

    private String invoiceId;

    private String partnerId;

    private String partnerName;

    private BigDecimal amount;

    private String periodCode;

    private String reason;

    private String applicantId;

    private String applicantName;
}

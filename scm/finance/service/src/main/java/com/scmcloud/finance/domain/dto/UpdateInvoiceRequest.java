package com.scmcloud.finance.domain.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdateInvoiceRequest {

    private String invoiceNo;

    private Integer invoiceType;

    private Integer invoiceKind;

    private String partyType;

    private String partyId;

    private String partyName;

    private String partyTaxNo;

    private String invoiceCode;

    private String invoiceNumber;

    private BigDecimal amount;

    private BigDecimal taxAmount;

    private BigDecimal taxRate;

    private String relatedOrders;

    private String settlementId;

    private String remark;
}

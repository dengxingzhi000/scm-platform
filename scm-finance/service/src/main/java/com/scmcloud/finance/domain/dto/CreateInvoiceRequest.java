package com.scmcloud.finance.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateInvoiceRequest {

    @NotBlank(message = "发票号码不能为空")
    private String invoiceNo;

    @NotNull(message = "发票类型不能为空")
    private Integer invoiceType;

    @NotNull(message = "发票种类不能为空")
    private Integer invoiceKind;

    @NotBlank(message = "往来方ID不能为空")
    private String partyId;

    private String partyType;

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

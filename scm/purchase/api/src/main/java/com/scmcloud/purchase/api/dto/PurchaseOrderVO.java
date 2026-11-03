package com.scmcloud.purchase.api.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class PurchaseOrderVO {

    private Long id;
    private String purchaseNo;
    private Long supplierId;
    private Long warehouseId;
    private Long applicantId;
    private BigDecimal totalAmount;
    private String status;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

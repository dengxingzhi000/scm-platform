package com.cognora.scm.purchase.domain.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 采购申请转采购单的入参。明细未给单价时沿用申请单的预算价。
 */
public record PurRequestConvertDTO(
        String supplierId,
        String supplierName,
        String supplierContact,
        String warehouseId,
        String warehouseName,
        String warehouseAddress,
        Integer orderType,
        LocalDateTime expectedDelivery,
        BigDecimal freightAmount,
        BigDecimal discountAmount,
        BigDecimal taxRate,
        String remark,
        List<ItemPrice> itemPrices) {

    public record ItemPrice(String requestItemId, BigDecimal unitPrice) {
    }
}

package com.frog.purchase.api.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

@Data
@Accessors(chain = true)
public class PurchaseItemDTO {

    private Long skuId;
    private Integer quantity;
    private BigDecimal unitPrice;
}

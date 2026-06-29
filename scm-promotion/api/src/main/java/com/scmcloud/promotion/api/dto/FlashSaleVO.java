package com.scmcloud.promotion.api.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class FlashSaleVO {

    private Long id;
    private Long productId;
    private Long skuId;
    private BigDecimal originalPrice;
    private BigDecimal flashPrice;
    private Integer flashStock;
    private Integer soldCount;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}

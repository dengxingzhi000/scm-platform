package com.scmcloud.promotion.api.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CouponVO {

    private Long id;
    private Long templateId;
    private String userId;
    private String couponCode;
    private Integer status;
    private BigDecimal discountValue;
    private BigDecimal minAmount;
    private LocalDateTime usedAt;
    private String orderNo;
    private LocalDateTime createdAt;
}

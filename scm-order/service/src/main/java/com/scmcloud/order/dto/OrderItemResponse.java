package com.scmcloud.order.dto;

import lombok.Data;
import java.util.UUID;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * 订单明细响应。
 *
 * @author SCM Platform Team
 */
@Data
public class OrderItemResponse {

    private UUID id;
    private String skuId;
    private String spuId;
    private String skuCode;
    private String skuName;
    private String spuName;
    private String attributes;
    private String imageUrl;

    private BigDecimal originalPrice;
    private BigDecimal sellingPrice;
    private Integer quantity;
    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
    private String warehouseId;

    private Integer refundQuantity;
    private BigDecimal refundAmount;
    private String remark;
}

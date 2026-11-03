package com.scmcloud.order.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * 退款明细响应。
 *
 * @author SCM Platform Team
 */
@Data
public class RefundItemResponse {
    private UUID id;
    private UUID refundId;
    private String refundNo;
    private UUID orderId;
    private String orderNo;
    private UUID orderItemId;
    private UUID skuId;
    private String skuCode;
    private String skuName;
    private Integer quantity;
    private BigDecimal refundAmount;
    private String remark;
}

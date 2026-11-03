package com.scmcloud.order.dto;

import com.scmcloud.order.domain.entity.OrderStatus;
import java.util.UUID;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 订单响应。
 *
 * <p>仅暴露对前端可见的字段；内部审计字段（createBy/updateBy/deleted）不返回。</p>
 *
 * @author SCM Platform Team
 */
@Data
public class OrderResponse {

    private UUID id;
    private String orderNo;
    private String userId;
    private String username;
    private Integer orderType;
    private String orderSource;
    private OrderStatus status;
    private String statusName;

    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private BigDecimal freightAmount;
    private BigDecimal payableAmount;
    private BigDecimal paidAmount;

    private Integer paymentMethod;
    private String paymentNo;
    private LocalDateTime paidAt;

    private String shippingAddress;
    private String warehouseId;
    private String skuId;
    private Integer quantity;
    private String waybillNo;
    private String carrier;
    private LocalDateTime shippedAt;
    private LocalDateTime estimatedArrival;

    private LocalDateTime paymentDeadline;
    private LocalDateTime autoCancelAt;
    private LocalDateTime autoCompleteAt;
    private LocalDateTime completedAt;
    private LocalDateTime cancelledAt;
    private String cancelReason;
    private String reservationId;
    private String buyerMessage;
    private String sellerMessage;
    private String tags;
    private String remark;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /** 订单明细（可选） */
    private List<OrderItemResponse> items;
}

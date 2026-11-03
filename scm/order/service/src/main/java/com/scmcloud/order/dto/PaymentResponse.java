package com.scmcloud.order.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 支付记录响应。
 *
 * @author SCM Platform Team
 */
@Data
public class PaymentResponse {
    private UUID id;
    private String paymentNo;
    private UUID orderId;
    private String orderNo;
    private String userId;
    private Integer paymentMethod;
    private String paymentChannel;
    private BigDecimal paymentAmount;
    private String paymentStatus;
    private String thirdPartyNo;
    private LocalDateTime initiatedAt;
    private LocalDateTime paidAt;
    private LocalDateTime failedAt;
    private LocalDateTime refundedAt;
    private BigDecimal refundAmount;
    private String refundReason;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private String remark;
}

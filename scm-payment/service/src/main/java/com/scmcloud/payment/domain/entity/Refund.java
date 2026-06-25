package com.scmcloud.payment.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("pay_refund")
public class Refund {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("refund_no")
    private String refundNo;

    @TableField("payment_no")
    private String paymentNo;

    @TableField("order_no")
    private String orderNo;

    @TableField("user_id")
    private String userId;

    @TableField("refund_amount")
    private BigDecimal refundAmount;

    @TableField("refund_reason")
    private String refundReason;

    @TableField("status")
    private Integer status;

    @TableField("refunded_at")
    private LocalDateTime refundedAt;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}

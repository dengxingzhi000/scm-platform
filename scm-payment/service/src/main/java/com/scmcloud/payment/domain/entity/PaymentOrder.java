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
@TableName("pay_payment_order")
public class PaymentOrder {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("payment_no")
    private String paymentNo;

    @TableField("order_no")
    private String orderNo;

    @TableField("user_id")
    private String userId;

    @TableField("amount")
    private BigDecimal amount;

    @TableField("currency")
    private String currency;

    @TableField("payment_method")
    private String paymentMethod;

    @TableField("payment_channel")
    private String paymentChannel;

    @TableField("status")
    private Integer status;

    @TableField("paid_at")
    private LocalDateTime paidAt;

    @TableField("callback_url")
    private String callbackUrl;

    @TableField("notify_url")
    private String notifyUrl;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}

package com.scmcloud.ordercenter.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("oc_order")
public class OcOrder {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("order_no")
    private String orderNo;

    @TableField("parent_order_no")
    private String parentOrderNo;

    @TableField("channel")
    private String channel;

    @TableField("order_type")
    private String orderType;

    @TableField("user_id")
    private String userId;

    @TableField("seller_id")
    private Long sellerId;

    @TableField("status")
    private Integer status;

    @TableField("total_amount")
    private BigDecimal totalAmount;

    @TableField("discount_amount")
    private BigDecimal discountAmount;

    @TableField("payable_amount")
    private BigDecimal payableAmount;

    @TableField("paid_amount")
    private BigDecimal paidAmount;

    @TableField("shipping_fee")
    private BigDecimal shippingFee;

    @TableField("receiver_name")
    private String receiverName;

    @TableField("receiver_phone")
    private String receiverPhone;

    @TableField("receiver_address")
    private String receiverAddress;

    @TableField("remark")
    private String remark;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}

package com.scmcloud.fulfillment.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("ful_fulfillment_order")
public class FulfillmentOrder {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("fulfillment_no")
    private String fulfillmentNo;

    @TableField("order_no")
    private String orderNo;

    @TableField("user_id")
    private String userId;

    @TableField("status")
    private Integer status;

    @TableField("fulfillment_type")
    private String fulfillmentType;

    @TableField("warehouse_id")
    private Long warehouseId;

    @TableField("shipping_method")
    private String shippingMethod;

    @TableField("receiver_name")
    private String receiverName;

    @TableField("receiver_phone")
    private String receiverPhone;

    @TableField("receiver_address")
    private String receiverAddress;

    @TableField("total_items")
    private Integer totalItems;

    @TableField("total_weight")
    private BigDecimal totalWeight;

    @TableField("shipping_fee")
    private BigDecimal shippingFee;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}

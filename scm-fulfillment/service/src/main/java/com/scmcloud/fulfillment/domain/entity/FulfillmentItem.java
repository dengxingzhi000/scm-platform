package com.scmcloud.fulfillment.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("ful_fulfillment_item")
public class FulfillmentItem {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("fulfillment_no")
    private String fulfillmentNo;

    @TableField("order_item_id")
    private Long orderItemId;

    @TableField("sku_id")
    private Long skuId;

    @TableField("product_name")
    private String productName;

    @TableField("quantity")
    private Integer quantity;

    @TableField("picked_quantity")
    private Integer pickedQuantity;

    @TableField("packed_quantity")
    private Integer packedQuantity;

    @TableField("status")
    private Integer status;

    @TableField("created_at")
    private LocalDateTime createdAt;
}

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
@TableName("oc_order_item")
public class OcOrderItem {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("order_no")
    private String orderNo;

    @TableField("product_id")
    private Long productId;

    @TableField("sku_id")
    private Long skuId;

    @TableField("product_name")
    private String productName;

    @TableField("sku_name")
    private String skuName;

    @TableField("price")
    private BigDecimal price;

    @TableField("quantity")
    private Integer quantity;

    @TableField("discount_amount")
    private BigDecimal discountAmount;

    @TableField("payable_amount")
    private BigDecimal payableAmount;

    @TableField("seller_id")
    private Long sellerId;

    @TableField("status")
    private Integer status;

    @TableField("created_at")
    private LocalDateTime createdAt;
}

package com.scmcloud.mall.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("mall_cart")
public class Cart {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("user_id")
    private String userId;

    @TableField("sku_id")
    private Long skuId;

    @TableField("product_id")
    private Long productId;

    @TableField("quantity")
    private Integer quantity;

    @TableField("selected")
    private Boolean selected;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}

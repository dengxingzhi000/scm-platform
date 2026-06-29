package com.scmcloud.promotion.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("pro_group_buy")
public class GroupBuy {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("activity_id")
    private Long activityId;

    @TableField("product_id")
    private Long productId;

    @TableField("sku_id")
    private Long skuId;

    @TableField("group_price")
    private BigDecimal groupPrice;

    @TableField("group_size")
    private Integer groupSize;

    @TableField("current_size")
    private Integer currentSize;

    @TableField("expire_time")
    private LocalDateTime expireTime;

    @TableField("status")
    private Integer status;

    @TableField("created_at")
    private LocalDateTime createdAt;
}

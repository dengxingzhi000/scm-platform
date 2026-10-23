package com.scmcloud.promotion.domain.entity;

import com.scmcloud.common.domain.TenantId;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("pro_coupon")
public class Coupon {

    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    private TenantId tenantId;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("template_id")
    private Long templateId;

    @TableField("user_id")
    private String userId;

    @TableField("coupon_code")
    private String couponCode;

    @TableField("status")
    private Integer status;

    @TableField("used_at")
    private LocalDateTime usedAt;

    @TableField("order_no")
    private String orderNo;

    @TableField("created_at")
    private LocalDateTime createdAt;
}

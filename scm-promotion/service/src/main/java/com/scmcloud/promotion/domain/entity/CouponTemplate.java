package com.scmcloud.promotion.domain.entity;

import com.scmcloud.common.domain.TenantId;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("pro_coupon_template")
public class CouponTemplate {

    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    private TenantId tenantId;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("template_name")
    private String templateName;

    @TableField("coupon_type")
    private String couponType;

    @TableField("discount_value")
    private BigDecimal discountValue;

    @TableField("min_amount")
    private BigDecimal minAmount;

    @TableField("max_discount")
    private BigDecimal maxDiscount;

    @TableField("total_count")
    private Integer totalCount;

    @TableField("issued_count")
    private Integer issuedCount;

    @TableField("start_time")
    private LocalDateTime startTime;

    @TableField("end_time")
    private LocalDateTime endTime;

    @TableField("status")
    private Integer status;

    @TableField("created_at")
    private LocalDateTime createdAt;
}

package com.scmcloud.tenant.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * <p>
 * 租户平台服务费账单
 * </p>
 *
 * @author SCM Platform Team
 * @since 2026-10-29
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tenant_platform_fee")
public class TenantPlatformFee implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.NONE)
    private String id;

    @TableField("tenant_id")
    private String tenantId;

    @TableField("billing_period")
    private String billingPeriod;

    @TableField("start_date")
    private LocalDate startDate;

    @TableField("end_date")
    private LocalDate endDate;

    @TableField("package_id")
    private String packageId;

    @TableField("package_name")
    private String packageName;

    @TableField("base_fee")
    private BigDecimal baseFee;

    @TableField("storage_fee")
    private BigDecimal storageFee;

    @TableField("api_fee")
    private BigDecimal apiFee;

    @TableField("total_fee")
    private BigDecimal totalFee;

    @TableField("discount_amount")
    private BigDecimal discountAmount;

    @TableField("final_fee")
    private BigDecimal finalFee;

    @TableField("status")
    private Integer status;

    @TableField("paid_at")
    private LocalDateTime paidAt;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    @TableField("remark")
    private String remark;
}

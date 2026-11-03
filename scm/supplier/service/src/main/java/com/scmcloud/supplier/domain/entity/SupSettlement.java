package com.scmcloud.supplier.domain.entity;

import java.math.BigDecimal;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableId;
import java.util.UUID;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 对账单表
 * </p>
 *
 * @author author
 * @since 2025-12-25
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("sup_settlement")
public class SupSettlement {

    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    private String tenantId;

    @TableId(value = "id", type = IdType.NONE)
    private UUID id;

    @TableField("settlement_no")
    private String settlementNo;

    @TableField("supplier_id")
    private String supplierId;

    @TableField("supplier_name")
    private String supplierName;

    @TableField("settlement_period")
    private String settlementPeriod;

    @TableField("start_date")
    private LocalDate startDate;

    @TableField("end_date")
    private LocalDate endDate;

    @TableField("purchase_order_ids")
    private String purchaseOrderIds;

    @TableField("purchase_count")
    private Integer purchaseCount;

    @TableField("total_amount")
    private BigDecimal totalAmount;

    @TableField("discount_amount")
    private BigDecimal discountAmount;

    @TableField("actual_amount")
    private BigDecimal actualAmount;

    @TableField("payment_amount")
    private BigDecimal paymentAmount;

    @TableField("payment_date")
    private LocalDate paymentDate;

    @TableField("status")
    private Integer status;

    @TableField("approver_id")
    private String approverId;

    @TableField("approver_name")
    private String approverName;

    @TableField("approved_at")
    private LocalDateTime approvedAt;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("create_by")
    private String createBy;

    @TableField("update_time")
    private LocalDateTime updateTime;

    @TableField("update_by")
    private String updateBy;

    @TableField("deleted")
    private Boolean deleted;

    @TableField("remark")
    private String remark;


}

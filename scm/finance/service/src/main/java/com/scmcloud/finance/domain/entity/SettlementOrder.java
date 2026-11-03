package com.scmcloud.finance.domain.entity;

import java.io.Serial;
import java.math.BigDecimal;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 结算单表
 * </p>
 *
 * @author deng
 * @since 2025-12-26
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("settlement_order")
public class SettlementOrder implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.NONE)
    private String id;

    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    private String tenantId;

    @TableField("settlement_no")
    private String settlementNo;

    @TableField("settlement_type")
    private Integer settlementType;

    @TableField("partner_type")
    private String partnerType;

    @TableField("partner_id")
    private String partnerId;

    @TableField("partner_name")
    private String partnerName;

    @TableField("settlement_period")
    private String settlementPeriod;

    @TableField("start_date")
    private LocalDate startDate;

    @TableField("end_date")
    private LocalDate endDate;

    @TableField("total_amount")
    private BigDecimal totalAmount;

    @TableField("discount_amount")
    private BigDecimal discountAmount;

    @TableField("adjustment_amount")
    private BigDecimal adjustmentAmount;

    @TableField("actual_amount")
    private BigDecimal actualAmount;

    @TableField("paid_amount")
    private BigDecimal paidAmount;

    @TableField("unpaid_amount")
    private BigDecimal unpaidAmount;

    @TableField("status")
    private Integer status;

    @TableField("approver_id")
    private String approverId;

    @TableField("approver_name")
    private String approverName;

    @TableField("approved_at")
    private LocalDateTime approvedAt;

    @TableField("attachments")
    private String attachments;

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

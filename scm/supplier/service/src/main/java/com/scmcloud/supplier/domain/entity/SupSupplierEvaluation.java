package com.scmcloud.supplier.domain.entity;

import java.math.BigDecimal;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 供应商评价表
 * </p>
 *
 * @author author
 * @since 2025-12-25
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("sup_supplier_evaluation")
public class SupSupplierEvaluation {

    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    private String tenantId;

    @TableId(value = "id", type = IdType.NONE)
    private String id;

    @TableField("supplier_id")
    private String supplierId;

    @TableField("evaluation_period")
    private String evaluationPeriod;

    @TableField("start_date")
    private LocalDate startDate;

    @TableField("end_date")
    private LocalDate endDate;

    @TableField("quality_score")
    private BigDecimal qualityScore;

    @TableField("delivery_score")
    private BigDecimal deliveryScore;

    @TableField("service_score")
    private BigDecimal serviceScore;

    @TableField("price_score")
    private BigDecimal priceScore;

    @TableField("total_score")
    private BigDecimal totalScore;

    @TableField("purchase_count")
    private Integer purchaseCount;

    @TableField("total_amount")
    private BigDecimal totalAmount;

    @TableField("on_time_delivery_rate")
    private BigDecimal onTimeDeliveryRate;

    @TableField("quality_pass_rate")
    private BigDecimal qualityPassRate;

    @TableField("evaluator_id")
    private String evaluatorId;

    @TableField("evaluator_name")
    private String evaluatorName;

    @TableField("evaluated_at")
    private LocalDateTime evaluatedAt;

    @TableField("improvement_suggestions")
    private String improvementSuggestions;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("create_by")
    private String createBy;

    @TableField("update_time")
    private LocalDateTime updateTime;

    @TableField("update_by")
    private String updateBy;

    @TableField("remark")
    private String remark;
}

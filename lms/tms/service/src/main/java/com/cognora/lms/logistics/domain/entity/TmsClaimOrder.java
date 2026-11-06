package com.cognora.lms.logistics.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 理赔单据（批次 3）：金额、证据与审批/赔付状态。
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tms_claim_order")
public class TmsClaimOrder implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    private String tenantId;

    @TableId(value = "id", type = IdType.NONE)
    private String id;

    @TableField("case_id")
    private String caseId;

    @TableField("waybill_no")
    private String waybillNo;

    @TableField("claim_amount")
    private BigDecimal claimAmount;

    @TableField("currency")
    private String currency;

    /** DRAFT / SUBMITTED / APPROVED / REJECTED / PAID。 */
    @TableField("status")
    private String status;

    @TableField("reason")
    private String reason;

    @TableField("evidence_url")
    private String evidenceUrl;

    @TableField("submitted_by")
    private String submittedBy;

    @TableField("submitted_time")
    private LocalDateTime submittedTime;

    @TableField("reviewed_by")
    private String reviewedBy;

    @TableField("reviewed_time")
    private LocalDateTime reviewedTime;

    @TableField("paid_time")
    private LocalDateTime paidTime;

    @TableField("create_by")
    private String createBy;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    @TableField("remark")
    private String remark;
}

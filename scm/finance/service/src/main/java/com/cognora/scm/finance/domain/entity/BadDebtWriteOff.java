package com.cognora.scm.finance.domain.entity;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 坏账核销单据：申请 → 审批 → 核销生效。
 *
 * <p>每一笔核销都绑定一个 {@code period_code}（YYYY-MM），核销生效时该账期必须
 * 可写（未关账）；核销生效后同一结算单/发票不允许再次核销。</p>
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("fin_bad_debt_write_off")
public class BadDebtWriteOff implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.NONE)
    private String id;

    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    private String tenantId;

    @TableField("write_off_no")
    private String writeOffNo;

    @TableField("settlement_id")
    private String settlementId;

    @TableField("invoice_id")
    private String invoiceId;

    @TableField("partner_id")
    private String partnerId;

    @TableField("partner_name")
    private String partnerName;

    @TableField("amount")
    private BigDecimal amount;

    @TableField("period_code")
    private String periodCode;

    @TableField("status")
    private Integer status;

    @TableField("reason")
    private String reason;

    @TableField("applicant_id")
    private String applicantId;

    @TableField("applicant_name")
    private String applicantName;

    @TableField("applied_at")
    private LocalDateTime appliedAt;

    @TableField("approver_id")
    private String approverId;

    @TableField("approver_name")
    private String approverName;

    @TableField("approved_at")
    private LocalDateTime approvedAt;

    @TableField("approve_remark")
    private String approveRemark;

    @TableField("rejected_by")
    private String rejectedBy;

    @TableField("rejected_at")
    private LocalDateTime rejectedAt;

    @TableField("reject_reason")
    private String rejectReason;

    @TableField("written_off_by")
    private String writtenOffBy;

    @TableField("written_off_at")
    private LocalDateTime writtenOffAt;

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
}

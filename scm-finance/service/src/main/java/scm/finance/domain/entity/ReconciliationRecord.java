package scm.finance.domain.entity;

import java.io.Serial;
import java.math.BigDecimal;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 对账记录表
 * </p>
 *
 * @author deng
 * @since 2025-12-26
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("reconciliation_record")
@Schema(description = "对账记录表")
public class ReconciliationRecord implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.NONE)
    private String id;

    @TableField("tenant_id")
    private String tenantId;

    @TableField("reconciliation_no")
    private String reconciliationNo;

    @TableField("party_type")
    private String partyType;

    @TableField("party_id")
    private String partyId;

    @TableField("party_name")
    private String partyName;

    @TableField("reconciliation_period")
    private String reconciliationPeriod;

    @TableField("start_date")
    private LocalDate startDate;

    @TableField("end_date")
    private LocalDate endDate;

    @TableField("our_total_amount")
    private BigDecimal ourTotalAmount;

    @TableField("our_paid_amount")
    private BigDecimal ourPaidAmount;

    @TableField("our_unpaid_amount")
    private BigDecimal ourUnpaidAmount;

    @TableField("their_total_amount")
    private BigDecimal theirTotalAmount;

    @TableField("their_paid_amount")
    private BigDecimal theirPaidAmount;

    @TableField("their_unpaid_amount")
    private BigDecimal theirUnpaidAmount;

    @TableField("diff_amount")
    private BigDecimal diffAmount;

    @TableField("has_diff")
    private Boolean hasDiff;

    @TableField("diff_reason")
    private String diffReason;

    @Schema(description = "状态:0-待对账,1-已对账,2-已确认,3-有差异")
    @TableField("status")
    private Integer status;

    @TableField("reconciler_id")
    private String reconcilerId;

    @TableField("reconciler_name")
    private String reconcilerName;

    @TableField("reconciled_at")
    private LocalDateTime reconciledAt;

    @TableField("confirmer_id")
    private String confirmerId;

    @TableField("confirmer_name")
    private String confirmerName;

    @TableField("confirmed_at")
    private LocalDateTime confirmedAt;

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

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
@Schema(description = "结算单表")
public class SettlementOrder implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.NONE)
    private String id;

    @TableField("tenant_id")
    private String tenantId;

    @TableField("settlement_no")
    private String settlementNo;

    @Schema(description = "结算类型:1-采购结算,2-销售结算,3-物流结算")
    @TableField("settlement_type")
    private Integer settlementType;

    @Schema(description = "结算对象:SUPPLIER-供应商,CUSTOMER-客户,CARRIER-物流商")
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

    @Schema(description = "状态:0-待确认,1-已确认,2-待付款,3-部分付款,4-已付款")
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

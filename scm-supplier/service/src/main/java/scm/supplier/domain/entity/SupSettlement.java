package scm.supplier.domain.entity;

import java.math.BigDecimal;
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
@Schema(description = "对账单表")
public class SupSettlement implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.UUID)
    private String id;

    @TableField("id")
    private String id;

    @TableField("settlement_no")
    private String settlementNo;

    @TableField("settlement_no")
    private String settlementNo;

    @TableField("supplier_id")
    private String supplierId;

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

    @TableField("total_amount")
    private BigDecimal totalAmount;

    @TableField("discount_amount")
    private BigDecimal discountAmount;

    @TableField("actual_amount")
    private BigDecimal actualAmount;

    @TableField("actual_amount")
    private BigDecimal actualAmount;

    @TableField("payment_amount")
    private BigDecimal paymentAmount;

    @TableField("payment_date")
    private LocalDate paymentDate;

    @Schema(description = "状态:0-待确认,1-已确认,2-待付款,3-部分付款,4-已付款")
    @TableField("status")
    private Integer status;

    @Schema(description = "状态:0-待确认,1-已确认,2-待付款,3-部分付款,4-已付款")
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

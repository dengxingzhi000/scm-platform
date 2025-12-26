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
 * 平台服务费表（SaaS平台）
 * </p>
 *
 * @author deng
 * @since 2025-12-26
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("platform_service_fee")
@Schema(description = "平台服务费表（SaaS平台）")
public class PlatformServiceFee implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.NONE)
    private String id;

    @TableField("tenant_id")
    private String tenantId;

    @Schema(description = "费用类型:1-交易佣金,2-存储费用,3-API调用费,4-增值服务费")
    @TableField("fee_type")
    private Integer feeType;

    @TableField("billing_period")
    private String billingPeriod;

    @TableField("start_date")
    private LocalDate startDate;

    @TableField("end_date")
    private LocalDate endDate;

    @TableField("order_count")
    private Integer orderCount;

    @TableField("transaction_amount")
    private BigDecimal transactionAmount;

    @TableField("storage_used_gb")
    private BigDecimal storageUsedGb;

    @TableField("api_calls")
    private Integer apiCalls;

    @TableField("fee_rate")
    private BigDecimal feeRate;

    @TableField("unit_price")
    private BigDecimal unitPrice;

    @TableField("base_fee")
    private BigDecimal baseFee;

    @TableField("transaction_fee")
    private BigDecimal transactionFee;

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

    @Schema(description = "状态:0-待付款,1-已付款,2-已作废")
    @TableField("status")
    private Integer status;

    @TableField("paid_amount")
    private BigDecimal paidAmount;

    @TableField("paid_at")
    private LocalDateTime paidAt;

    @TableField("invoice_id")
    private String invoiceId;

    @TableField("invoice_no")
    private String invoiceNo;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    @TableField("remark")
    private String remark;


}

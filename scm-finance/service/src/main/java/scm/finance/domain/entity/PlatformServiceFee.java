package scm.finance.domain.entity;

import java.math.BigDecimal;

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
 * 平台服务费表（SaaS平台？
 * </p>
 *
 * @author deng
 * @since 2025-12-26
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("platform_service_fee")
public class PlatformServiceFee {

    @TableId(value = "id", type = IdType.NONE)
    private String id;

    @TableField("tenant_id")
    private String tenantId;

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

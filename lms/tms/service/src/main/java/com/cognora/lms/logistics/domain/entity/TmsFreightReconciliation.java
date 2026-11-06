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
 * 运费对账记录：保存系统应收、承运商账单与差异分类（批次 2）。
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tms_freight_reconciliation")
public class TmsFreightReconciliation implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public static final String STATUS_OPEN = "OPEN";
    public static final String STATUS_CONFIRMED = "CONFIRMED";

    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    private String tenantId;

    @TableId(value = "id", type = IdType.NONE)
    private String id;

    /** 对账批次；同一批次内同一运单只落一条，保证重复对账幂等。 */
    @TableField("batch_no")
    private String batchNo;

    @TableField("waybill_id")
    private String waybillId;

    @TableField("waybill_no")
    private String waybillNo;

    @TableField("carrier_id")
    private String carrierId;

    @TableField("region_code")
    private String regionCode;

    @TableField("actual_weight")
    private BigDecimal actualWeight;

    @TableField("volume")
    private BigDecimal volume;

    @TableField("chargeable_weight")
    private BigDecimal chargeableWeight;

    @TableField("expected_amount")
    private BigDecimal expectedAmount;

    @TableField("billed_amount")
    private BigDecimal billedAmount;

    /** 差异 = 账单 - 应收，正数为多收。 */
    @TableField("diff_amount")
    private BigDecimal diffAmount;

    /** MATCH / OVERCHARGE / UNDERCHARGE。 */
    @TableField("diff_type")
    private String diffType;

    @TableField("status")
    private String status;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("create_by")
    private String createBy;

    @TableField("update_time")
    private LocalDateTime updateTime;

    @TableField("remark")
    private String remark;
}

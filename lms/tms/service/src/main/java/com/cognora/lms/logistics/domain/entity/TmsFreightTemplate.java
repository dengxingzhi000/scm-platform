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
 * 运费模板：按租户 + 承运商 + 区域（省）配置首重/续重/体积重/区域价（批次 2）。
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tms_freight_template")
public class TmsFreightTemplate implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    private String tenantId;

    @TableId(value = "id", type = IdType.NONE)
    private String id;

    @TableField("template_name")
    private String templateName;

    @TableField("carrier_id")
    private String carrierId;

    /** 目的省份/区域编码；DEFAULT 表示兜底模板。 */
    @TableField("region_code")
    private String regionCode;

    @TableField("region_name")
    private String regionName;

    @TableField("first_weight_kg")
    private BigDecimal firstWeightKg;

    @TableField("first_weight_fee")
    private BigDecimal firstWeightFee;

    @TableField("additional_unit_kg")
    private BigDecimal additionalUnitKg;

    @TableField("additional_unit_fee")
    private BigDecimal additionalUnitFee;

    @TableField("volume_divisor")
    private BigDecimal volumeDivisor;

    @TableField("region_surcharge")
    private BigDecimal regionSurcharge;

    @TableField("min_charge")
    private BigDecimal minCharge;

    @TableField("enabled")
    private Boolean enabled;

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

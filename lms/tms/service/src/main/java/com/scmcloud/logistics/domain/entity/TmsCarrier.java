package com.scmcloud.logistics.domain.entity;

import java.io.Serial;
import java.math.BigDecimal;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 物流商表
 * </p>
 *
 * @author deng
 * @since 2025-12-26
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tms_carrier")
public class TmsCarrier implements Serializable {
    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    private String tenantId;

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.NONE)
    private String id;

    @TableField("carrier_code")
    private String carrierCode;

    @TableField("carrier_name")
    private String carrierName;

    @TableField("carrier_type")
    private Integer carrierType;

    @TableField("contact_name")
    private String contactName;

    @TableField("contact_phone")
    private String contactPhone;

    @TableField("contact_email")
    private String contactEmail;

    @TableField("website")
    private String website;

    @TableField("api_url")
    private String apiUrl;

    @TableField("api_key")
    private String apiKey;

    @TableField("api_secret")
    private String apiSecret;

    @TableField("service_area")
    private String serviceArea;

    @TableField("service_types")
    private String serviceTypes;

    @TableField("base_rate")
    private BigDecimal baseRate;

    @TableField("enabled")
    private Boolean enabled;

    @TableField("sort_order")
    private Integer sortOrder;

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

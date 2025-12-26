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
 * 运费规则表
 * </p>
 *
 * @author deng
 * @since 2025-12-26
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("freight_rule")
@Schema(description = "运费规则表")
public class FreightRule implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.NONE)
    private String id;

    @TableField("tenant_id")
    private String tenantId;

    @TableField("rule_code")
    private String ruleCode;

    @TableField("rule_name")
    private String ruleName;

    @TableField("carrier_id")
    private String carrierId;

    @TableField("carrier_name")
    private String carrierName;

    @TableField("region_type")
    private String regionType;

    @TableField("regions")
    private String regions;

    @Schema(description = "计费类型:1-按重量,2-按件数,3-按体积,4-固定运费")
    @TableField("billing_type")
    private Integer billingType;

    @TableField("first_weight")
    private BigDecimal firstWeight;

    @TableField("first_price")
    private BigDecimal firstPrice;

    @TableField("additional_weight")
    private BigDecimal additionalWeight;

    @TableField("additional_price")
    private BigDecimal additionalPrice;

    @Schema(description = "满额免运费阈值")
    @TableField("free_threshold")
    private BigDecimal freeThreshold;

    @TableField("fixed_freight")
    private BigDecimal fixedFreight;

    @TableField("remote_area_fee")
    private BigDecimal remoteAreaFee;

    @TableField("handling_fee")
    private BigDecimal handlingFee;

    @TableField("priority")
    private Integer priority;

    @TableField("effective_date")
    private LocalDate effectiveDate;

    @TableField("expiry_date")
    private LocalDate expiryDate;

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

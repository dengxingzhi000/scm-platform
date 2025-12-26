package scm.tenant.domain.entity;

import java.math.BigDecimal;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import java.io.Serializable;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 租户套餐表
 * </p>
 *
 * @author deng
 * @since 2025-12-25
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tenant_package")
@Schema(description = "租户套餐表")
public class TenantPackage implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.NONE)
    private String id;

    @TableField("package_code")
    private String packageCode;

    @TableField("package_name")
    private String packageName;

    @Schema(description = "套餐级别:1-基础版,2-专业版,3-企业版,4-旗舰版")
    @TableField("package_level")
    private Integer packageLevel;

    @TableField("price_monthly")
    private BigDecimal priceMonthly;

    @TableField("price_yearly")
    private BigDecimal priceYearly;

    @TableField("discount_rate")
    private BigDecimal discountRate;

    @TableField("max_users")
    private Integer maxUsers;

    @TableField("max_warehouses")
    private Integer maxWarehouses;

    @TableField("max_skus")
    private Integer maxSkus;

    @TableField("max_orders_per_day")
    private Integer maxOrdersPerDay;

    @TableField("max_storage_gb")
    private Integer maxStorageGb;

    @TableField("max_api_calls_per_day")
    private Integer maxApiCallsPerDay;

    @Schema(description = "功能模块配置（JSONB）")
    @TableField("features")
    private String features;

    @TableField("description")
    private String description;

    @TableField("highlights")
    private String highlights;

    @TableField("enabled")
    private Boolean enabled;

    @TableField("is_trial")
    private Boolean isTrial;

    @TableField("trial_days")
    private Integer trialDays;

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

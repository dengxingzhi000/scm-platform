package scm.tenant.domain.entity;

import java.math.BigDecimal;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import java.time.LocalDate;
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
 * 租户资源配额表
 * </p>
 *
 * @author deng
 * @since 2025-12-25
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tenant_resource_quota")
@Schema(description = "租户资源配额表")
public class TenantResourceQuota implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.NONE)
    private String id;

    @TableField("tenant_id")
    private String tenantId;

    @TableField("max_users")
    private Integer maxUsers;

    @TableField("current_users")
    private Integer currentUsers;

    @TableField("max_warehouses")
    private Integer maxWarehouses;

    @TableField("current_warehouses")
    private Integer currentWarehouses;

    @TableField("max_skus")
    private Integer maxSkus;

    @TableField("current_skus")
    private Integer currentSkus;

    @TableField("max_orders_per_day")
    private Integer maxOrdersPerDay;

    @TableField("current_orders_today")
    private Integer currentOrdersToday;

    @Schema(description = "订单配额重置日期（每日重置）")
    @TableField("last_order_reset_date")
    private LocalDate lastOrderResetDate;

    @TableField("max_storage_gb")
    private Integer maxStorageGb;

    @TableField("current_storage_gb")
    private BigDecimal currentStorageGb;

    @TableField("max_api_calls_per_day")
    private Integer maxApiCallsPerDay;

    @TableField("current_api_calls_today")
    private Integer currentApiCallsToday;

    @Schema(description = "API配额重置日期（每日重置）")
    @TableField("last_api_reset_date")
    private LocalDate lastApiResetDate;

    @TableField("custom_quotas")
    private String customQuotas;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;


}

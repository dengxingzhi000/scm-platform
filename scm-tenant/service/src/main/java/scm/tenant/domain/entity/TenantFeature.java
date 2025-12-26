package scm.tenant.domain.entity;

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
 * 租户功能开关表
 * </p>
 *
 * @author deng
 * @since 2025-12-25
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tenant_feature")
@Schema(description = "租户功能开关表")
public class TenantFeature implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.NONE)
    private String id;

    @TableField("tenant_id")
    private String tenantId;

    @Schema(description = "功能代码:INVENTORY,WMS,OMS,TMS,APS,FINANCE等")
    @TableField("feature_code")
    private String featureCode;

    @TableField("feature_name")
    private String featureName;

    @TableField("enabled")
    private Boolean enabled;

    @Schema(description = "Beta功能标识")
    @TableField("is_beta")
    private Boolean isBeta;

    @TableField("usage_limit")
    private Integer usageLimit;

    @TableField("current_usage")
    private Integer currentUsage;

    @TableField("expire_at")
    private LocalDateTime expireAt;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("create_by")
    private String createBy;

    @TableField("update_time")
    private LocalDateTime updateTime;

    @TableField("update_by")
    private String updateBy;


}

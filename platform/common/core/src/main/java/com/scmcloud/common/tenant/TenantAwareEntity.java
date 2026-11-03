package com.scmcloud.common.tenant;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.io.Serializable;

/**
 * Legacy minimal tenant-aware base class.
 *
 * @deprecated since 1.1.0 — use
 *   {@link com.scmcloud.common.entity.TenantAwareEntity} which also includes
 *   audit fields, optimistic locking, and Snowflake ID. This class is kept for
 *   binary compatibility in v1.x and will be removed in v2.0.
 */
@Data
@Deprecated(since = "1.1.0", forRemoval = true)
public abstract class TenantAwareEntity implements Serializable {

    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    private String tenantId;
}

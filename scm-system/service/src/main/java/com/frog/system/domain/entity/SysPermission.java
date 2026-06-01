package com.frog.system.domain.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 权限表
 * </p>
 *
 * @author author
 * @since 2025-10-14
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("sys_permission")
public class SysPermission {

    @TableId(value = "id", type = IdType.NONE)
    private UUID id;

    @TableField("tenant_id")
    private UUID tenantId;

    private UUID parentId;

    private String permissionCode;

    private String permissionName;

    private Integer permissionType;

    @TableField("permission_scope")
    private String permissionScope;

    private String routePath;

    private String component;

    private String redirect;

    private String icon;

    private String apiPath;

    @TableField("http_method")
    private String httpMethod;

    private Integer permissionLevel;

    private Integer riskLevel;

    private Boolean needApproval;

    private Boolean needTwoFactor;

    private Integer sortOrder;

    private Boolean visible;

    private Integer status;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "create_by", fill = FieldFill.INSERT)
    private UUID createBy;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(value = "update_by", fill = FieldFill.INSERT_UPDATE)
    private UUID updateBy;

    @TableLogic(value = "false", delval = "true")
    private Boolean deleted;
}

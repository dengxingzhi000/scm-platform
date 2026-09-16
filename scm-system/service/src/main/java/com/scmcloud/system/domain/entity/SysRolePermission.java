package com.scmcloud.system.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 瑙掕壊鏉冮檺鍏宠仈锟?
 *
 * @author Deng
 * @since 2025-12-17
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("sys_role_permission")
public class SysRolePermission {

    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private UUID id;

    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    private UUID tenantId;

    @TableField("role_id")
    private UUID roleId;

    @TableField("permission_id")
    private UUID permissionId;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "create_by", fill = FieldFill.INSERT)
    private UUID createBy;
}
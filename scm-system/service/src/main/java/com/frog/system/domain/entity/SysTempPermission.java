package com.frog.system.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 临时权限表- 用于临时授权
 *
 * @author Deng
 * @since 2025-12-17
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("sys_temp_permission")
public class SysTempPermission {

    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private UUID id;

    @TableField("user_id")
    private UUID userId;

    @TableField("permission_id")
    private UUID permissionId;

    @TableField("approval_id")
    private UUID approvalId;

    @TableField("effective_time")
    private LocalDateTime effectiveTime;

    @TableField("expire_time")
    private LocalDateTime expireTime;

    @TableField("status")
    private Integer status;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "create_by", fill = FieldFill.INSERT)
    private UUID createBy;

    /**
     * 判断是否在有效期内
     */
    public boolean isEffective() {
        if (status == null || status != 1) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        return now.isAfter(effectiveTime) && now.isBefore(expireTime);
    }
}
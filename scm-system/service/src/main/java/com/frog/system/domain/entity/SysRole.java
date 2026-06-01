package com.frog.system.domain.entity;

import java.math.BigDecimal;

import com.baomidou.mybatisplus.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 角色表
 * </p>
 *
 * @author author
 * @since 2025-10-15
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("sys_role")
public class SysRole {

    @TableId(value = "id", type = IdType.NONE)
    private UUID id;

    @TableField("tenant_id")
    private UUID tenantId;

    private String roleCode;

    private String roleName;

    private String roleDesc;

    @TableField("role_level")
    private Integer roleLevel;

    private Integer dataScope;

    private BigDecimal maxApprovalAmount;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> businessScope;

    @TableField("role_type")
    private String roleType;

    @TableField("role_category")
    private String roleCategory;

    @TableField(value = "custom_dept_ids", typeHandler = JacksonTypeHandler.class)
    private List<UUID> customDeptIds;

    private Integer status;

    private Integer sortOrder;

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

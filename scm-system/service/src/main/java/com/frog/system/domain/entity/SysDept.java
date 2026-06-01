package com.frog.system.domain.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 部门表
 * </p>
 *
 * @author author
 * @since 2025-11-07
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("sys_dept")
public class SysDept {

    @TableId(value = "id", type = IdType.NONE)
    private UUID id;

    @TableField("tenant_id")
    private UUID tenantId;

    @TableField("parent_id")
    private UUID parentId;

    @TableField("dept_code")
    private String deptCode;

    @TableField("dept_name")
    private String deptName;

    @TableField("dept_type")
    private Integer deptType;

    @TableField("dept_level")
    private Integer deptLevel;

    @TableField("dept_path")
    private String deptPath;

    @TableField("leader_id")
    private UUID leaderId;

    @TableField("phone")
    private String phone;

    @TableField("email")
    private String email;

    @TableField("isolation_level")
    private Integer isolationLevel;

    @TableField("sort_order")
    private Integer sortOrder;

    @TableField("status")
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
    @TableField("deleted")
    private Boolean deleted;

    // ==================== 冗余字段（来自db_user.sys_user？===================

    @TableField("leader_name")
    private String leaderName;

    @TableField("leader_phone")
    private String leaderPhone;
}

package com.scmcloud.common.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Base entity with tenant isolation and audit fields.
 * 
 * All business entities should extend this class to ensure:
 * 1. tenant_id is automatically populated on INSERT
 * 2. Audit fields (createTime, updateTime, createBy, updateBy) are auto-filled
 * 3. Optimistic locking via version field
 *
 * @author SCM Platform
 * @since 2026-06-04
 */
@Data
public abstract class TenantAwareEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Primary key - uses Snowflake algorithm by default
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * Tenant ID - automatically populated on INSERT by MetaObjectHandler
     */
    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    private String tenantId;

    /**
     * Record creation time - auto-filled on INSERT
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * Record creator - auto-filled on INSERT
     */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    private String createBy;

    /**
     * Last update time - auto-filled on INSERT and UPDATE
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * Last updater - auto-filled on INSERT and UPDATE
     */
    @TableField(value = "update_by", fill = FieldFill.INSERT_UPDATE)
    private String updateBy;

    /**
     * Optimistic lock version
     */
    @Version
    @TableField("version")
    private Integer version;

    /**
     * Logical deletion flag
     */
    @TableField("deleted")
    private Boolean deleted;

    /**
     * Remark/notes
     */
    @TableField("remark")
    private String remark;
}

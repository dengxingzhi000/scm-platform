package com.scmcloud.notify.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 用户站内通知表
 * </p>
 *
 * <p>
 * 与 {@code sys_notification_audit}（发送审计）不同，本表保存用户在消息中心
 * 看到的一条条通知及其已读状态。
 * </p>
 *
 * @author deng
 * @since 2025-12-26
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("sys_user_notification")
public class SysUserNotification implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    private String tenantId;

    @TableId(value = "id", type = IdType.NONE)
    private String id;

    @TableField("user_id")
    private String userId;

    @TableField("title")
    private String title;

    @TableField("content")
    private String content;

    /**
     * 通知类型：SYSTEM / ORDER / INVENTORY / APPROVAL / SECURITY
     */
    @TableField("type")
    private String type;

    @TableField("is_read")
    private Boolean isRead;

    @TableField("link")
    private String link;

    /** Stable key from the producing event; makes repeated deliveries idempotent. */
    @TableField("business_key")
    private String businessKey;

    @TableField("read_at")
    private LocalDateTime readAt;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField("deleted")
    private Boolean deleted;

    @TableField("remark")
    private String remark;
}

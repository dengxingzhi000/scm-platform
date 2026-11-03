package com.scmcloud.system.domain.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 状态流转表
 * </p>
 *
 * @author author
 * @since 2025-10-15
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("sys_status_transition")
public class SysStatusTransition implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.NONE)
    private String id;

    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    private UUID tenantId;

    @TableField("biz_type")
    private String bizType;

    @TableField("from_status")
    private String fromStatus;

    @TableField("to_status")
    private String toStatus;

    @TableField("action_code")
    private String actionCode;

    @TableField("action_name")
    private String actionName;

    @TableField("action_name_en")
    private String actionNameEn;

    @TableField("need_approval")
    private Boolean needApproval;

    @TableField("enabled")
    private Boolean enabled;

    @TableField("condition_expression")
    private String conditionExpression;

    @TableField("pre_action")
    private String preAction;

    @TableField("post_action")
    private String postAction;

    @TableField("sort_order")
    private Integer sortOrder;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "create_by", fill = FieldFill.INSERT)
    private String createBy;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(value = "update_by", fill = FieldFill.INSERT_UPDATE)
    private String updateBy;

    @TableLogic(value = "false", delval = "true")
    @TableField("deleted")
    private Boolean deleted;

    @TableField("remark")
    private String remark;
}

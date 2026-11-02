package com.scmcloud.decision.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_decision_rule")
public class DecisionRuleEntity {
    @TableId(type = IdType.NONE)
    private String id;

    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    private String tenantId;

    private String engineType;
    private String scene;
    private String ruleType;
    private String expression;
    private String description;
    private Boolean enabled;
    private Integer priority;
    private Integer version;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** Postgres BOOLEAN, so the logic-delete literals must be the words, not 1/0. */
    @TableLogic(value = "false", delval = "true")
    private Boolean deleted;
}

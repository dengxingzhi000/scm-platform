package com.scmcloud.decision.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_decision_rule")
public class DecisionRuleEntity {
    @TableId(type = IdType.NONE)
    private String id;
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
}

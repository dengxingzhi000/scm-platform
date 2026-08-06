package com.scmcloud.decision.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_decision_feedback")
public class DecisionFeedbackEntity {
    @TableId(type = IdType.NONE)
    private String id;
    private String engineType;
    private String decisionId;
    private String outcome;
    private String metrics;
    private String context;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}

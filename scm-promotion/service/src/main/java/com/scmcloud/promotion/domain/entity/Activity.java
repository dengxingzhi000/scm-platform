package com.scmcloud.promotion.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("pro_activity")
public class Activity {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("activity_name")
    private String activityName;

    @TableField("activity_type")
    private String activityType;

    @TableField("start_time")
    private LocalDateTime startTime;

    @TableField("end_time")
    private LocalDateTime endTime;

    @TableField("status")
    private Integer status;

    @TableField("rules_json")
    private String rulesJson;

    @TableField("applicable_scope")
    private String applicableScope;

    @TableField("created_by")
    private String createdBy;

    @TableField("created_at")
    private LocalDateTime createdAt;
}

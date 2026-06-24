package com.scmcloud.decision.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_weight_profile")
public class WeightProfileEntity {
    @TableId(type = IdType.NONE)
    private String id;
    private String engineType;
    private String scene;
    private Integer version;
    private String weights;
    private String conditions;
    private Boolean active;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}

package com.cognora.platform.decision.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_weight_profile")
public class WeightProfileEntity {
    @TableId(type = IdType.NONE)
    private String id;

    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    private String tenantId;

    private String engineType;
    private String scene;
    private Integer version;

    /** jsonb column holding the dimension weights; kept as raw JSON text. */
    private String weights;

    /** jsonb column holding the condition list; kept as raw JSON text. */
    private String conditions;

    private Boolean active;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** Postgres BOOLEAN, so the logic-delete literals must be the words, not 1/0. */
    @TableLogic(value = "false", delval = "true")
    private Boolean deleted;
}

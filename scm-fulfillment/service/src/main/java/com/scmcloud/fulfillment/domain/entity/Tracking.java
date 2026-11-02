package com.scmcloud.fulfillment.domain.entity;

import com.scmcloud.common.domain.TenantId;
import com.baomidou.mybatisplus.annotation.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("ful_tracking")
public class Tracking {

    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    private TenantId tenantId;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @NotBlank(message = "履约单号不能为空")
    @TableField("fulfillment_no")
    private String fulfillmentNo;

    @TableField("tracking_no")
    private String trackingNo;

    @TableField("carrier")
    private String carrier;

    @NotBlank(message = "跟踪状态不能为空")
    @TableField("status")
    private String status;

    @TableField("location")
    private String location;

    @TableField("description")
    private String description;

    @TableField("event_time")
    private LocalDateTime eventTime;

    @TableField("created_at")
    private LocalDateTime createdAt;
}

package com.scmcloud.message.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.util.Date;

@Data
@TableName("sys_event_outbox")
public class EventOutbox {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String eventType;
    private String aggregateType;
    private String aggregateId;
    private String payload;
    private Integer retryCount;
    private Integer maxRetries;
    private String status;
    private String errorMessage;
    private Long tenantId;
    private Date createTime;
    private Date publishedAt;
    private Date nextRetryAt;
}

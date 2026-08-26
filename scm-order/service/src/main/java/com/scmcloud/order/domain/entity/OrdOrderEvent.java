package com.scmcloud.order.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@TableName("ord_order_event")
public class OrdOrderEvent {
    @TableId(type = IdType.AUTO)
    private Long id;

    private UUID eventId;

    @TableField("tenant_id")
    private UUID tenantId;

    private Long orderId;

    @TableField("order_no")
    private String orderNo;

    private String eventType;

    private String eventData;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}

package com.cognora.lms.logistics.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * SLA 超时预警记录（批次 3）。同一运单同一承诺时间只生成一条，扫描任务幂等。
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tms_sla_alert")
public class TmsSlaAlert implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public static final String STATUS_OPEN = "OPEN";
    public static final String STATUS_ACK = "ACK";
    public static final String STATUS_CLOSED = "CLOSED";

    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    private String tenantId;

    @TableId(value = "id", type = IdType.NONE)
    private String id;

    @TableField("waybill_id")
    private String waybillId;

    @TableField("waybill_no")
    private String waybillNo;

    @TableField("carrier_id")
    private String carrierId;

    @TableField("promised_time")
    private LocalDateTime promisedTime;

    @TableField("overdue_hours")
    private BigDecimal overdueHours;

    /** WARN / CRITICAL。 */
    @TableField("level")
    private String level;

    /** OPEN / ACK / CLOSED。 */
    @TableField("status")
    private String status;

    @TableField("ack_by")
    private String ackBy;

    @TableField("ack_time")
    private LocalDateTime ackTime;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    @TableField("remark")
    private String remark;
}

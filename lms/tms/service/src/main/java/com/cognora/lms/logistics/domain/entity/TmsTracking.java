package com.cognora.lms.logistics.domain.entity;

import java.io.Serial;
import java.math.BigDecimal;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 鐗╂祦杞ㄨ抗锟?
 * </p>
 *
 * @author deng
 * @since 2025-12-26
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tms_tracking")
public class TmsTracking implements Serializable {
    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    private String tenantId;

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.NONE)
    private String id;

    @TableField("waybill_id")
    private String waybillId;

    @TableField("waybill_no")
    private String waybillNo;

    /** 承运商回传的真实跟踪号；写入运单跟踪号字段并随轨迹留存。 */
    @TableField("tracking_no")
    private String trackingNo;

    /** 承运商推送的事件 ID；同一租户下唯一，用于回调幂等。 */
    @TableField("event_id")
    private String eventId;

    /** 轨迹内容的 SHA-256 指纹，未传 eventId 时用于生成稳定的幂等键。 */
    @TableField("payload_hash")
    private String payloadHash;

    @TableField("track_time")
    private LocalDateTime trackTime;

    @TableField("location")
    private String location;

    @TableField("description")
    private String description;

    @TableField("longitude")
    private BigDecimal longitude;

    @TableField("latitude")
    private BigDecimal latitude;

    @TableField("track_status")
    private String trackStatus;

    @TableField("operator")
    private String operator;

    @TableField("source")
    private String source;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("extra_data")
    private String extraData;

}

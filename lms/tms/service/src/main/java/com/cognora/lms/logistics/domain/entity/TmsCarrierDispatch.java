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
import java.time.LocalDateTime;

/**
 * 承运商下单派发记录（批次 3）：幂等键 + 实际承运商 + 降级结果。
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tms_carrier_dispatch")
public class TmsCarrierDispatch implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public static final String RESULT_RUNNING = "RUNNING";
    public static final String RESULT_SUCCESS = "SUCCESS";
    public static final String RESULT_FALLBACK = "FALLBACK";
    public static final String RESULT_FAILED = "FAILED";

    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    private String tenantId;

    @TableId(value = "id", type = IdType.NONE)
    private String id;

    /** 下单幂等键：同一租户内唯一，重复请求直接返回既有记录。 */
    @TableField("idempotency_key")
    private String idempotencyKey;

    @TableField("waybill_no")
    private String waybillNo;

    @TableField("primary_carrier_id")
    private String primaryCarrierId;

    @TableField("actual_carrier_id")
    private String actualCarrierId;

    @TableField("attempt_count")
    private Integer attemptCount;

    /** RUNNING / SUCCESS / FALLBACK / FAILED。 */
    @TableField("result")
    private String result;

    @TableField("error_message")
    private String errorMessage;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}

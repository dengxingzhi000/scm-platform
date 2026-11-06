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
 * 异常件单据（批次 3）：责任方认领 + 处理闭环。
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tms_exception_case")
public class TmsExceptionCase implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    private String tenantId;

    @TableId(value = "id", type = IdType.NONE)
    private String id;

    @TableField("waybill_id")
    private String waybillId;

    @TableField("waybill_no")
    private String waybillNo;

    @TableField("exception_type")
    private String exceptionType;

    @TableField("description")
    private String description;

    /** 责任方：CARRIER / WAREHOUSE / CUSTOMER / PLATFORM。 */
    @TableField("responsible_party")
    private String responsibleParty;

    /** NEW / CLAIMED / PROCESSING / RESOLVED / CLOSED。 */
    @TableField("status")
    private String status;

    @TableField("claimed_by")
    private String claimedBy;

    @TableField("claimed_time")
    private LocalDateTime claimedTime;

    @TableField("resolution")
    private String resolution;

    @TableField("resolved_time")
    private LocalDateTime resolvedTime;

    @TableField("create_by")
    private String createBy;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    @TableField("remark")
    private String remark;
}

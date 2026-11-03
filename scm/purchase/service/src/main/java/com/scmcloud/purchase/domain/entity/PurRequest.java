package com.scmcloud.purchase.domain.entity;

import java.math.BigDecimal;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.IdType;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 采购申请单表
 * </p>
 *
 * @author deng
 * @since 2025-12-25
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("pur_request")
public class PurRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.NONE)
    private String id;

    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    private String tenantId;

    private String requestNo;
    private Integer requestType;
    private Integer priority;

    private String deptId;

    private String deptName;

    private String requesterId;

    private String requesterName;

    private String requesterPhone;

    private LocalDateTime expectedDelivery;

    private String purpose;

    private BigDecimal budgetAmount;
    private Integer status;

    private String approvalFlowId;

    private String currentApproverId;

    private String currentApproverName;

    private LocalDateTime submittedAt;

    private LocalDateTime approvedAt;

    private LocalDateTime rejectedAt;

    private String rejectReason;

    private Boolean converted;

    private String purchaseOrderId;

    private String purchaseOrderNo;

    private String attachments;

    private LocalDateTime createTime;

    private String createBy;

    private LocalDateTime updateTime;

    private String updateBy;

    private Boolean deleted;

    private String remark;


}

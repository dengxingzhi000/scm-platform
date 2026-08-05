package com.scmcloud.order.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.scmcloud.common.domain.Money;
import com.scmcloud.common.domain.TenantId;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 閫€锟介€€璐ц〃
 * </p>
 *
 * @author deng
 * @since 2025-12-25
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("ord_refund")
public class OrdRefund implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    private TenantId tenantId;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("refund_no")
    private String refundNo;

    @TableField("order_id")
    private Long orderId;

    @TableField("order_no")
    private String orderNo;

    @TableField("user_id")
    private String userId;
    @TableField("refund_type")
    private Integer refundType;

    @TableField("reason")
    private String reason;

    @TableField("description")
    private String description;

    @TableField("evidence_images")
    private String evidenceImages;

    @TableField("refund_items")
    private String refundItems;

    @TableField("refund_amount")
    private Money refundAmount;
    @TableField("status")
    private Integer status;

    @TableField("approved_at")
    private LocalDateTime approvedAt;

    @TableField("rejected_at")
    private LocalDateTime rejectedAt;

    @TableField("completed_at")
    private LocalDateTime completedAt;

    @TableField("handler_id")
    private String handlerId;

    @TableField("handler_name")
    private String handlerName;

    @TableField("handler_remark")
    private String handlerRemark;

    @TableField("return_waybill_no")
    private String returnWaybillNo;

    @TableField("return_carrier")
    private String returnCarrier;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    @TableField("remark")
    private String remark;


}

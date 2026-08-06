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
 * 鏀粯璁板綍锟?
 * </p>
 *
 * @author deng
 * @since 2025-12-25
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("ord_payment")
public class OrdPayment implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    private TenantId tenantId;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("payment_no")
    private String paymentNo;

    @TableField("order_id")
    private Long orderId;

    @TableField("order_no")
    private String orderNo;

    @TableField("user_id")
    private String userId;

    @TableField("payment_method")
    private Integer paymentMethod;

    @TableField("payment_channel")
    private String paymentChannel;

    @TableField("payment_amount")
    private Money paymentAmount;
    @TableField("status")
    private Integer status;

    @TableField("third_party_no")
    private String thirdPartyNo;
    @TableField("third_party_response")
    private String thirdPartyResponse;

    @TableField("initiated_at")
    private LocalDateTime initiatedAt;

    @TableField("paid_at")
    private LocalDateTime paidAt;

    @TableField("failed_at")
    private LocalDateTime failedAt;

    @TableField("refunded_at")
    private LocalDateTime refundedAt;

    @TableField("refund_amount")
    private Money refundAmount;

    @TableField("refund_reason")
    private String refundReason;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    @TableField("remark")
    private String remark;


}

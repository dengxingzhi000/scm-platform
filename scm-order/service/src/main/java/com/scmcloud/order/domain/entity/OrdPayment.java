package com.scmcloud.order.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.scmcloud.common.domain.Money;
import com.scmcloud.common.domain.TenantId;
import java.time.LocalDateTime;
import java.util.UUID;
import com.baomidou.mybatisplus.annotation.TableId;
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
public class OrdPayment {

    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    private TenantId tenantId;

    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private UUID id;

    @TableField("payment_no")
    private String paymentNo;

    @TableField("order_id")
    private UUID orderId;

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

    // ─── Domain Behavior ─────────────────────────────────────────

    /**
     * 返回当前状态对应的 {@link PaymentStatus} 枚举。
     */
    public PaymentStatus getStatusEnum() {
        return status == null ? null : PaymentStatus.fromCode(status);
    }

    /**
     * 通过枚举设置状态（写入 {@code status} 整数字段）。
     */
    public OrdPayment setStatusEnum(PaymentStatus paymentStatus) {
        this.status = paymentStatus == null ? null : paymentStatus.getCode();
        return this;
    }
}

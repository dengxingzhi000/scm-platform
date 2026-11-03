package com.cognora.ecom.ordercenter.domain.entity;

import com.cognora.platform.common.domain.TenantId;
import com.baomidou.mybatisplus.annotation.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("oc_order")
public class OcOrder {

    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    private TenantId tenantId;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @NotBlank(message = "订单号不能为空")
    @TableField("order_no")
    private String orderNo;

    @TableField("parent_order_no")
    private String parentOrderNo;

    @TableField("channel")
    private String channel;

    @TableField("order_type")
    private String orderType;

    @NotBlank(message = "用户 ID 不能为空")
    @TableField("user_id")
    private String userId;

    @TableField("seller_id")
    private Long sellerId;

    @TableField("status")
    private Integer status;

    @DecimalMin(value = "0.00", message = "订单总额不能为负数")
    @TableField("total_amount")
    private BigDecimal totalAmount;

    @DecimalMin(value = "0.00", message = "优惠金额不能为负数")
    @TableField("discount_amount")
    private BigDecimal discountAmount;

    @DecimalMin(value = "0.00", message = "应付金额不能为负数")
    @TableField("payable_amount")
    private BigDecimal payableAmount;

    @TableField("paid_amount")
    private BigDecimal paidAmount;

    @DecimalMin(value = "0.00", message = "运费不能为负数")
    @TableField("shipping_fee")
    private BigDecimal shippingFee;

    @TableField("receiver_name")
    private String receiverName;

    @TableField("receiver_phone")
    private String receiverPhone;

    @TableField("receiver_address")
    private String receiverAddress;

    @TableField("remark")
    private String remark;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}

package com.scmcloud.order.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.scmcloud.common.domain.Money;
import com.scmcloud.common.domain.TenantId;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 退款单明细（{@code ord_refund_item} 表）。
 *
 * <p>每行对应一个 SKU 的退款记录，关联 {@link OrdRefund}（{@code refund_id}）
 * 与 {@link OrdOrderItem}（{@code order_item_id}）。</p>
 *
 * @author SCM Platform Team
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("ord_refund_item")
public class OrdRefundItem {

    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    private TenantId tenantId;

    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private UUID id;

    @TableField("refund_id")
    private UUID refundId;

    @TableField("refund_no")
    private String refundNo;

    @TableField("order_id")
    private UUID orderId;

    @TableField("order_no")
    private String orderNo;

    @TableField("order_item_id")
    private UUID orderItemId;

    @TableField("sku_id")
    private UUID skuId;

    @TableField("sku_code")
    private String skuCode;

    @TableField("sku_name")
    private String skuName;

    @TableField("quantity")
    private Integer quantity;

    @TableField("refund_amount")
    private Money refundAmount;

    @TableField("remark")
    private String remark;

    @TableField("create_time")
    private LocalDateTime createTime;
}

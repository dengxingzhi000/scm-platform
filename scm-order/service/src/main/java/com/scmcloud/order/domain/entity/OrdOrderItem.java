package com.scmcloud.order.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.scmcloud.common.domain.Money;
import com.scmcloud.common.domain.Quantity;
import com.scmcloud.common.domain.TenantId;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 璁㈠崟鏄庣粏锟?
 * </p>
 *
 * @author deng
 * @since 2025-12-25
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("ord_order_item")
public class OrdOrderItem {

    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    private TenantId tenantId;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("order_id")
    private Long orderId;

    @TableField("order_no")
    private String orderNo;

    @TableField("sku_id")
    private String skuId;

    @TableField("spu_id")
    private String spuId;

    @TableField("sku_code")
    private String skuCode;

    @TableField("sku_name")
    private String skuName;

    @TableField("spu_name")
    private String spuName;
    @TableField("attributes")
    private String attributes;

    @TableField("image_url")
    private String imageUrl;

    @TableField("original_price")
    private Money originalPrice;

    @TableField("selling_price")
    private Money sellingPrice;

    @TableField("quantity")
    private Quantity quantity;

    @TableField("subtotal")
    private Money subtotal;

    @TableField("discount_amount")
    private Money discountAmount;

    @TableField("final_amount")
    private Money finalAmount;

    @TableField("warehouse_id")
    private String warehouseId;

    @TableField("refund_quantity")
    private Quantity refundQuantity;

    @TableField("refund_amount")
    private Money refundAmount;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("remark")
    private String remark;
}

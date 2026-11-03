package com.cognora.ecom.ordercenter.domain.entity;

import com.cognora.platform.common.domain.TenantId;
import com.baomidou.mybatisplus.annotation.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("oc_order_item")
public class OcOrderItem {

    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    private TenantId tenantId;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("order_no")
    private String orderNo;

    @TableField("product_id")
    private Long productId;

    @TableField("sku_id")
    private Long skuId;

    @TableField("product_name")
    private String productName;

    @TableField("sku_name")
    private String skuName;

    @DecimalMin(value = "0.00", message = "商品单价不能为负数")
    @TableField("price")
    private BigDecimal price;

    @Min(value = 1, message = "商品数量必须大于 0")
    @TableField("quantity")
    private Integer quantity;

    @TableField("discount_amount")
    private BigDecimal discountAmount;

    @DecimalMin(value = "0.00", message = "明细应付金额不能为负数")
    @TableField("payable_amount")
    private BigDecimal payableAmount;

    @TableField("seller_id")
    private Long sellerId;

    @TableField("status")
    private Integer status;

    @TableField("created_at")
    private LocalDateTime createdAt;
}

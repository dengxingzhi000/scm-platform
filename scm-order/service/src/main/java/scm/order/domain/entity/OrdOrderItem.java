package scm.order.domain.entity;

import java.math.BigDecimal;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.IdType;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 订单明细表
 * </p>
 *
 * @author deng
 * @since 2025-12-25
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("ord_order_item")
@Schema(description = "订单明细表")
public class OrdOrderItem implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.NONE)
    private String id;

    @TableField("order_id")
    private String orderId;

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

    @Schema(description = "下单时SKU属性快照")
    @TableField("attributes")
    private String attributes;

    @TableField("image_url")
    private String imageUrl;

    @TableField("original_price")
    private BigDecimal originalPrice;

    @TableField("selling_price")
    private BigDecimal sellingPrice;

    @TableField("quantity")
    private Integer quantity;

    @TableField("subtotal")
    private BigDecimal subtotal;

    @TableField("discount_amount")
    private BigDecimal discountAmount;

    @TableField("final_amount")
    private BigDecimal finalAmount;

    @TableField("warehouse_id")
    private String warehouseId;

    @TableField("refund_quantity")
    private Integer refundQuantity;

    @TableField("refund_amount")
    private BigDecimal refundAmount;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("remark")
    private String remark;


}

package scm.supplier.domain.entity;

import java.math.BigDecimal;
import com.baomidou.mybatisplus.annotation.TableName;
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
 * 采购单明细表
 * </p>
 *
 * @author author
 * @since 2025-12-25
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("sup_purchase_order_item")
@Schema(description = "采购单明细表")
public class SupPurchaseOrderItem implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableField("id")
    private String id;

    @TableId(value = "id", type = IdType.UUID)
    private String id;

    @TableField("purchase_id")
    private String purchaseId;

    @TableField("purchase_no")
    private String purchaseNo;

    @TableField("sku_id")
    private String skuId;

    @TableField("sku_code")
    private String skuCode;

    @TableField("sku_name")
    private String skuName;

    @TableField("quantity")
    private Integer quantity;

    @TableField("quantity")
    private Integer quantity;

    @TableField("received_quantity")
    private Integer receivedQuantity;

    @TableField("unit_price")
    private BigDecimal unitPrice;

    @TableField("unit_price")
    private BigDecimal unitPrice;

    @TableField("tax_rate")
    private BigDecimal taxRate;

    @TableField("subtotal")
    private BigDecimal subtotal;

    @TableField("subtotal")
    private BigDecimal subtotal;

    @TableField("quality_requirement")
    private String qualityRequirement;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("remark")
    private String remark;


}

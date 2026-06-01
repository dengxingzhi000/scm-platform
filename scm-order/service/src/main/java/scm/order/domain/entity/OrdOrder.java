package scm.order.domain.entity;

import java.math.BigDecimal;
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
 * 订单表（分区表）
 * </p>
 *
 * @author deng
 * @since 2025-12-25
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("ord_order")
public class OrdOrder {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private String id;
    @TableField("order_no")
    private String orderNo;
    @TableField("user_id")
    private String userId;
    @TableField("username")
    private String username;
    @TableField("order_type")
    private Integer orderType;
    @TableField("order_source")
    private String orderSource;
    @TableField("status")
    private Integer status;
    @TableField("total_amount")
    private BigDecimal totalAmount;
    @TableField("discount_amount")
    private BigDecimal discountAmount;
    @TableField("freight_amount")
    private BigDecimal freightAmount;
    @TableField("payable_amount")
    private BigDecimal payableAmount;
    @TableField("payment_method")
    private Integer paymentMethod;
    @TableField("payment_no")
    private String paymentNo;
    @TableField("paid_amount")
    private BigDecimal paidAmount;
    @TableField("paid_at")
    private LocalDateTime paidAt;
    @TableField("shipping_address")
    private String shippingAddress;
    @TableField("invoice_required")
    private Boolean invoiceRequired;
    @TableField("invoice_info")
    private String invoiceInfo;
    @TableField("warehouse_id")
    private String warehouseId;
    @TableField("sku_id")
    private String skuId;
    @TableField("quantity")
    private Integer quantity;
    @TableField("waybill_no")
    private String waybillNo;
    @TableField("carrier")
    private String carrier;
    @TableField("shipped_at")
    private LocalDateTime shippedAt;
    @TableField("estimated_arrival")
    private LocalDateTime estimatedArrival;
    @TableField("payment_deadline")
    private LocalDateTime paymentDeadline;
    @TableField("auto_cancel_at")
    private LocalDateTime autoCancelAt;
    @TableField("auto_complete_at")
    private LocalDateTime autoCompleteAt;
    @TableField("completed_at")
    private LocalDateTime completedAt;
    @TableField("cancelled_at")
    private LocalDateTime cancelledAt;
    @TableField("cancel_reason")
    private String cancelReason;
    @TableField("reservation_id")
    private String reservationId;
    @TableField("buyer_message")
    private String buyerMessage;
    @TableField("seller_message")
    private String sellerMessage;
    @TableField("tags")
    private String tags;
    @TableField("extra_data")
    private String extraData;
    @TableField("create_time")
    private LocalDateTime createTime;
    @TableField("create_by")
    private String createBy;
    @TableField("update_time")
    private LocalDateTime updateTime;
    @TableField("update_by")
    private String updateBy;
    @TableField("deleted")
    private Boolean deleted;
    @TableField("remark")
    private String remark;
}

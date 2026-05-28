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
@Schema(description = "订单表")
public class OrdOrder {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private String id;

    @Schema(description = "订单号")
    @TableField("order_no")
    private String orderNo;

    @Schema(description = "用户ID")
    @TableField("user_id")
    private String userId;

    @Schema(description = "用户名")
    @TableField("username")
    private String username;

    @Schema(description = "订单类型:1-普通,2-秒杀,3-预售,4-团购")
    @TableField("order_type")
    private Integer orderType;

    @Schema(description = "订单来源:WEB,MOBILE,WECHAT,API")
    @TableField("order_source")
    private String orderSource;

    @Schema(description = "状态:0-待支付,1-已支付,2-待发货,3-已发货,4-运输中,5-已送达,6-已完成,7-已取消,8-退款中,9-已退款")
    @TableField("status")
    private Integer status;

    @Schema(description = "总金额")
    @TableField("total_amount")
    private BigDecimal totalAmount;

    @Schema(description = "优惠金额")
    @TableField("discount_amount")
    private BigDecimal discountAmount;

    @Schema(description = "运费")
    @TableField("freight_amount")
    private BigDecimal freightAmount;

    @Schema(description = "应付金额")
    @TableField("payable_amount")
    private BigDecimal payableAmount;

    @Schema(description = "支付方式:1-支付宝,2-微信,3-银行卡,4-余额,5-货到付款")
    @TableField("payment_method")
    private Integer paymentMethod;

    @Schema(description = "支付流水号")
    @TableField("payment_no")
    private String paymentNo;

    @Schema(description = "已付金额")
    @TableField("paid_amount")
    private BigDecimal paidAmount;

    @Schema(description = "支付时间")
    @TableField("paid_at")
    private LocalDateTime paidAt;

    @Schema(description = "收货地址（JSON）")
    @TableField("shipping_address")
    private String shippingAddress;

    @Schema(description = "是否需要发票")
    @TableField("invoice_required")
    private Boolean invoiceRequired;

    @Schema(description = "发票信息（JSON）")
    @TableField("invoice_info")
    private String invoiceInfo;

    @Schema(description = "仓库ID")
    @TableField("warehouse_id")
    private String warehouseId;

    @Schema(description = "运单号")
    @TableField("waybill_no")
    private String waybillNo;

    @Schema(description = "承运商")
    @TableField("carrier")
    private String carrier;

    @Schema(description = "发货时间")
    @TableField("shipped_at")
    private LocalDateTime shippedAt;

    @Schema(description = "预计到达时间")
    @TableField("estimated_arrival")
    private LocalDateTime estimatedArrival;

    @Schema(description = "支付截止时间")
    @TableField("payment_deadline")
    private LocalDateTime paymentDeadline;

    @Schema(description = "自动取消时间")
    @TableField("auto_cancel_at")
    private LocalDateTime autoCancelAt;

    @Schema(description = "自动完成时间")
    @TableField("auto_complete_at")
    private LocalDateTime autoCompleteAt;

    @Schema(description = "完成时间")
    @TableField("completed_at")
    private LocalDateTime completedAt;

    @Schema(description = "取消时间")
    @TableField("cancelled_at")
    private LocalDateTime cancelledAt;

    @Schema(description = "取消原因")
    @TableField("cancel_reason")
    private String cancelReason;

    @Schema(description = "库存预占ID")
    @TableField("reservation_id")
    private String reservationId;

    @Schema(description = "买家留言")
    @TableField("buyer_message")
    private String buyerMessage;

    @Schema(description = "卖家留言")
    @TableField("seller_message")
    private String sellerMessage;

    @Schema(description = "标签")
    @TableField("tags")
    private String tags;

    @Schema(description = "扩展数据（JSON）")
    @TableField("extra_data")
    private String extraData;

    @Schema(description = "创建时间")
    @TableField("create_time")
    private LocalDateTime createTime;

    @Schema(description = "创建人")
    @TableField("create_by")
    private String createBy;

    @Schema(description = "更新时间")
    @TableField("update_time")
    private LocalDateTime updateTime;

    @Schema(description = "更新人")
    @TableField("update_by")
    private String updateBy;

    @Schema(description = "是否删除")
    @TableField("deleted")
    private Boolean deleted;

    @Schema(description = "备注")
    @TableField("remark")
    private String remark;
}

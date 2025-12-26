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
 * 退款/退货表
 * </p>
 *
 * @author deng
 * @since 2025-12-25
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("ord_refund")
@Schema(description = "退款/退货表")
public class OrdRefund implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.NONE)
    private String id;

    @TableField("refund_no")
    private String refundNo;

    @TableField("order_id")
    private String orderId;

    @TableField("order_no")
    private String orderNo;

    @TableField("user_id")
    private String userId;

    @Schema(description = "退款类型:1-仅退款,2-退货退款")
    @TableField("refund_type")
    private Integer refundType;

    @TableField("reason")
    private String reason;

    @TableField("description")
    private String description;

    @TableField("evidence_images")
    private String evidenceImages;

    @TableField("refund_items")
    private String refundItems;

    @TableField("refund_amount")
    private BigDecimal refundAmount;

    @Schema(description = "状态:0-待审核,1-已同意,2-已拒绝,3-已完成")
    @TableField("status")
    private Integer status;

    @TableField("approved_at")
    private LocalDateTime approvedAt;

    @TableField("rejected_at")
    private LocalDateTime rejectedAt;

    @TableField("completed_at")
    private LocalDateTime completedAt;

    @TableField("handler_id")
    private String handlerId;

    @TableField("handler_name")
    private String handlerName;

    @TableField("handler_remark")
    private String handlerRemark;

    @TableField("return_waybill_no")
    private String returnWaybillNo;

    @TableField("return_carrier")
    private String returnCarrier;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    @TableField("remark")
    private String remark;


}

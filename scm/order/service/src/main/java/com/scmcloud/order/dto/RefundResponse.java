package com.scmcloud.order.dto;

import com.scmcloud.order.domain.entity.RefundStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 退款单响应。
 *
 * @author SCM Platform Team
 */
@Data
public class RefundResponse {
    private UUID id;
    private String refundNo;
    private UUID orderId;
    private String orderNo;
    private String userId;
    private Integer refundType;
    private String reason;
    private String description;
    private BigDecimal refundAmount;
    private String refundStatus;
    private LocalDateTime approvedAt;
    private LocalDateTime rejectedAt;
    private LocalDateTime completedAt;
    private String handlerId;
    private String handlerName;
    private String handlerRemark;
    private String returnWaybillNo;
    private String returnCarrier;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private String remark;

    /** 退款明细行（{@code GET /refunds/{id}/items} 单独加载） */
    private List<RefundItemResponse> items;
}

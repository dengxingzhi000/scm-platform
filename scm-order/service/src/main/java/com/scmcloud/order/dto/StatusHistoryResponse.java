package com.scmcloud.order.dto;

import com.scmcloud.order.domain.entity.OrderStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 状态历史响应（append-only 审计日志）。
 *
 * @author SCM Platform Team
 */
@Data
public class StatusHistoryResponse {
    private UUID id;
    private UUID orderId;
    private String orderNo;
    private Integer fromStatus;
    private String fromStatusName;
    private OrderStatus toStatus;
    private String toStatusName;
    private String event;
    private String operatorId;
    private String operatorName;
    private String operatorType;
    private String remark;
    private LocalDateTime transitionedAt;
}

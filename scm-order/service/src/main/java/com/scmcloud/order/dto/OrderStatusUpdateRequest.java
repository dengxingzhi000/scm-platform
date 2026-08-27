package com.scmcloud.order.dto;

import com.scmcloud.order.domain.entity.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.UUID;

/**
 * 订单状态流转请求。
 *
 * <p>PUT /api/v1/orders/{id}/status 的请求体。</p>
 *
 * <p>状态机校验由 {@code OrdOrderCommandService} 通过 Dubbo 状态机服务完成。</p>
 *
 * @author SCM Platform Team
 */
@Data
public class OrderStatusUpdateRequest {

    /** 目标状态 */
    @NotNull(message = "目标状态不能为空")
    private OrderStatus targetStatus;

    /** 操作原因（可选，便于审计） */
    private String reason;
}

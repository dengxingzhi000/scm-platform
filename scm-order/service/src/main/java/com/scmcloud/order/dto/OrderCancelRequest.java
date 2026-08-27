package com.scmcloud.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.UUID;

/**
 * 订单取消请求。
 *
 * <p>PUT /api/v1/orders/{id}/cancel 的请求体。</p>
 *
 * <p>仅 PENDING_PAYMENT / PAID 状态可取消，由领域方法
 * {@link com.scmcloud.order.domain.entity.OrdOrder#cancel(String)} 保证。</p>
 *
 * @author SCM Platform Team
 */
@Data
public class OrderCancelRequest {

    /** 取消原因 */
    @NotBlank(message = "取消原因不能为空")
    @Size(max = 500, message = "取消原因不能超过 500 字")
    private String reason;
}

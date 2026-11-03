package com.scmcloud.order.dto;

import com.scmcloud.order.domain.entity.PaymentStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 支付状态流转请求。
 *
 * @author SCM Platform Team
 */
@Data
public class PaymentStatusUpdateRequest {
    @NotNull(message = "目标状态不能为空")
    private PaymentStatus targetStatus;

    @Size(max = 256)
    private String reason;
}

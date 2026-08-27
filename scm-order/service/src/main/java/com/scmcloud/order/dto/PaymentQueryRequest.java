package com.scmcloud.order.dto;

import com.scmcloud.order.domain.entity.PaymentStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 支付记录分页查询请求。
 *
 * @author SCM Platform Team
 */
@Data
public class PaymentQueryRequest {
    private String paymentNo;
    private UUID orderId;
    private String userId;
    private PaymentStatus status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    @NotNull(message = "页码不能为空")
    @Min(value = 1, message = "页码必须 ≥ 1")
    private Integer pageNum;

    @NotNull(message = "每页大小不能为空")
    @Min(value = 1, message = "每页大小必须 ≥ 1")
    @Max(value = 200, message = "每页大小不能超过 200")
    private Integer pageSize;
}

package com.scmcloud.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * 创建退款单请求。
 *
 * @author SCM Platform Team
 */
@Data
public class RefundCreateRequest {
    @NotBlank(message = "退款单号不能为空")
    @Size(max = 128)
    private String refundNo;

    @NotNull(message = "订单 ID 不能为空")
    private UUID orderId;

    @NotBlank(message = "订单号不能为空")
    private String orderNo;

    @NotBlank(message = "用户 ID 不能为空")
    private String userId;

    @NotNull(message = "退款类型不能为空")
    private Integer refundType;

    @NotBlank(message = "退款原因不能为空")
    @Size(max = 256)
    private String reason;

    @Size(max = 1000)
    private String description;

    @NotNull(message = "退款金额不能为空")
    @Positive(message = "退款金额必须 > 0")
    private BigDecimal refundAmount;

    @NotEmpty(message = "退款明细不能为空")
    @Valid
    private List<RefundItemRequest> items;
}

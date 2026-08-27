package com.scmcloud.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * 创建支付记录请求。
 *
 * @author SCM Platform Team
 */
@Data
public class PaymentCreateRequest {
    @NotBlank(message = "支付单号不能为空")
    private String paymentNo;

    @NotNull(message = "订单 ID 不能为空")
    private UUID orderId;

    @NotBlank(message = "订单号不能为空")
    private String orderNo;

    @NotBlank(message = "用户 ID 不能为空")
    private String userId;

    @NotNull(message = "支付方式不能为空")
    private Integer paymentMethod;

    private String paymentChannel;

    @NotNull(message = "支付金额不能为空")
    @Positive(message = "支付金额必须 > 0")
    private BigDecimal paymentAmount;

    private String remark;
}

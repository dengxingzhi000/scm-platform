package com.scmcloud.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * 退款明细请求（创建退款时携带）。
 *
 * @author SCM Platform Team
 */
@Data
public class RefundItemRequest {
    @NotNull(message = "订单明细 ID 不能为空")
    private UUID orderItemId;

    @NotBlank(message = "SKU ID 不能为空")
    private String skuId;

    private String skuCode;

    @NotBlank(message = "SKU 名称不能为空")
    private String skuName;

    @NotNull(message = "退款件数不能为空")
    @Positive(message = "退款件数必须 > 0")
    private Integer quantity;

    @NotNull(message = "退款金额不能为空")
    @Positive(message = "退款金额必须 > 0")
    private BigDecimal refundAmount;

    private String remark;
}

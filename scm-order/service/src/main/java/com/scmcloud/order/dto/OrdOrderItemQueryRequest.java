package com.scmcloud.order.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

/**
 * 订单明细分页查询请求。
 *
 * @author SCM Platform Team
 */
@Data
public class OrdOrderItemQueryRequest {
    @NotNull(message = "订单 ID 不能为空")
    private UUID orderId;

    @NotNull(message = "页码不能为空")
    @Min(value = 1, message = "页码必须 ≥ 1")
    private Integer pageNum;

    @NotNull(message = "每页大小不能为空")
    @Min(value = 1, message = "每页大小必须 ≥ 1")
    @Max(value = 200, message = "每页大小不能超过 200")
    private Integer pageSize;
}

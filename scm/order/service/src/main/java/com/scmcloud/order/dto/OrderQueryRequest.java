package com.scmcloud.order.dto;

import com.scmcloud.order.domain.entity.OrderStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 订单分页查询请求。
 *
 * <p>POST /api/v1/orders/query 的请求体。</p>
 *
 * @author SCM Platform Team
 */
@Data
public class OrderQueryRequest {

    /** 订单号（精确匹配） */
    @Size(max = 64)
    private String orderNo;

    /** 用户 ID */
    @Size(max = 64)
    private String userId;

    /** 订单状态 */
    private OrderStatus status;

    /** 订单来源 */
    @Size(max = 32)
    private String orderSource;

    /** 创建时间起点 */
    private LocalDateTime startTime;

    /** 创建时间终点 */
    private LocalDateTime endTime;

    /** 页码，从 1 开始 */
    @NotNull(message = "页码不能为空")
    @Min(value = 1, message = "页码必须 ≥ 1")
    private Integer pageNum;

    /** 每页大小 */
    @NotNull(message = "每页大小不能为空")
    @Min(value = 1, message = "每页大小必须 ≥ 1")
    @Max(value = 200, message = "每页大小不能超过 200")
    private Integer pageSize;
}

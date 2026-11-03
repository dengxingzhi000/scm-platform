package com.scmcloud.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * 创建订单请求。
 *
 * <p>POST /api/v1/orders 的请求体。</p>
 *
 * @author SCM Platform Team
 */
@Data
public class OrderCreateRequest {

    /** 业务订单号（可选；为空时由系统生成） */
    @Size(max = 64)
    private String orderNo;

    /** 下单用户 ID */
    @NotBlank(message = "用户 ID 不能为空")
    private String userId;

    /** 下单用户名（冗余，便于查询） */
    private String username;

    /** 订单类型：1-普通 2-预售 3-拼团 4-秒杀 */
    private Integer orderType;

    /** 订单来源：WEB / APP / MINI_PROGRAM / ADMIN */
    @Size(max = 32)
    private String orderSource;

    /** 收货地址 */
    @NotBlank(message = "收货地址不能为空")
    private String shippingAddress;

    /** 支付方式：1-微信 2-支付宝 3-银联 */
    private Integer paymentMethod;

    /** 优惠金额（元，可选） */
    private BigDecimal discountAmount;

    /** 运费（元，可选） */
    private BigDecimal freightAmount;

    /** 买家留言 */
    @Size(max = 500)
    private String buyerMessage;

    /** 订单明细（至少 1 条） */
    @NotEmpty(message = "订单明细不能为空")
    @Valid
    private List<OrderItemRequest> items;
}

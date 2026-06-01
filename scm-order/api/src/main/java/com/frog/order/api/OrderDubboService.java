package com.frog.order.api;

import com.frog.order.api.dto.OrderVO;
import com.frog.order.api.request.CreateOrderRequest;

/**
 * 订单服务 Dubbo 接口
 *
 * @author SCM Platform Team
 * @since 2025-12-26
 */
public interface OrderDubboService {

    /**
     * 创建订单
     *
     * @param request 创建订单请求
     * @return 订单 VO
     */
    OrderVO createOrder(CreateOrderRequest request);

    /**
     * 查询订单
     *
     * @param orderNo 订单号
     * @return 订单 VO
     */
    OrderVO queryOrder(String orderNo);

    /**
     * 取消订单
     *
     * @param orderNo 订单号
     */
    void cancelOrder(String orderNo);
}

package com.scmcloud.order.api;

import com.scmcloud.order.api.dto.OrderVO;
import com.scmcloud.order.api.request.CreateOrderRequest;

/**
 * 璁㈠崟鏈嶅姟 Dubbo 鎺ュ彛
 *
 * @author SCM Platform Team
 * @since 2025-12-26
 */
public interface OrderDubboService {

    /**
     * 鍒涘缓璁㈠崟
     *
     * @param request 鍒涘缓璁㈠崟璇锋眰
     * @return 璁㈠崟 VO
     */
    OrderVO createOrder(CreateOrderRequest request);

    /**
     * 鏌ヨ璁㈠崟
     *
     * @param orderNo 璁㈠崟锟?
     * @return 璁㈠崟 VO
     */
    OrderVO queryOrder(String orderNo);

    /**
     * 鍙栨秷璁㈠崟
     *
     * @param orderNo 璁㈠崟锟?
     */
    void cancelOrder(String orderNo);
}

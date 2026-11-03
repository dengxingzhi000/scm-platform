package com.scmcloud.ordercenter.service.dubbo;

import com.scmcloud.ordercenter.api.OrderCenterDubboService;
import com.scmcloud.ordercenter.domain.entity.OcOrder;
import com.scmcloud.ordercenter.service.IOrderCenterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;

@Slf4j
@DubboService
@RequiredArgsConstructor
public class OrderCenterDubboServiceImpl implements OrderCenterDubboService {

    private final IOrderCenterService orderCenterService;

    @Override
    public String getOrderStatus(String orderNo) {
        OcOrder order = orderCenterService.getOrder(orderNo);
        return order == null || order.getStatus() == null ? null : String.valueOf(order.getStatus());
    }

    @Override
    public boolean cancelOrder(String orderNo, String reason) {
        orderCenterService.cancelOrder(orderNo, reason);
        return true;
    }
}

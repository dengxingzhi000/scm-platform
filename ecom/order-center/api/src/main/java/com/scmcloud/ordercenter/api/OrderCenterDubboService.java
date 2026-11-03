package com.scmcloud.ordercenter.api;

public interface OrderCenterDubboService {

    String getOrderStatus(String orderNo);

    boolean cancelOrder(String orderNo, String reason);
}

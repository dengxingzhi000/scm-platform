package com.cognora.ecom.ordercenter.api;

public interface OrderCenterDubboService {

    String getOrderStatus(String orderNo);

    boolean cancelOrder(String orderNo, String reason);
}

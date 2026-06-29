package com.scmcloud.ordercenter.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.scmcloud.ordercenter.domain.entity.OcOrder;
import com.scmcloud.ordercenter.domain.entity.OcOrderItem;

import java.util.List;

public interface IOrderCenterService extends IService<OcOrder> {

    OcOrder createOrder(OcOrder order, List<OcOrderItem> items);

    OcOrder getOrder(String orderNo);

    void cancelOrder(String orderNo, String reason);

    void payOrder(String orderNo, String paymentNo);

    void shipOrder(String orderNo, String logisticsNo);

    void deliverOrder(String orderNo);

    void confirmOrder(String orderNo);
}

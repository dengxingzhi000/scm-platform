package com.scmcloud.fulfillment.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.scmcloud.fulfillment.domain.entity.FulfillmentOrder;

public interface IFulfillmentOrderService extends IService<FulfillmentOrder> {

    FulfillmentOrder createFulfillment(String orderNo, String userId, String fulfillmentType);

    FulfillmentOrder getFulfillment(String fulfillmentNo);

    void cancelFulfillment(String fulfillmentNo, String reason);

    void pickItems(String fulfillmentNo);

    void packItems(String fulfillmentNo);

    void shipItems(String fulfillmentNo, String trackingNo, String carrier);

    void confirmDelivery(String fulfillmentNo);
}

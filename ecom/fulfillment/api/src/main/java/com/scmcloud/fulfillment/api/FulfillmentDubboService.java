package com.scmcloud.fulfillment.api;

import com.scmcloud.fulfillment.api.dto.TrackingDTO;

import java.util.List;

public interface FulfillmentDubboService {

    String createFulfillment(String orderNo, String userId, String fulfillmentType);

    String getFulfillmentStatus(String fulfillmentNo);

    void ship(String fulfillmentNo, String trackingNo, String carrier);

    List<TrackingDTO> getTracking(String fulfillmentNo);
}

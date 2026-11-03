package com.cognora.ecom.fulfillment.api;

import com.cognora.ecom.fulfillment.api.dto.TrackingDTO;

import java.util.List;

public interface FulfillmentDubboService {

    String createFulfillment(String orderNo, String userId, String fulfillmentType);

    String getFulfillmentStatus(String fulfillmentNo);

    void ship(String fulfillmentNo, String trackingNo, String carrier);

    List<TrackingDTO> getTracking(String fulfillmentNo);
}

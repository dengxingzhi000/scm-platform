package com.scmcloud.fulfillment.service.dubbo;

import com.scmcloud.fulfillment.api.FulfillmentDubboService;
import com.scmcloud.fulfillment.api.dto.TrackingDTO;
import com.scmcloud.fulfillment.domain.entity.FulfillmentOrder;
import com.scmcloud.fulfillment.domain.entity.Tracking;
import com.scmcloud.fulfillment.service.IFulfillmentOrderService;
import com.scmcloud.fulfillment.service.ITrackingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;

import java.util.List;

@Slf4j
@DubboService
@RequiredArgsConstructor
public class FulfillmentDubboServiceImpl implements FulfillmentDubboService {

    private final IFulfillmentOrderService fulfillmentOrderService;
    private final ITrackingService trackingService;

    @Override
    public String createFulfillment(String orderNo, String userId, String fulfillmentType) {
        FulfillmentOrder order = fulfillmentOrderService.createFulfillment(orderNo, userId, fulfillmentType);
        return order.getFulfillmentNo();
    }

    @Override
    public String getFulfillmentStatus(String fulfillmentNo) {
        FulfillmentOrder order = fulfillmentOrderService.getFulfillment(fulfillmentNo);
        return order == null || order.getStatus() == null ? null : String.valueOf(order.getStatus());
    }

    @Override
    public void ship(String fulfillmentNo, String trackingNo, String carrier) {
        fulfillmentOrderService.shipItems(fulfillmentNo, trackingNo, carrier);
    }

    @Override
    public List<TrackingDTO> getTracking(String fulfillmentNo) {
        return trackingService.getByFulfillmentNo(fulfillmentNo).stream()
                .map(this::toDTO)
                .toList();
    }

    private TrackingDTO toDTO(Tracking tracking) {
        TrackingDTO dto = new TrackingDTO();
        dto.setFulfillmentNo(tracking.getFulfillmentNo());
        dto.setTrackingNo(tracking.getTrackingNo());
        dto.setCarrier(tracking.getCarrier());
        dto.setStatus(tracking.getStatus());
        dto.setLocation(tracking.getLocation());
        dto.setDescription(tracking.getDescription());
        dto.setEventTime(tracking.getEventTime());
        return dto;
    }
}

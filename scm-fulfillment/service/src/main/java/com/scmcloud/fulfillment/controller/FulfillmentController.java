package com.scmcloud.fulfillment.controller;

import com.scmcloud.fulfillment.domain.entity.FulfillmentOrder;
import com.scmcloud.fulfillment.service.IFulfillmentOrderService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/api/v1/fulfillments")
public class FulfillmentController {

    private final IFulfillmentOrderService fulfillmentService;

    @PostMapping
    public FulfillmentOrder createFulfillment(@RequestBody CreateFulfillmentRequest request) {
        log.info("[API] Create fulfillment: orderNo={}", request.getOrderNo());
        return fulfillmentService.createFulfillment(request.getOrderNo(), request.getUserId(), request.getFulfillmentType());
    }

    @GetMapping("/{fulfillmentNo}")
    public FulfillmentOrder getFulfillment(@PathVariable String fulfillmentNo) {
        log.info("[API] Get fulfillment: fulfillmentNo={}", fulfillmentNo);
        return fulfillmentService.getFulfillment(fulfillmentNo);
    }

    @PostMapping("/{fulfillmentNo}/cancel")
    public boolean cancelFulfillment(@PathVariable String fulfillmentNo, @RequestBody CancelRequest request) {
        log.info("[API] Cancel fulfillment: fulfillmentNo={}, reason={}", fulfillmentNo, request.getReason());
        fulfillmentService.cancelFulfillment(fulfillmentNo, request.getReason());
        return true;
    }

    @PostMapping("/{fulfillmentNo}/pick")
    public boolean pickItems(@PathVariable String fulfillmentNo) {
        log.info("[API] Pick items: fulfillmentNo={}", fulfillmentNo);
        fulfillmentService.pickItems(fulfillmentNo);
        return true;
    }

    @PostMapping("/{fulfillmentNo}/pack")
    public boolean packItems(@PathVariable String fulfillmentNo) {
        log.info("[API] Pack items: fulfillmentNo={}", fulfillmentNo);
        fulfillmentService.packItems(fulfillmentNo);
        return true;
    }

    @PostMapping("/{fulfillmentNo}/ship")
    public boolean shipItems(@PathVariable String fulfillmentNo, @RequestBody ShipRequest request) {
        log.info("[API] Ship items: fulfillmentNo={}, trackingNo={}, carrier={}", fulfillmentNo, request.getTrackingNo(), request.getCarrier());
        fulfillmentService.shipItems(fulfillmentNo, request.getTrackingNo(), request.getCarrier());
        return true;
    }

    @PostMapping("/{fulfillmentNo}/deliver")
    public boolean confirmDelivery(@PathVariable String fulfillmentNo) {
        log.info("[API] Confirm delivery: fulfillmentNo={}", fulfillmentNo);
        fulfillmentService.confirmDelivery(fulfillmentNo);
        return true;
    }

    @Data
    public static class CreateFulfillmentRequest {
        private String orderNo;
        private String userId;
        private String fulfillmentType;
    }

    @Data
    public static class CancelRequest {
        private String reason;
    }

    @Data
    public static class ShipRequest {
        private String trackingNo;
        private String carrier;
    }
}

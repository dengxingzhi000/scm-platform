package com.scmcloud.fulfillment.controller;

import com.scmcloud.fulfillment.domain.entity.Tracking;
import com.scmcloud.fulfillment.service.ITrackingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/api/v1/fulfillments")
public class TrackingController {

    private final ITrackingService trackingService;

    @PostMapping("/tracking")
    public Tracking recordEvent(@RequestBody Tracking tracking) {
        log.info("[API] Record tracking event: fulfillmentNo={}, status={}",
                tracking.getFulfillmentNo(), tracking.getStatus());
        return trackingService.recordEvent(tracking);
    }

    @GetMapping("/tracking/{trackingNo}")
    public List<Tracking> getByTrackingNo(@PathVariable String trackingNo) {
        return trackingService.getByTrackingNo(trackingNo);
    }

    @GetMapping("/{fulfillmentNo}/tracking")
    public List<Tracking> getByFulfillmentNo(@PathVariable String fulfillmentNo) {
        return trackingService.getByFulfillmentNo(fulfillmentNo);
    }

    @GetMapping("/{fulfillmentNo}/tracking/latest")
    public Tracking getLatest(@PathVariable String fulfillmentNo) {
        return trackingService.getLatest(fulfillmentNo);
    }
}

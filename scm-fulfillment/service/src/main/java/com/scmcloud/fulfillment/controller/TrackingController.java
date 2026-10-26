package com.scmcloud.fulfillment.controller;

import com.scmcloud.fulfillment.domain.entity.Tracking;
import com.scmcloud.fulfillment.service.ITrackingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "履约跟踪", description = "履约轨迹事件的登记与按物流单号、履约单号查询跟踪记录的接口。")
public class TrackingController {

    private final ITrackingService trackingService;

    @Operation(summary = "记录跟踪事件", description = "登记一条履约轨迹事件，用于形成物流跟踪记录。")
    @PostMapping("/tracking")
    public Tracking recordEvent(@RequestBody Tracking tracking) {
        log.info("[API] Record tracking event: fulfillmentNo={}, status={}",
                tracking.getFulfillmentNo(), tracking.getStatus());
        return trackingService.recordEvent(tracking);
    }

    @Operation(summary = "按物流单号查询跟踪", description = "根据物流运单号查询其下的全部履约轨迹记录。")
    @GetMapping("/tracking/{trackingNo}")
    public List<Tracking> getByTrackingNo(@Parameter(description = "物流运单号") @PathVariable String trackingNo) {
        return trackingService.getByTrackingNo(trackingNo);
    }

    @Operation(summary = "按履约单号查询跟踪", description = "根据履约单号查询该履约单的全部轨迹记录。")
    @GetMapping("/{fulfillmentNo}/tracking")
    public List<Tracking> getByFulfillmentNo(@Parameter(description = "履约单号") @PathVariable String fulfillmentNo) {
        return trackingService.getByFulfillmentNo(fulfillmentNo);
    }

    @Operation(summary = "查询最新跟踪记录", description = "查询指定履约单最新的一条轨迹记录。")
    @GetMapping("/{fulfillmentNo}/tracking/latest")
    public Tracking getLatest(@Parameter(description = "履约单号") @PathVariable String fulfillmentNo) {
        return trackingService.getLatest(fulfillmentNo);
    }
}

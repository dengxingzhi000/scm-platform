package com.scmcloud.fulfillment.controller;

import com.scmcloud.fulfillment.domain.entity.FulfillmentOrder;
import com.scmcloud.fulfillment.service.IFulfillmentOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/api/v1/fulfillments")
@Tag(name = "履约管理", description = "履约单的创建、查询以及取消、拣货、打包、发货、签收等状态流转接口。")
public class FulfillmentController {

    private final IFulfillmentOrderService fulfillmentService;

    @Operation(summary = "创建履约单", description = "根据订单号与履约类型创建一张新的履约单。")
    @PostMapping
    public FulfillmentOrder createFulfillment(@RequestBody CreateFulfillmentRequest request) {
        log.info("[API] Create fulfillment: orderNo={}", request.getOrderNo());
        return fulfillmentService.createFulfillment(request.getOrderNo(), request.getUserId(), request.getFulfillmentType());
    }

    @Operation(summary = "查询履约单", description = "根据履约单号查询履约单的明细与当前状态。")
    @GetMapping("/{fulfillmentNo}")
    public FulfillmentOrder getFulfillment(@Parameter(description = "履约单号") @PathVariable String fulfillmentNo) {
        log.info("[API] Get fulfillment: fulfillmentNo={}", fulfillmentNo);
        return fulfillmentService.getFulfillment(fulfillmentNo);
    }

    @Operation(summary = "取消履约单", description = "根据履约单号取消履约单，并记录取消原因。")
    @PostMapping("/{fulfillmentNo}/cancel")
    public boolean cancelFulfillment(@Parameter(description = "履约单号") @PathVariable String fulfillmentNo, @RequestBody CancelRequest request) {
        log.info("[API] Cancel fulfillment: fulfillmentNo={}, reason={}", fulfillmentNo, request.getReason());
        fulfillmentService.cancelFulfillment(fulfillmentNo, request.getReason());
        return true;
    }

    @Operation(summary = "拣货", description = "对指定履约单执行拣货操作，将履约单推进至拣货完成状态。")
    @PostMapping("/{fulfillmentNo}/pick")
    public boolean pickItems(@Parameter(description = "履约单号") @PathVariable String fulfillmentNo) {
        log.info("[API] Pick items: fulfillmentNo={}", fulfillmentNo);
        fulfillmentService.pickItems(fulfillmentNo);
        return true;
    }

    @Operation(summary = "打包", description = "对指定履约单执行打包操作，将履约单推进至打包完成状态。")
    @PostMapping("/{fulfillmentNo}/pack")
    public boolean packItems(@Parameter(description = "履约单号") @PathVariable String fulfillmentNo) {
        log.info("[API] Pack items: fulfillmentNo={}", fulfillmentNo);
        fulfillmentService.packItems(fulfillmentNo);
        return true;
    }

    @Operation(summary = "发货", description = "登记物流单号与承运商，对指定履约单执行发货操作。")
    @PostMapping("/{fulfillmentNo}/ship")
    public boolean shipItems(@Parameter(description = "履约单号") @PathVariable String fulfillmentNo, @RequestBody ShipRequest request) {
        log.info("[API] Ship items: fulfillmentNo={}, trackingNo={}, carrier={}", fulfillmentNo, request.getTrackingNo(), request.getCarrier());
        fulfillmentService.shipItems(fulfillmentNo, request.getTrackingNo(), request.getCarrier());
        return true;
    }

    @Operation(summary = "确认签收", description = "对指定履约单确认收货签收，将履约单推进至已完成状态。")
    @PostMapping("/{fulfillmentNo}/deliver")
    public boolean confirmDelivery(@Parameter(description = "履约单号") @PathVariable String fulfillmentNo) {
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

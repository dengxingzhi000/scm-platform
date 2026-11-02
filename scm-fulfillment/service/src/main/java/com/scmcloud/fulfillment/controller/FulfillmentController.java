package com.scmcloud.fulfillment.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.fulfillment.domain.entity.FulfillmentOrder;
import com.scmcloud.fulfillment.service.IFulfillmentOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/fulfillments")
@Tag(name = "履约管理", description = "履约单的创建、分页查询以及取消、拣货、打包、发货、签收等状态流转接口。")
public class FulfillmentController {

    private final IFulfillmentOrderService fulfillmentService;

    @Operation(summary = "分页查询履约单", description = "按订单号、用户与状态分页查询履约单。")
    @GetMapping("/page")
    public Page<FulfillmentOrder> page(
            @Parameter(description = "页码，默认 1") @RequestParam(defaultValue = "1") @Min(1) int page,
            @Parameter(description = "每页数量，默认 20，最大 100") @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @Parameter(description = "订单号（模糊）") @RequestParam(required = false) String orderNo,
            @Parameter(description = "用户 ID") @RequestParam(required = false) String userId,
            @Parameter(description = "履约状态") @RequestParam(required = false) Integer status) {
        return fulfillmentService.pageQuery(page, size, orderNo, userId, status);
    }

    @Operation(summary = "创建履约单", description = "根据订单号与履约类型创建一张新的履约单。")
    @PostMapping
    public FulfillmentOrder createFulfillment(@Valid @RequestBody CreateFulfillmentRequest request) {
        log.info("[API] Create fulfillment: orderNo={}", (request.getOrderNo() + "").replace("\r", "_").replace("\n", "_"));
        return fulfillmentService.createFulfillment(request.getOrderNo(), request.getUserId(), request.getFulfillmentType());
    }

    @Operation(summary = "查询履约单", description = "根据履约单号查询履约单的明细与当前状态。")
    @GetMapping("/{fulfillmentNo}")
    public FulfillmentOrder getFulfillment(@Parameter(description = "履约单号") @PathVariable String fulfillmentNo) {
        log.info("[API] Get fulfillment: fulfillmentNo={}", (fulfillmentNo + "").replace("\r", "_").replace("\n", "_"));
        return fulfillmentService.getFulfillment(fulfillmentNo);
    }

    @Operation(summary = "取消履约单", description = "根据履约单号取消履约单，并记录取消原因。")
    @PostMapping("/{fulfillmentNo}/cancel")
    public boolean cancelFulfillment(@Parameter(description = "履约单号") @PathVariable String fulfillmentNo, @Valid @RequestBody CancelRequest request) {
        log.info("[API] Cancel fulfillment: fulfillmentNo={}, reason={}", (fulfillmentNo + "").replace("\r", "_").replace("\n", "_"), (request.getReason() + "").replace("\r", "_").replace("\n", "_"));
        fulfillmentService.cancelFulfillment(fulfillmentNo, request.getReason());
        return true;
    }

    @Operation(summary = "拣货", description = "对指定履约单执行拣货操作，将履约单推进至拣货完成状态。")
    @PostMapping("/{fulfillmentNo}/pick")
    public boolean pickItems(@Parameter(description = "履约单号") @PathVariable String fulfillmentNo) {
        log.info("[API] Pick items: fulfillmentNo={}", (fulfillmentNo + "").replace("\r", "_").replace("\n", "_"));
        fulfillmentService.pickItems(fulfillmentNo);
        return true;
    }

    @Operation(summary = "打包", description = "对指定履约单执行打包操作，将履约单推进至打包完成状态。")
    @PostMapping("/{fulfillmentNo}/pack")
    public boolean packItems(@Parameter(description = "履约单号") @PathVariable String fulfillmentNo) {
        log.info("[API] Pack items: fulfillmentNo={}", (fulfillmentNo + "").replace("\r", "_").replace("\n", "_"));
        fulfillmentService.packItems(fulfillmentNo);
        return true;
    }

    @Operation(summary = "发货", description = "登记物流单号与承运商，对指定履约单执行发货操作。")
    @PostMapping("/{fulfillmentNo}/ship")
    public boolean shipItems(@Parameter(description = "履约单号") @PathVariable String fulfillmentNo, @Valid @RequestBody ShipRequest request) {
        log.info("[API] Ship items: fulfillmentNo={}, trackingNo={}, carrier={}", (fulfillmentNo + "").replace("\r", "_").replace("\n", "_"), (request.getTrackingNo() + "").replace("\r", "_").replace("\n", "_"), (request.getCarrier() + "").replace("\r", "_").replace("\n", "_"));
        fulfillmentService.shipItems(fulfillmentNo, request.getTrackingNo(), request.getCarrier());
        return true;
    }

    @Operation(summary = "确认签收", description = "对指定履约单确认收货签收，将履约单推进至已完成状态。")
    @PostMapping("/{fulfillmentNo}/deliver")
    public boolean confirmDelivery(@Parameter(description = "履约单号") @PathVariable String fulfillmentNo) {
        log.info("[API] Confirm delivery: fulfillmentNo={}", (fulfillmentNo + "").replace("\r", "_").replace("\n", "_"));
        fulfillmentService.confirmDelivery(fulfillmentNo);
        return true;
    }

    @Data
    public static class CreateFulfillmentRequest {
        @NotBlank(message = "订单号不能为空")
        private String orderNo;

        @NotBlank(message = "用户 ID 不能为空")
        private String userId;

        @NotBlank(message = "履约类型不能为空")
        private String fulfillmentType;
    }

    @Data
    public static class CancelRequest {
        @Size(max = 200, message = "取消原因不能超过 200 个字符")
        private String reason;
    }

    @Data
    public static class ShipRequest {
        @NotBlank(message = "物流单号不能为空")
        private String trackingNo;

        @NotBlank(message = "承运商不能为空")
        private String carrier;
    }
}

package com.scmcloud.ordercenter.controller;

import com.scmcloud.ordercenter.domain.entity.OcOrder;
import com.scmcloud.ordercenter.domain.entity.OcOrderItem;
import com.scmcloud.ordercenter.service.IOrderCenterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "订单中心", description = "提供订单创建、查询、取消、支付、发货、送达与确认收货接口。")
public class OrderCenterController {

    private final IOrderCenterService orderService;

    @Operation(summary = "创建订单", description = "根据订单主体与订单明细创建新订单并返回订单信息。")
    @PostMapping
    public OcOrder createOrder(@RequestBody CreateOrderRequest request) {
        log.info("[API] Create order: orderNo={}", request.getOrder().getOrderNo());
        return orderService.createOrder(request.getOrder(), request.getItems());
    }

    @Operation(summary = "查询订单详情", description = "根据订单号查询订单中心订单详情。")
    @GetMapping("/{orderNo}")
    public OcOrder getOrder(@Parameter(description = "订单号") @PathVariable String orderNo) {
        log.info("[API] Get order: orderNo={}", orderNo);
        return orderService.getOrder(orderNo);
    }

    @Operation(summary = "取消订单", description = "按订单号取消订单并记录取消原因。")
    @PostMapping("/{orderNo}/cancel")
    public boolean cancelOrder(@Parameter(description = "订单号") @PathVariable String orderNo, @RequestBody CancelRequest request) {
        log.info("[API] Cancel order: orderNo={}, reason={}", orderNo, request.getReason());
        orderService.cancelOrder(orderNo, request.getReason());
        return true;
    }

    @Operation(summary = "订单支付", description = "按订单号记录支付结果并完成订单支付状态流转。")
    @PostMapping("/{orderNo}/pay")
    public boolean payOrder(@Parameter(description = "订单号") @PathVariable String orderNo, @RequestBody PayRequest request) {
        log.info("[API] Pay order: orderNo={}, paymentNo={}", orderNo, request.getPaymentNo());
        orderService.payOrder(orderNo, request.getPaymentNo());
        return true;
    }

    @Operation(summary = "订单发货", description = "按订单号执行发货并绑定物流单号。")
    @PostMapping("/{orderNo}/ship")
    public boolean shipOrder(@Parameter(description = "订单号") @PathVariable String orderNo, @RequestBody ShipRequest request) {
        log.info("[API] Ship order: orderNo={}, logisticsNo={}", orderNo, request.getLogisticsNo());
        orderService.shipOrder(orderNo, request.getLogisticsNo());
        return true;
    }

    @Operation(summary = "订单送达", description = "按订单号将订单状态流转为已送达。")
    @PostMapping("/{orderNo}/deliver")
    public boolean deliverOrder(@Parameter(description = "订单号") @PathVariable String orderNo) {
        log.info("[API] Deliver order: orderNo={}", orderNo);
        orderService.deliverOrder(orderNo);
        return true;
    }

    @Operation(summary = "确认收货", description = "按订单号确认收货并完成订单状态流转。")
    @PostMapping("/{orderNo}/confirm")
    public boolean confirmOrder(@Parameter(description = "订单号") @PathVariable String orderNo) {
        log.info("[API] Confirm order: orderNo={}", orderNo);
        orderService.confirmOrder(orderNo);
        return true;
    }

    @Data
    public static class CreateOrderRequest {
        private OcOrder order;
        private List<OcOrderItem> items;
    }

    @Data
    public static class CancelRequest {
        private String reason;
    }

    @Data
    public static class PayRequest {
        private String paymentNo;
    }

    @Data
    public static class ShipRequest {
        private String logisticsNo;
    }
}

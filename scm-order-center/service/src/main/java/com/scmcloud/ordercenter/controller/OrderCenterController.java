package com.scmcloud.ordercenter.controller;

import com.scmcloud.ordercenter.domain.entity.OcOrder;
import com.scmcloud.ordercenter.domain.entity.OcOrderItem;
import com.scmcloud.ordercenter.service.IOrderCenterService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/api/v1/orders")
public class OrderCenterController {

    private final IOrderCenterService orderService;

    @PostMapping
    public OcOrder createOrder(@RequestBody CreateOrderRequest request) {
        log.info("[API] Create order: orderNo={}", request.getOrder().getOrderNo());
        return orderService.createOrder(request.getOrder(), request.getItems());
    }

    @GetMapping("/{orderNo}")
    public OcOrder getOrder(@PathVariable String orderNo) {
        log.info("[API] Get order: orderNo={}", orderNo);
        return orderService.getOrder(orderNo);
    }

    @PostMapping("/{orderNo}/cancel")
    public boolean cancelOrder(@PathVariable String orderNo, @RequestBody CancelRequest request) {
        log.info("[API] Cancel order: orderNo={}, reason={}", orderNo, request.getReason());
        orderService.cancelOrder(orderNo, request.getReason());
        return true;
    }

    @PostMapping("/{orderNo}/pay")
    public boolean payOrder(@PathVariable String orderNo, @RequestBody PayRequest request) {
        log.info("[API] Pay order: orderNo={}, paymentNo={}", orderNo, request.getPaymentNo());
        orderService.payOrder(orderNo, request.getPaymentNo());
        return true;
    }

    @PostMapping("/{orderNo}/ship")
    public boolean shipOrder(@PathVariable String orderNo, @RequestBody ShipRequest request) {
        log.info("[API] Ship order: orderNo={}, logisticsNo={}", orderNo, request.getLogisticsNo());
        orderService.shipOrder(orderNo, request.getLogisticsNo());
        return true;
    }

    @PostMapping("/{orderNo}/deliver")
    public boolean deliverOrder(@PathVariable String orderNo) {
        log.info("[API] Deliver order: orderNo={}", orderNo);
        orderService.deliverOrder(orderNo);
        return true;
    }

    @PostMapping("/{orderNo}/confirm")
    public boolean confirmOrder(@PathVariable String orderNo) {
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

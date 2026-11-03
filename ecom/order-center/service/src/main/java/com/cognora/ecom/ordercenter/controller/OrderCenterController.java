package com.cognora.ecom.ordercenter.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cognora.ecom.ordercenter.domain.entity.OcOrder;
import com.cognora.ecom.ordercenter.domain.entity.OcOrderItem;
import com.cognora.ecom.ordercenter.service.IOrderCenterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "订单中心", description = "提供订单创建、分页查询、取消、支付、发货、送达与确认收货接口。")
public class OrderCenterController {

    private final IOrderCenterService orderService;

    @Operation(summary = "分页查询订单", description = "按订单号、用户与状态分页查询订单中心订单。")
    @GetMapping("/page")
    public Page<OcOrder> page(
            @Parameter(description = "页码，默认 1") @RequestParam(defaultValue = "1") @Min(1) int page,
            @Parameter(description = "每页数量，默认 20，最大 100") @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @Parameter(description = "订单号（模糊）") @RequestParam(required = false) String orderNo,
            @Parameter(description = "用户 ID") @RequestParam(required = false) String userId,
            @Parameter(description = "订单状态") @RequestParam(required = false) Integer status) {
        return orderService.pageQuery(page, size, orderNo, userId, status);
    }

    @Operation(summary = "创建订单", description = "根据订单主体与订单明细创建新订单并返回订单信息。")
    @PostMapping
    public OcOrder createOrder(@Valid @RequestBody CreateOrderRequest request) {
        log.info("[API] Create order: orderNo={}", (request.getOrder().getOrderNo() + "").replace("\r", "_").replace("\n", "_"));
        return orderService.createOrder(request.getOrder(), request.getItems());
    }

    @Operation(summary = "查询订单详情", description = "根据订单号查询订单中心订单详情。")
    @GetMapping("/{orderNo}")
    public OcOrder getOrder(@Parameter(description = "订单号") @PathVariable String orderNo) {
        log.info("[API] Get order: orderNo={}", (orderNo + "").replace("\r", "_").replace("\n", "_"));
        return orderService.getOrder(orderNo);
    }

    @Operation(summary = "取消订单", description = "按订单号取消订单并记录取消原因。")
    @PostMapping("/{orderNo}/cancel")
    public boolean cancelOrder(@Parameter(description = "订单号") @PathVariable String orderNo, @Valid @RequestBody CancelRequest request) {
        log.info("[API] Cancel order: orderNo={}, reason={}", (orderNo + "").replace("\r", "_").replace("\n", "_"), (request.getReason() + "").replace("\r", "_").replace("\n", "_"));
        orderService.cancelOrder(orderNo, request.getReason());
        return true;
    }

    @Operation(summary = "订单支付", description = "按订单号记录支付结果并完成订单支付状态流转。")
    @PostMapping("/{orderNo}/pay")
    public boolean payOrder(@Parameter(description = "订单号") @PathVariable String orderNo, @Valid @RequestBody PayRequest request) {
        log.info("[API] Pay order: orderNo={}, paymentNo={}", (orderNo + "").replace("\r", "_").replace("\n", "_"), (request.getPaymentNo() + "").replace("\r", "_").replace("\n", "_"));
        orderService.payOrder(orderNo, request.getPaymentNo());
        return true;
    }

    @Operation(summary = "订单发货", description = "按订单号执行发货并绑定物流单号。")
    @PostMapping("/{orderNo}/ship")
    public boolean shipOrder(@Parameter(description = "订单号") @PathVariable String orderNo, @Valid @RequestBody ShipRequest request) {
        log.info("[API] Ship order: orderNo={}, logisticsNo={}", (orderNo + "").replace("\r", "_").replace("\n", "_"), (request.getLogisticsNo() + "").replace("\r", "_").replace("\n", "_"));
        orderService.shipOrder(orderNo, request.getLogisticsNo());
        return true;
    }

    @Operation(summary = "订单送达", description = "按订单号将订单状态流转为已送达。")
    @PostMapping("/{orderNo}/deliver")
    public boolean deliverOrder(@Parameter(description = "订单号") @PathVariable String orderNo) {
        log.info("[API] Deliver order: orderNo={}", (orderNo + "").replace("\r", "_").replace("\n", "_"));
        orderService.deliverOrder(orderNo);
        return true;
    }

    @Operation(summary = "确认收货", description = "按订单号确认收货并完成订单状态流转。")
    @PostMapping("/{orderNo}/confirm")
    public boolean confirmOrder(@Parameter(description = "订单号") @PathVariable String orderNo) {
        log.info("[API] Confirm order: orderNo={}", (orderNo + "").replace("\r", "_").replace("\n", "_"));
        orderService.confirmOrder(orderNo);
        return true;
    }

    @Data
    public static class CreateOrderRequest {
        @NotNull(message = "订单不能为空")
        @Valid
        private OcOrder order;

        @NotEmpty(message = "订单明细不能为空")
        @Valid
        private List<OcOrderItem> items;
    }

    @Data
    public static class CancelRequest {
        @Size(max = 200, message = "取消原因不能超过 200 个字符")
        private String reason;
    }

    @Data
    public static class PayRequest {
        @NotBlank(message = "支付单号不能为空")
        private String paymentNo;
    }

    @Data
    public static class ShipRequest {
        @NotBlank(message = "物流单号不能为空")
        private String logisticsNo;
    }
}

package com.scmcloud.order.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.common.response.ApiResponse;
import com.scmcloud.order.domain.entity.OrdOrderItem;
import com.scmcloud.order.dto.OrderConverter;
import com.scmcloud.order.dto.OrderItemResponse;
import com.scmcloud.order.dto.OrdOrderItemQueryRequest;
import com.scmcloud.order.service.query.OrdOrderItemQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * 订单明细 controller（<b>只读</b>）。
 *
 * <p>订单明细<b>不</b>提供独立 CRUD：</p>
 * <ul>
 *   <li>创建 → {@link OrdOrderController#createOrder} 在创建订单时通过 {@code items} 携带写入</li>
 *   <li>更新 / 删除 → 不支持（明细随订单生命周期管理）</li>
 * </ul>
 *
 * @author SCM Platform Team
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/order-items")
@RequiredArgsConstructor
public class OrdOrderItemController {

    private final OrdOrderItemQueryService queryService;

    @GetMapping("/{id}")
    public ApiResponse<OrderItemResponse> getById(@PathVariable UUID id) {
        log.info("[API] Query order item: id={}", id);
        OrdOrderItem item = queryService.getById(id.toString());
        return ApiResponse.success(OrderConverter.toItemResponse(item));
    }

    /**
     * 按订单 ID 分页查询明细。
     */
    @PostMapping("/query")
    public ApiResponse<Page<OrderItemResponse>> queryByOrderId(@RequestBody @Valid OrdOrderItemQueryRequest request) {
        log.info("[API] Query order items: orderId={}, page={}/{}",
                request.getOrderId(), request.getPageNum(), request.getPageSize());

        Page<OrdOrderItem> page = queryService.pageByOrderId(
                new Page<>(request.getPageNum(), request.getPageSize()),
                request.getOrderId());
        Page<OrderItemResponse> mapped = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        java.util.List<OrderItemResponse> items = new java.util.ArrayList<>();
        for (OrdOrderItem item : page.getRecords()) {
            items.add(OrderConverter.toItemResponse(item));
        }
        mapped.setRecords(items);
        return ApiResponse.success(mapped);
    }
}

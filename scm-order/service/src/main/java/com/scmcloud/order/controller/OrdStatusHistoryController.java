package com.scmcloud.order.controller;

import com.scmcloud.common.response.ApiResponse;
import com.scmcloud.order.dto.OrderConverter;
import com.scmcloud.order.dto.StatusHistoryResponse;
import com.scmcloud.order.service.query.OrdStatusHistoryQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * 订单状态历史 controller（<b>只读</b>）。
 *
 * <p>状态历史是 append-only 审计日志，由 {@code OrdOrderCommandService} 在状态流转同一事务内写入。
 * 本 controller <b>不暴露任何写入端点</b>——POST / DELETE 已被删除（违反审计原则）。</p>
 *
 * @author SCM Platform Team
 */
@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/api/v1/status-history")
@Tag(name = "订单状态历史", description = "订单状态变更历史的只读查询接口。")
public class OrdStatusHistoryController {

    private final OrdStatusHistoryQueryService queryService;

    @Operation(summary = "查询状态历史", description = "根据状态历史 ID 查询单条状态变更记录。")
    @GetMapping("/{id}")
    public ApiResponse<StatusHistoryResponse> getById(@Parameter(description = "状态历史 ID") @PathVariable UUID id) {
        log.info("[API] Query status history: id={}", id);
        return ApiResponse.success(
                OrderConverter.toStatusHistoryResponse(queryService.getById(id.toString())));
    }

    @Operation(summary = "按订单查询状态历史", description = "根据订单 ID 查询该订单的全部状态变更记录。")
    @GetMapping("/order/{orderId}")
    public ApiResponse<List<StatusHistoryResponse>> listByOrderId(@Parameter(description = "订单 ID") @PathVariable UUID orderId) {
        log.info("[API] Query order status history: orderId={}", orderId);
        return ApiResponse.success(
                OrderConverter.toStatusHistoryResponseList(queryService.listByOrderId(orderId)));
    }
}

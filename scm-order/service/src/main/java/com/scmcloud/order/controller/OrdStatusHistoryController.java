package com.scmcloud.order.controller;

import com.scmcloud.common.response.ApiResponse;
import com.scmcloud.order.dto.OrderConverter;
import com.scmcloud.order.dto.StatusHistoryResponse;
import com.scmcloud.order.service.query.OrdStatusHistoryQueryService;
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
public class OrdStatusHistoryController {

    private final OrdStatusHistoryQueryService queryService;

    @GetMapping("/{id}")
    public ApiResponse<StatusHistoryResponse> getById(@PathVariable UUID id) {
        log.info("[API] Query status history: id={}", id);
        return ApiResponse.success(
                OrderConverter.toStatusHistoryResponse(queryService.getById(id.toString())));
    }

    @GetMapping("/order/{orderId}")
    public ApiResponse<List<StatusHistoryResponse>> listByOrderId(@PathVariable UUID orderId) {
        log.info("[API] Query order status history: orderId={}", orderId);
        return ApiResponse.success(
                OrderConverter.toStatusHistoryResponseList(queryService.listByOrderId(orderId)));
    }
}

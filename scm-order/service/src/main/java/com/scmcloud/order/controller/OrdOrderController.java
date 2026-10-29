package com.scmcloud.order.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.common.response.ApiResponse;
import com.scmcloud.order.domain.entity.OrderStatus;
import com.scmcloud.order.domain.entity.OrdOrder;
import com.scmcloud.order.domain.entity.OrdOrderItem;
import com.scmcloud.order.dto.OrderCancelRequest;
import com.scmcloud.order.dto.OrderConverter;
import com.scmcloud.order.dto.OrderCreateRequest;
import com.scmcloud.order.dto.OrderItemResponse;
import com.scmcloud.order.dto.OrderQueryRequest;
import com.scmcloud.order.dto.OrderResponse;
import com.scmcloud.order.dto.OrderStatusUpdateRequest;
import com.scmcloud.order.service.command.OrdOrderCommandService;
import com.scmcloud.order.service.query.OrdOrderQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 订单管理控制器。
 *
 * <p>按 CQRS 拆分职责：</p>
 * <ul>
 *   <li>读操作 → {@link OrdOrderQueryService}（自动 {@code @Slave} 路由到只读副本）</li>
 *   <li>写操作 → {@link OrdOrderCommandService}（自动 {@code @Master} + 事务 + 状态机校验 + 事件溯源）</li>
 * </ul>
 *
 * <p>所有响应统一包装为 {@link ApiResponse}；领域实体不在响应中直接暴露。</p>
 *
 * <p>相比旧版本，删除/替换的不安全端点：</p>
 * <ul>
 *   <li>删除 {@code GET /} 无分页全表查询（生产危险）</li>
 *   <li>删除 {@code PUT /} 任意字段更新（绕过领域校验/状态机）</li>
 *   <li>删除 {@code DELETE /{id}} 物理删除（改用 {@code PUT /{id}/cancel} 走领域取消）</li>
 *   <li>{@code GET /page} 和 {@code GET /user/{userId}/page} 合并为 {@code POST /query} 统一条件查询</li>
 * </ul>
 *
 * @author SCM Platform Team
 */
@Validated
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "订单管理", description = "订单的创建、查询、状态流转与取消接口。")
public class OrdOrderController {
    private final OrdOrderQueryService queryService;
    private final OrdOrderCommandService commandService;

    // ───────────────────────── Query Side ─────────────────────────

    /**
     * 按 ID 查询订单。
     */
    @Operation(summary = "查询订单", description = "根据订单 ID 查询订单详情。")
    @GetMapping("/{id}")
    public ApiResponse<OrderResponse> getById(@Parameter(description = "订单 ID") @PathVariable UUID id) {
        log.info("[API] 查询订单: id={}", id);
        OrdOrder order = queryService.getById(String.valueOf(id));
        return ApiResponse.success(OrderConverter.toResponse(order));
    }

    /**
     * 分页查询订单（统一条件查询入口）。
     *
     * <p>支持按 orderNo / userId / status / orderSource / 时间区间 过滤。
     * 用 POST + body 而非 GET + queryString，便于复杂过滤条件扩展。</p>
     */
    @Operation(summary = "分页查询订单", description = "按条件分页查询订单，支持订单号、用户、状态、来源及时间区间过滤。")
    @PostMapping("/query")
    public ApiResponse<Page<OrderResponse>> query(@RequestBody @Valid OrderQueryRequest request) {
        log.info("[API] 分页查询订单: {}", (request + "").replace("\r", "_").replace("\n", "_"));

        Page<OrdOrder> result = queryService.query(request);
        Page<OrderResponse> mapped = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        mapped.setRecords(OrderConverter.toResponseList(result.getRecords()));
        return ApiResponse.success(mapped);
    }

    /**
     * 查询订单明细（按订单 ID）。
     */
    @Operation(summary = "查询订单明细", description = "根据订单 ID 查询该订单下的全部明细项。")
    @GetMapping("/{id}/items")
    public ApiResponse<List<OrderItemResponse>> getItems(@Parameter(description = "订单 ID") @PathVariable UUID id) {
        log.info("[API] 查询订单明细: id={}", id);
        List<OrdOrderItem> items = queryService.listItemsByOrderId(id);
        return ApiResponse.success(items.stream()
                .map(OrderConverter::toItemResponse)
                .collect(Collectors.toList()));
    }

    // ───────────────────────── Command Side ─────────────────────────

    /**
     * 创建订单。
     */
    @Operation(summary = "创建订单", description = "创建新订单并同时写入订单明细。")
    @PostMapping
    public ApiResponse<OrderResponse> createOrder(@RequestBody @Valid OrderCreateRequest request) {
        log.info("[API] 创建订单: orderNo={}, userId={}", (request.getOrderNo() + "").replace("\r", "_").replace("\n", "_"), (request.getUserId() + "").replace("\r", "_").replace("\n", "_"));

        OrderConverter.OrderDraft draft = OrderConverter.toOrderEntity(request);
        OrdOrder created = commandService.createOrder(draft.order(), draft.items());
        return ApiResponse.success(OrderConverter.toResponse(created));
    }

    /**
     * 订单状态流转。
     *
     * <p>状态机校验由 {@link OrdOrderCommandService} 通过 Dubbo 状态机服务完成；
     * 非法流转会被拒绝并写入 {@code ord_status_history}。</p>
     */
    @Operation(summary = "订单状态流转", description = "按状态机规则更新订单状态，非法流转将被拒绝。")
    @PutMapping("/{id}/status")
    public ApiResponse<OrderResponse> updateStatus(@Parameter(description = "订单 ID") @PathVariable UUID id,
                                                   @RequestBody @Valid OrderStatusUpdateRequest request) {
        log.info("[API] 更新订单状态: id={}, target={}", id, request.getTargetStatus());

        OrderStatus target = request.getTargetStatus();
        boolean updated = commandService.updateOrderStatus(id, target.getCode());
        if (!updated) {
            return ApiResponse.fail(404, "订单不存在或状态未变更: id=" + id);
        }
        OrdOrder order = queryService.getById(String.valueOf(id));
        return ApiResponse.success(OrderConverter.toResponse(order));
    }

    /**
     * 取消订单。
     *
     * <p>仅 PENDING_PAYMENT / PAID 状态可取消；领域方法
     * {@link OrdOrder#cancel(String)} 校验后写入取消时间、原因，
     * 并通过 outbox 发 {@code OrderCancelledEvent} 给下游（库存释放、退款、通知）。</p>
     */
    @Operation(summary = "取消订单", description = "取消待支付或已支付的订单并记录取消原因。")
    @PutMapping("/{id}/cancel")
    public ApiResponse<OrderResponse> cancel(@Parameter(description = "订单 ID") @PathVariable UUID id,
                                             @RequestBody @Valid OrderCancelRequest request) {
        log.info("[API] 取消订单: id={}, reason={}", (id + "").replace("\r", "_").replace("\n", "_"), (request.getReason() + "").replace("\r", "_").replace("\n", "_"));

        boolean cancelled = commandService.cancelOrder(id, request.getReason());
        if (!cancelled) {
            return ApiResponse.fail(404, "订单不存在: id=" + id);
        }

        OrdOrder updated = queryService.getById(String.valueOf(id));
        return ApiResponse.success(OrderConverter.toResponse(updated));
    }
}

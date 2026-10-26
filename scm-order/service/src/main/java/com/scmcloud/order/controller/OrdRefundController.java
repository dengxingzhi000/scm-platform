package com.scmcloud.order.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.common.response.ApiResponse;
import com.scmcloud.order.domain.entity.OrdRefund;
import com.scmcloud.order.domain.entity.OrdRefundItem;
import com.scmcloud.order.dto.OrderConverter;
import com.scmcloud.order.dto.RefundApproveRequest;
import com.scmcloud.order.dto.RefundCreateRequest;
import com.scmcloud.order.dto.RefundItemResponse;
import com.scmcloud.order.dto.RefundQueryRequest;
import com.scmcloud.order.dto.RefundRejectRequest;
import com.scmcloud.order.dto.RefundResponse;
import com.scmcloud.order.service.command.OrdRefundCommandService;
import com.scmcloud.order.service.query.OrdRefundItemQueryService;
import com.scmcloud.order.service.query.OrdRefundQueryService;
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

/**
 * 退款 controller。
 *
 * <p>职责：</p>
 * <ul>
 *   <li>GET {@code /{id}} — 按 ID 查询退款单</li>
 *   <li>GET {@code /{id}/items} — 查询退款明细</li>
 *   <li>GET {@code /order/{orderId}} — 按订单查退款列表</li>
 *   <li>POST {@code /query} — 统一条件分页查询</li>
 *   <li>POST {@code /} — 创建退款单（带 items）</li>
 *   <li>PUT {@code /{id}/approve} — 审核通过</li>
 *   <li>PUT {@code /{id}/reject} — 拒绝退款</li>
 *   <li>PUT {@code /{id}/complete} — 完成退款（线下已退款后回调）</li>
 * </ul>
 *
 * <p><b>不</b>暴露任意字段更新 / 物理删除入口。</p>
 *
 * @author SCM Platform Team
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/refunds")
@RequiredArgsConstructor
@Tag(name = "订单退款", description = "退款单的查询、创建、审核与完成接口。")
public class OrdRefundController {

    private final OrdRefundQueryService queryService;
    private final OrdRefundItemQueryService refundItemQueryService;
    private final OrdRefundCommandService commandService;

    @Operation(summary = "查询退款单", description = "根据退款单 ID 查询退款详情及其明细。")
    @GetMapping("/{id}")
    public ApiResponse<RefundResponse> getById(@Parameter(description = "退款单 ID") @PathVariable UUID id) {
        log.info("[API] Query refund: id={}", id);
        RefundResponse r = OrderConverter.toRefundResponse(queryService.getById(id));
        if (r != null) {
            List<OrdRefundItem> items = refundItemQueryService.listByRefundId(id);
            r.setItems(OrderConverter.toRefundItemResponseList(items));
        }
        return ApiResponse.success(r);
    }

    @Operation(summary = "查询退款明细", description = "根据退款单 ID 查询退款明细列表。")
    @GetMapping("/{id}/items")
    public ApiResponse<List<RefundItemResponse>> listItems(@Parameter(description = "退款单 ID") @PathVariable UUID id) {
        log.info("[API] Query refund items: refundId={}", id);
        return ApiResponse.success(
                OrderConverter.toRefundItemResponseList(refundItemQueryService.listByRefundId(id)));
    }

    @Operation(summary = "按订单查询退款单", description = "根据订单 ID 查询该订单关联的退款单列表。")
    @GetMapping("/order/{orderId}")
    public ApiResponse<List<RefundResponse>> listByOrderId(@Parameter(description = "订单 ID") @PathVariable UUID orderId) {
        log.info("[API] Query refunds by order: orderId={}", orderId);
        return ApiResponse.success(OrderConverter.toRefundResponseList(queryService.listByOrderId(orderId)));
    }

    /**
     * 统一条件分页查询。
     */
    @Operation(summary = "分页查询退款单", description = "按条件分页查询退款单，支持退款单号、订单、用户、状态及时间区间过滤。")
    @PostMapping("/query")
    public ApiResponse<Page<RefundResponse>> query(@RequestBody @Valid RefundQueryRequest request) {
        log.info("[API] Query refunds: {}", request);

        Page<OrdRefund> page = queryService.pageWithWrapper(
                new Page<>(request.getPageNum(), request.getPageSize()),
                request.getRefundNo(), request.getOrderId(), request.getUserId(),
                request.getStatus(), request.getStartTime(), request.getEndTime());
        Page<RefundResponse> mapped = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        java.util.List<RefundResponse> items = new java.util.ArrayList<>();
        for (OrdRefund r : page.getRecords()) {
            items.add(OrderConverter.toRefundResponse(r));
        }
        mapped.setRecords(items);
        return ApiResponse.success(mapped);
    }

    /**
     * 创建退款单（带 items，同事务内写入子表）。
     */
    @Operation(summary = "创建退款单", description = "创建退款单并同时写入退款明细。")
    @PostMapping
    public ApiResponse<RefundResponse> createRefund(@RequestBody @Valid RefundCreateRequest request) {
        log.info("[API] Create refund: refundNo={}, orderId={}", request.getRefundNo(), request.getOrderId());

        OrdRefund refund = OrderConverter.toRefundEntity(request);
        java.util.List<OrdRefundItem> items = new java.util.ArrayList<>();
        for (var ri : request.getItems()) {
            items.add(OrderConverter.toRefundItemEntity(ri));
        }
        refund.setItems(items);

        OrdRefund created = commandService.createRefund(refund);
        return ApiResponse.success(OrderConverter.toRefundResponse(created));
    }

    /**
     * 审核通过退款。
     */
    @Operation(summary = "审核通过退款", description = "审核通过退款申请并记录处理人信息。")
    @PutMapping("/{id}/approve")
    public ApiResponse<RefundResponse> approve(
            @Parameter(description = "退款单 ID") @PathVariable UUID id,
            @RequestBody @Valid RefundApproveRequest request) {
        log.info("[API] Approve refund: id={}, handler={}", id, request.getHandlerId());
        commandService.approveRefund(id, request.getHandlerId(), request.getHandlerName(), request.getHandlerRemark());
        RefundResponse r = OrderConverter.toRefundResponse(queryService.getById(id));
        if (r != null) {
            r.setItems(OrderConverter.toRefundItemResponseList(refundItemQueryService.listByRefundId(id)));
        }
        return ApiResponse.success(r);
    }

    /**
     * 拒绝退款。
     */
    @Operation(summary = "拒绝退款", description = "拒绝退款申请并记录处理人信息。")
    @PutMapping("/{id}/reject")
    public ApiResponse<RefundResponse> reject(
            @Parameter(description = "退款单 ID") @PathVariable UUID id,
            @RequestBody @Valid RefundRejectRequest request) {
        log.info("[API] Reject refund: id={}, handler={}", id, request.getHandlerId());
        commandService.rejectRefund(id, request.getHandlerId(), request.getHandlerName(), request.getHandlerRemark());
        RefundResponse r = OrderConverter.toRefundResponse(queryService.getById(id));
        if (r != null) {
            r.setItems(OrderConverter.toRefundItemResponseList(refundItemQueryService.listByRefundId(id)));
        }
        return ApiResponse.success(r);
    }

    /**
     * 完成退款（线下已退款成功后回调）。
     */
    @Operation(summary = "完成退款", description = "标记退款单已完成，用于线下退款成功后的回调。")
    @PutMapping("/{id}/complete")
    public ApiResponse<RefundResponse> complete(@Parameter(description = "退款单 ID") @PathVariable UUID id) {
        log.info("[API] Complete refund: id={}", id);
        commandService.completeRefund(id);
        RefundResponse r = OrderConverter.toRefundResponse(queryService.getById(id));
        if (r != null) {
            r.setItems(OrderConverter.toRefundItemResponseList(refundItemQueryService.listByRefundId(id)));
        }
        return ApiResponse.success(r);
    }
}

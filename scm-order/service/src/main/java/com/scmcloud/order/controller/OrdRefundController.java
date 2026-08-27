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
public class OrdRefundController {

    private final OrdRefundQueryService queryService;
    private final OrdRefundItemQueryService refundItemQueryService;
    private final OrdRefundCommandService commandService;

    @GetMapping("/{id}")
    public ApiResponse<RefundResponse> getById(@PathVariable UUID id) {
        log.info("[API] Query refund: id={}", id);
        RefundResponse r = OrderConverter.toRefundResponse(queryService.getById(id));
        if (r != null) {
            List<OrdRefundItem> items = refundItemQueryService.listByRefundId(id);
            r.setItems(OrderConverter.toRefundItemResponseList(items));
        }
        return ApiResponse.success(r);
    }

    @GetMapping("/{id}/items")
    public ApiResponse<List<RefundItemResponse>> listItems(@PathVariable UUID id) {
        log.info("[API] Query refund items: refundId={}", id);
        return ApiResponse.success(
                OrderConverter.toRefundItemResponseList(refundItemQueryService.listByRefundId(id)));
    }

    @GetMapping("/order/{orderId}")
    public ApiResponse<List<RefundResponse>> listByOrderId(@PathVariable UUID orderId) {
        log.info("[API] Query refunds by order: orderId={}", orderId);
        return ApiResponse.success(OrderConverter.toRefundResponseList(queryService.listByOrderId(orderId)));
    }

    /**
     * 统一条件分页查询。
     */
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
    @PutMapping("/{id}/approve")
    public ApiResponse<RefundResponse> approve(
            @PathVariable UUID id,
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
    @PutMapping("/{id}/reject")
    public ApiResponse<RefundResponse> reject(
            @PathVariable UUID id,
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
    @PutMapping("/{id}/complete")
    public ApiResponse<RefundResponse> complete(@PathVariable UUID id) {
        log.info("[API] Complete refund: id={}", id);
        commandService.completeRefund(id);
        RefundResponse r = OrderConverter.toRefundResponse(queryService.getById(id));
        if (r != null) {
            r.setItems(OrderConverter.toRefundItemResponseList(refundItemQueryService.listByRefundId(id)));
        }
        return ApiResponse.success(r);
    }
}

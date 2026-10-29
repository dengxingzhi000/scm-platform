package com.scmcloud.order.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.common.response.ApiResponse;
import com.scmcloud.common.response.ResultCode;
import com.scmcloud.order.domain.entity.OrdPayment;
import com.scmcloud.order.dto.OrderConverter;
import com.scmcloud.order.dto.PaymentCreateRequest;
import com.scmcloud.order.dto.PaymentQueryRequest;
import com.scmcloud.order.dto.PaymentResponse;
import com.scmcloud.order.dto.PaymentStatusUpdateRequest;
import com.scmcloud.order.service.command.OrdPaymentCommandService;
import com.scmcloud.order.service.query.OrdPaymentQueryService;
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
 * 支付 controller。
 *
 * <p>职责：</p>
 * <ul>
 *   <li>GET {@code /{id}} — 按 ID 查询单条支付</li>
 *   <li>POST {@code /query} — 统一条件分页查询（含 orderId / status / 时间区间）</li>
 *   <li>POST {@code /} — 创建支付记录（带 {@code @Valid}）</li>
 *   <li>PUT {@code /{id}/status} — 状态流转（带枚举状态，{@code PaymentStatus}）</li>
 * </ul>
 *
 * <p><b>不</b>暴露任意字段更新 / 物理删除入口（支付记录应 append-only 或走"作废"状态）。</p>
 *
 * @author SCM Platform Team
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "订单支付", description = "支付记录的查询、创建与状态流转接口。")
public class OrdPaymentController {

    private final OrdPaymentQueryService queryService;
    private final OrdPaymentCommandService commandService;

    @Operation(summary = "查询支付记录", description = "根据支付记录 ID 查询支付详情。")
    @GetMapping("/{id}")
    public ApiResponse<PaymentResponse> getById(@Parameter(description = "支付记录 ID") @PathVariable UUID id) {
        log.info("[API] Query payment: id={}", id);
        return ApiResponse.success(OrderConverter.toPaymentResponse(queryService.getById(id)));
    }

    /**
     * 按订单 ID 查询支付记录（单订单可能有多个支付记录：部分退款、合并支付等）。
     */
    @Operation(summary = "按订单查询支付记录", description = "根据订单 ID 查询该订单关联的全部支付记录。")
    @GetMapping("/order/{orderId}")
    public ApiResponse<List<PaymentResponse>> listByOrderId(@Parameter(description = "订单 ID") @PathVariable UUID orderId) {
        log.info("[API] Query payments by order: orderId={}", orderId);
        return ApiResponse.success(OrderConverter.toPaymentResponseList(queryService.listByOrderId(orderId)));
    }

    /**
     * 统一条件分页查询。
     */
    @Operation(summary = "分页查询支付记录", description = "按条件分页查询支付记录，支持支付单号、订单、用户、状态及时间区间过滤。")
    @PostMapping("/query")
    public ApiResponse<Page<PaymentResponse>> query(@RequestBody @Valid PaymentQueryRequest request) {
        log.info("[API] Query payments: {}", (request + "").replace("\r", "_").replace("\n", "_"));

        Page<OrdPayment> page = queryService.pageWithWrapper(
                new Page<>(request.getPageNum(), request.getPageSize()),
                request.getPaymentNo(), request.getOrderId(), request.getUserId(),
                request.getStatus(), request.getStartTime(), request.getEndTime());
        Page<PaymentResponse> mapped = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        java.util.List<PaymentResponse> items = new java.util.ArrayList<>();
        for (OrdPayment p : page.getRecords()) {
            items.add(OrderConverter.toPaymentResponse(p));
        }
        mapped.setRecords(items);
        return ApiResponse.success(mapped);
    }

    /**
     * 创建支付记录。
     */
    @Operation(summary = "创建支付记录", description = "创建新的支付记录。")
    @PostMapping
    public ApiResponse<PaymentResponse> createPayment(@RequestBody @Valid PaymentCreateRequest request) {
        log.info("[API] Create payment: paymentNo={}, orderId={}", (request.getPaymentNo() + "").replace("\r", "_").replace("\n", "_"), (request.getOrderId() + "").replace("\r", "_").replace("\n", "_"));

        OrdPayment payment = OrderConverter.toPaymentEntity(request);
        OrdPayment created = commandService.createPayment(payment);
        if (created == null) {
            return ApiResponse.fail(ResultCode.SERVER_ERROR.getCode(), "创建支付记录失败");
        }
        return ApiResponse.success(OrderConverter.toPaymentResponse(created));
    }

    /**
     * 支付状态流转。
     */
    @Operation(summary = "更新支付状态", description = "按状态流转规则更新支付记录状态并记录变更原因。")
    @PutMapping("/{id}/status")
    public ApiResponse<PaymentResponse> updateStatus(
            @Parameter(description = "支付记录 ID") @PathVariable UUID id,
            @RequestBody @Valid PaymentStatusUpdateRequest request) {
        log.info("[API] Update payment status: id={}, target={}", id, request.getTargetStatus());

        boolean updated = commandService.updatePaymentStatus(id, request.getTargetStatus(), request.getReason());
        if (!updated) {
            return ApiResponse.fail(ResultCode.NOT_FOUND.getCode(), "支付记录不存在或状态未变更");
        }
        return ApiResponse.success(OrderConverter.toPaymentResponse(queryService.getById(id)));
    }
}

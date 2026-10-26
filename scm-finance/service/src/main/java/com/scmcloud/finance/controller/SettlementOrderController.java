package com.scmcloud.finance.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.scmcloud.finance.domain.entity.SettlementOrder;
import com.scmcloud.finance.service.ISettlementOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.math.BigDecimal;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/settlement-order")
@Tag(name = "结算单", description = "管理结算单的创建、确认、付款与分页查询。")
public class SettlementOrderController {
    private final ISettlementOrderService settlementOrderService;

    @Operation(summary = "查询结算单详情", description = "根据结算单 ID 查询结算单的完整信息。")
    @GetMapping("/{id}")
    public ApiResponse<SettlementOrder> getById(@Parameter(description = "结算单 ID") @PathVariable String id) {
        SettlementOrder order = settlementOrderService.getById(id);
        return ApiResponse.success(order);
    }

    @Operation(summary = "创建结算单", description = "根据结算数据创建结算单并汇总实际应收应付金额。")
    @PostMapping
    public ApiResponse<SettlementOrder> create(@RequestBody SettlementOrder order) {
        SettlementOrder created = settlementOrderService.createSettlement(order);
        return ApiResponse.success(created);
    }

    @Operation(summary = "更新结算单", description = "按结算单 ID 更新结算单信息并刷新更新时间。")
    @PutMapping("/{id}")
    public ApiResponse<SettlementOrder> update(@Parameter(description = "结算单 ID") @PathVariable String id, @RequestBody SettlementOrder order) {
        order.setId(id);
        order.setUpdateTime(java.time.LocalDateTime.now());
        settlementOrderService.updateById(order);
        log.info("Settlement order updated successfully: id={}", id);
        return ApiResponse.success(order);
    }

    @Operation(summary = "删除结算单", description = "按结算单 ID 对结算单执行逻辑删除。")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@Parameter(description = "结算单 ID") @PathVariable String id) {
        SettlementOrder order = settlementOrderService.getById(id);
        if (order != null) {
            order.setDeleted(true);
            order.setUpdateTime(java.time.LocalDateTime.now());
            settlementOrderService.updateById(order);
            log.info("Settlement order deleted successfully: id={}", id);
        }
        return ApiResponse.success();
    }

    @Operation(summary = "确认结算单", description = "记录审批人并确认结算单，使其进入可付款状态。")
    @PostMapping("/{id}/confirm")
    public ApiResponse<SettlementOrder> confirm(
            @Parameter(description = "结算单 ID") @PathVariable String id,
            @Parameter(description = "审批人 ID") @RequestParam String approverId,
            @Parameter(description = "审批人姓名") @RequestParam String approverName) {
        SettlementOrder order = settlementOrderService.confirmSettlement(id, approverId, approverName);
        return ApiResponse.success(order);
    }

    @Operation(summary = "登记结算单付款", description = "按结算单 ID 登记付款金额并更新已付与未付金额。")
    @PostMapping("/{id}/pay")
    public ApiResponse<SettlementOrder> pay(
            @Parameter(description = "结算单 ID") @PathVariable String id,
            @Parameter(description = "本次付款金额") @RequestParam BigDecimal amount) {
        SettlementOrder order = settlementOrderService.recordPayment(id, amount);
        return ApiResponse.success(order);
    }

    @Operation(summary = "分页查询结算单", description = "按结算状态分页查询结算单列表，状态为空时查询全部。")
    @GetMapping("/list")
    public ApiResponse<Page<SettlementOrder>> listByStatus(
            @Parameter(description = "结算状态，不传则查询全部") @RequestParam(required = false) Integer status,
            @Parameter(description = "页码，从 1 开始") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int size) {
        Page<SettlementOrder> result = settlementOrderService.listByStatus(status, page, size);
        return ApiResponse.success(result);
    }
}

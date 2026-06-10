package com.scmcloud.finance.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.scmcloud.finance.domain.entity.SettlementOrder;
import com.scmcloud.finance.service.ISettlementOrderService;

import java.math.BigDecimal;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/settlement-order")
public class SettlementOrderController {

    private final ISettlementOrderService settlementOrderService;

    @GetMapping("/{id}")
    public ApiResponse<SettlementOrder> getById(@PathVariable String id) {
        SettlementOrder order = settlementOrderService.getById(id);
        return ApiResponse.success(order);
    }

    @PostMapping
    public ApiResponse<SettlementOrder> create(@RequestBody SettlementOrder order) {
        SettlementOrder created = settlementOrderService.createSettlement(order);
        return ApiResponse.success(created);
    }

    @PutMapping("/{id}")
    public ApiResponse<SettlementOrder> update(@PathVariable String id, @RequestBody SettlementOrder order) {
        order.setId(id);
        order.setUpdateTime(java.time.LocalDateTime.now());
        settlementOrderService.updateById(order);
        log.info("Settlement order updated successfully: id={}", id);
        return ApiResponse.success(order);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        SettlementOrder order = settlementOrderService.getById(id);
        if (order != null) {
            order.setDeleted(true);
            order.setUpdateTime(java.time.LocalDateTime.now());
            settlementOrderService.updateById(order);
            log.info("Settlement order deleted successfully: id={}", id);
        }
        return ApiResponse.success();
    }

    @PostMapping("/{id}/confirm")
    public ApiResponse<SettlementOrder> confirm(
            @PathVariable String id,
            @RequestParam String approverId,
            @RequestParam String approverName) {
        SettlementOrder order = settlementOrderService.confirmSettlement(id, approverId, approverName);
        return ApiResponse.success(order);
    }

    @PostMapping("/{id}/pay")
    public ApiResponse<SettlementOrder> pay(
            @PathVariable String id,
            @RequestParam BigDecimal amount) {
        SettlementOrder order = settlementOrderService.recordPayment(id, amount);
        return ApiResponse.success(order);
    }

    @GetMapping("/list")
    public ApiResponse<Page<SettlementOrder>> listByStatus(
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<SettlementOrder> result = settlementOrderService.listByStatus(status, page, size);
        return ApiResponse.success(result);
    }
}

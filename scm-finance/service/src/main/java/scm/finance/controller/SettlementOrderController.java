package scm.finance.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.frog.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import scm.finance.domain.entity.SettlementOrder;
import scm.finance.service.ISettlementOrderService;

import java.math.BigDecimal;

@Slf4j
@RestController
@RequestMapping("/settlement-order")
@Tag(name = "结算单", description = "结算单管理接口")
public class SettlementOrderController {

    @Autowired
    private ISettlementOrderService settlementOrderService;

    @GetMapping("/{id}")
    @Operation(summary = "查询结算单详情")
    public ApiResponse<SettlementOrder> getById(@PathVariable String id) {
        SettlementOrder order = settlementOrderService.getById(id);
        return ApiResponse.success(order);
    }

    @PostMapping
    @Operation(summary = "创建结算单")
    public ApiResponse<SettlementOrder> create(@RequestBody SettlementOrder order) {
        SettlementOrder created = settlementOrderService.createSettlement(order);
        return ApiResponse.success(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新结算单")
    public ApiResponse<SettlementOrder> update(@PathVariable String id, @RequestBody SettlementOrder order) {
        order.setId(id);
        order.setUpdateTime(java.time.LocalDateTime.now());
        settlementOrderService.updateById(order);
        log.info("结算单更新成功: id={}", id);
        return ApiResponse.success(order);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除结算单")
    public ApiResponse<Void> delete(@PathVariable String id) {
        SettlementOrder order = settlementOrderService.getById(id);
        if (order != null) {
            order.setDeleted(true);
            order.setUpdateTime(java.time.LocalDateTime.now());
            settlementOrderService.updateById(order);
            log.info("结算单删除成功: id={}", id);
        }
        return ApiResponse.success();
    }

    @PostMapping("/{id}/confirm")
    @Operation(summary = "确认结算单")
    public ApiResponse<SettlementOrder> confirm(
            @PathVariable String id,
            @Parameter(description = "审批人ID", required = true) @RequestParam String approverId,
            @Parameter(description = "审批人姓名", required = true) @RequestParam String approverName) {
        SettlementOrder order = settlementOrderService.confirmSettlement(id, approverId, approverName);
        return ApiResponse.success(order);
    }

    @PostMapping("/{id}/pay")
    @Operation(summary = "结算单付款")
    public ApiResponse<SettlementOrder> pay(
            @PathVariable String id,
            @Parameter(description = "付款金额", required = true) @RequestParam BigDecimal amount) {
        SettlementOrder order = settlementOrderService.recordPayment(id, amount);
        return ApiResponse.success(order);
    }

    @GetMapping("/list")
    @Operation(summary = "按状态查询结算单列表")
    public ApiResponse<Page<SettlementOrder>> listByStatus(
            @Parameter(description = "状态: 0-待确认,1-已确认,2-待付款,3-部分付款,4-已付款")
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<SettlementOrder> result = settlementOrderService.listByStatus(status, page, size);
        return ApiResponse.success(result);
    }
}

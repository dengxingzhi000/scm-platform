package com.scmcloud.supplier.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.common.response.ApiResponse;
import com.scmcloud.common.status.StatusValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.scmcloud.supplier.domain.entity.SupSettlement;
import com.scmcloud.supplier.service.ISupSettlementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/api/v1/settlements")
@Tag(name = "供应商结算", description = "提供供应商对账单的创建、查询、确认、付款与删除接口。")
public class SupSettlementController {

    private final ISupSettlementService settlementService;
    private final StatusValidator statusValidator;

    @Operation(summary = "查询对账单详情", description = "根据对账单ID查询未删除的对账单详情。")
    @GetMapping("/{id}")
    public ApiResponse<SupSettlement> getById(@Parameter(description = "对账单ID") @PathVariable String id) {
        log.info("[API] 查询对账单详情 id={}", (id + "").replace("\r", "_").replace("\n", "_"));
        SupSettlement settlement = settlementService.getById(id);
        if (settlement == null || Boolean.TRUE.equals(settlement.getDeleted())) {
            return ApiResponse.fail(404, "对账单不存在");
        }
        return ApiResponse.success(settlement);
    }

    @Operation(summary = "创建对账单", description = "新建供应商对账单并初始化为草稿状态。")
    @PostMapping
    public ApiResponse<SupSettlement> create(@RequestBody SupSettlement settlement) {
        log.info("[API] 创建对账单 supplierId={}", (settlement.getSupplierId() + "").replace("\r", "_").replace("\n", "_"));
        settlement.setId(UUID.randomUUID());
        settlement.setStatus(0);
        settlement.setDeleted(false);
        settlement.setCreateTime(LocalDateTime.now());
        settlement.setUpdateTime(LocalDateTime.now());
        settlementService.save(settlement);
        log.info("[API] 对账单创建成功 id={}", settlement.getId());
        return ApiResponse.success(settlement);
    }

    @Operation(summary = "更新对账单", description = "根据对账单ID更新对账单信息并校验状态变更是否合法。")
    @PutMapping("/{id}")
    public ApiResponse<SupSettlement> update(@Parameter(description = "对账单ID") @PathVariable String id, @RequestBody SupSettlement settlement) {
        log.info("[API] 更新对账单 id={}", (id + "").replace("\r", "_").replace("\n", "_"));
        SupSettlement existing = settlementService.getById(id);
        if (existing == null || Boolean.TRUE.equals(existing.getDeleted())) {
            return ApiResponse.fail(404, "对账单不存在");
        }
        statusValidator.validateTransition("SUPPLIER_SETTLEMENT", "DRAFT", "CONFIRMED");
        settlement.setId(UUID.fromString(id));
        settlement.setUpdateTime(LocalDateTime.now());
        settlementService.updateById(settlement);
        return ApiResponse.success(settlementService.getById(id));
    }

    @Operation(summary = "删除对账单", description = "对指定对账单执行逻辑删除。")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@Parameter(description = "对账单ID") @PathVariable String id) {
        log.info("[API] 删除对账单 id={}", (id + "").replace("\r", "_").replace("\n", "_"));
        SupSettlement existing = settlementService.getById(id);
        if (existing == null || Boolean.TRUE.equals(existing.getDeleted())) {
            return ApiResponse.fail(404, "对账单不存在");
        }
        existing.setDeleted(true);
        existing.setUpdateTime(LocalDateTime.now());
        settlementService.updateById(existing);
        return ApiResponse.success();
    }

    @Operation(summary = "分页查询对账单", description = "按供应商、状态与结算周期分页查询对账单列表。")
    @GetMapping
    public ApiResponse<Page<SupSettlement>> pageList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "供应商ID") @RequestParam(required = false) String supplierId,
            @Parameter(description = "对账单状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "结算周期，例如 2026-09") @RequestParam(required = false) String settlementPeriod) {
        log.info("[API] 分页查询对账单 page={}, size={}, supplierId={}, status={}", (page + "").replace("\r", "_").replace("\n", "_"), (size + "").replace("\r", "_").replace("\n", "_"), (supplierId + "").replace("\r", "_").replace("\n", "_"), (status + "").replace("\r", "_").replace("\n", "_"));
        Page<SupSettlement> result = settlementService.pageList(page, size, supplierId, status, settlementPeriod);
        return ApiResponse.success(result);
    }

    @Operation(summary = "查询供应商对账单", description = "查询指定供应商的全部对账单。")
    @GetMapping("/supplier/{supplierId}")
    public ApiResponse<List<SupSettlement>> listBySupplierId(@Parameter(description = "供应商ID") @PathVariable String supplierId) {
        log.info("[API] 查询供应商对账单列表: supplierId={}", (supplierId + "").replace("\r", "_").replace("\n", "_"));
        return ApiResponse.success(settlementService.listBySupplierId(supplierId));
    }

    @Operation(summary = "确认对账单", description = "确认供应商对账单并记录审核人信息。")
    @PutMapping("/{id}/confirm")
    public ApiResponse<Void> confirm(@Parameter(description = "对账单ID") @PathVariable String id,
                                     @Parameter(description = "审核人ID") @RequestParam String approverId,
                                     @Parameter(description = "审核人姓名") @RequestParam String approverName) {
        log.info("[API] 确认对账单 id={}, approverId={}", (id + "").replace("\r", "_").replace("\n", "_"), (approverId + "").replace("\r", "_").replace("\n", "_"));
        boolean success = settlementService.confirm(id, approverId, approverName);
        return success ? ApiResponse.success() : ApiResponse.fail(400, "确认失败，对账单状态不正确");
    }

    @Operation(summary = "标记对账单已付款", description = "将对账单标记为已付款并记录更新人。")
    @PutMapping("/{id}/pay")
    public ApiResponse<Void> markAsPaid(@Parameter(description = "对账单ID") @PathVariable String id, @Parameter(description = "更新人") @RequestParam String updateBy) {
        log.info("[API] 标记对账单已付款: id={}", (id + "").replace("\r", "_").replace("\n", "_"));
        boolean success = settlementService.markAsPaid(id, updateBy);
        return success ? ApiResponse.success() : ApiResponse.fail(400, "标记付款失败，对账单状态不正确");
    }
}

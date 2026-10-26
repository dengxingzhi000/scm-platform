package com.scmcloud.supplier.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.scmcloud.supplier.domain.entity.SupSupplier;
import com.scmcloud.supplier.service.ISupSupplierService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/api/v1/suppliers")
@Tag(name = "供应商管理", description = "提供供应商档案的创建、查询、维护与启停用接口。")
public class SupSupplierController {

    private final ISupSupplierService supplierService;

    @Operation(summary = "查询供应商详情", description = "根据供应商ID查询未删除的供应商详情。")
    @GetMapping("/{id}")
    public ApiResponse<SupSupplier> getById(@Parameter(description = "供应商ID") @PathVariable String id) {
        log.info("[API] 查询供应商详情 id={}", id);
        SupSupplier supplier = supplierService.getById(id);
        if (supplier == null || Boolean.TRUE.equals(supplier.getDeleted())) {
            return ApiResponse.fail(404, "供应商不存在");
        }
        return ApiResponse.success(supplier);
    }

    @Operation(summary = "创建供应商", description = "新增供应商档案并初始化启用状态与合作状态。")
    @PostMapping
    public ApiResponse<SupSupplier> create(@RequestBody SupSupplier supplier) {
        log.info("[API] 创建供应商 name={}", supplier.getSupplierName());
        supplier.setId(java.util.UUID.randomUUID().toString());
        supplier.setDeleted(false);
        if (supplier.getEnabled() == null) {
            supplier.setEnabled(true);
        }
        if (supplier.getCooperationStatus() == null) {
            supplier.setCooperationStatus(0);
        }
        supplier.setCreateTime(java.time.LocalDateTime.now());
        supplier.setUpdateTime(java.time.LocalDateTime.now());
        supplierService.save(supplier);
        log.info("[API] 供应商创建成功 id={}", supplier.getId());
        return ApiResponse.success(supplier);
    }

    @Operation(summary = "更新供应商", description = "根据供应商ID更新供应商档案信息。")
    @PutMapping("/{id}")
    public ApiResponse<SupSupplier> update(@Parameter(description = "供应商ID") @PathVariable String id, @RequestBody SupSupplier supplier) {
        log.info("[API] 更新供应商 id={}", id);
        SupSupplier existing = supplierService.getById(id);
        if (existing == null || Boolean.TRUE.equals(existing.getDeleted())) {
            return ApiResponse.fail(404, "供应商不存在");
        }
        supplier.setId(id);
        supplier.setUpdateTime(java.time.LocalDateTime.now());
        supplierService.updateById(supplier);
        return ApiResponse.success(supplierService.getById(id));
    }

    @Operation(summary = "删除供应商", description = "对指定供应商执行逻辑删除。")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@Parameter(description = "供应商ID") @PathVariable String id) {
        log.info("[API] 删除供应商 id={}", id);
        SupSupplier existing = supplierService.getById(id);
        if (existing == null || Boolean.TRUE.equals(existing.getDeleted())) {
            return ApiResponse.fail(404, "供应商不存在");
        }
        existing.setDeleted(true);
        existing.setUpdateTime(java.time.LocalDateTime.now());
        supplierService.updateById(existing);
        return ApiResponse.success();
    }

    @Operation(summary = "分页查询供应商", description = "按关键字、供应商类型、合作状态与启用状态分页查询供应商。")
    @GetMapping
    public ApiResponse<Page<SupSupplier>> pageList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "搜索关键字") @RequestParam(required = false) String keyword,
            @Parameter(description = "供应商类型") @RequestParam(required = false) Integer supplierType,
            @Parameter(description = "合作状态") @RequestParam(required = false) Integer cooperationStatus,
            @Parameter(description = "是否启用") @RequestParam(required = false) Boolean enabled) {
        log.info("[API] 分页查询供应商 page={}, size={}, keyword={}", page, size, keyword);
        Page<SupSupplier> result = supplierService.pageList(page, size, keyword, supplierType,
                cooperationStatus, enabled);
        return ApiResponse.success(result);
    }

    @Operation(summary = "查询启用供应商", description = "查询当前所有已启用的供应商。")
    @GetMapping("/active")
    public ApiResponse<List<SupSupplier>> listActive() {
        log.info("[API] 查询所有启用供应商");
        return ApiResponse.success(supplierService.listActive());
    }

    @Operation(summary = "搜索供应商", description = "根据供应商名称关键字模糊搜索供应商。")
    @GetMapping("/search")
    public ApiResponse<List<SupSupplier>> searchByName(
            @Parameter(description = "供应商名称关键字") @RequestParam String name) {
        log.info("[API] 搜索供应商 name={}", name);
        return ApiResponse.success(supplierService.searchByName(name));
    }

    @Operation(summary = "启用供应商", description = "将指定供应商状态置为启用。")
    @PutMapping("/{id}/enable")
    public ApiResponse<Void> enable(@Parameter(description = "供应商ID") @PathVariable String id) {
        log.info("[API] 启用供应商 id={}", id);
        boolean success = supplierService.enableSupplier(id);
        return success ? ApiResponse.success() : ApiResponse.fail(400, "启用失败");
    }

    @Operation(summary = "停用供应商", description = "将指定供应商状态置为停用。")
    @PutMapping("/{id}/disable")
    public ApiResponse<Void> disable(@Parameter(description = "供应商ID") @PathVariable String id) {
        log.info("[API] 停用供应商 id={}", id);
        boolean success = supplierService.disableSupplier(id);
        return success ? ApiResponse.success() : ApiResponse.fail(400, "停用失败");
    }
}

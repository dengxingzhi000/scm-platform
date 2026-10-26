package com.scmcloud.supplier.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.scmcloud.supplier.domain.entity.SupPurchaseOrderItem;
import com.scmcloud.supplier.service.ISupPurchaseOrderItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/api/v1/purchase-order-items")
@Tag(name = "采购订单明细", description = "提供采购订单明细的创建、查询、更新与删除接口。")
public class SupPurchaseOrderItemController {

    private final ISupPurchaseOrderItemService purchaseOrderItemService;

    @Operation(summary = "查询采购单明细", description = "根据明细ID查询采购订单明细详情。")
    @GetMapping("/{id}")
    public ApiResponse<SupPurchaseOrderItem> getById(@Parameter(description = "采购单明细ID") @PathVariable UUID id) {
        log.info("[API] 查询采购单明细详情 id={}", id);
        SupPurchaseOrderItem item = purchaseOrderItemService.getById(id);
        if (item == null) {
            return ApiResponse.fail(404, "采购单明细不存在");
        }
        return ApiResponse.success(item);
    }

    @Operation(summary = "创建采购单明细", description = "新增采购订单明细，未填写小计时按单价与数量自动计算。")
    @PostMapping
    public ApiResponse<SupPurchaseOrderItem> create(@RequestBody SupPurchaseOrderItem item) {
        log.info("[API] 创建采购单明细 purchaseId={}, skuId={}", item.getPurchaseId(), item.getSkuId());
        item.setId(UUID.randomUUID());
        item.setCreateTime(LocalDateTime.now());
        if (item.getSubtotal() == null && item.getUnitPrice() != null && item.getQuantity() != null) {
            item.setSubtotal(item.getUnitPrice().multiply(new java.math.BigDecimal(item.getQuantity())));
        }
        purchaseOrderItemService.save(item);
        log.info("[API] 采购单明细创建成功 id={}", item.getId());
        return ApiResponse.success(item);
    }

    @Operation(summary = "更新采购单明细", description = "根据明细ID更新采购订单明细，并重新计算小计金额。")
    @PutMapping("/{id}")
    public ApiResponse<SupPurchaseOrderItem> update(@Parameter(description = "采购单明细ID") @PathVariable UUID id,
                                                    @RequestBody SupPurchaseOrderItem item) {
        log.info("[API] 更新采购单明细 id={}", id);
        SupPurchaseOrderItem existing = purchaseOrderItemService.getById(id);
        if (existing == null) {
            return ApiResponse.fail(404, "采购单明细不存在");
        }
        item.setId(id);
        if (item.getSubtotal() == null && item.getUnitPrice() != null && item.getQuantity() != null) {
            item.setSubtotal(item.getUnitPrice().multiply(new java.math.BigDecimal(item.getQuantity())));
        }
        purchaseOrderItemService.updateById(item);
        return ApiResponse.success(purchaseOrderItemService.getById(id));
    }

    @Operation(summary = "删除采购单明细", description = "根据明细ID删除采购订单明细。")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@Parameter(description = "采购单明细ID") @PathVariable UUID id) {
        log.info("[API] 删除采购单明细 id={}", id);
        boolean success = purchaseOrderItemService.removeById(id);
        return success ? ApiResponse.success() : ApiResponse.fail(400, "删除失败");
    }

    @Operation(summary = "分页查询采购单明细", description = "按采购单ID与SKU分页查询采购订单明细列表。")
    @GetMapping
    public ApiResponse<Page<SupPurchaseOrderItem>> pageList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "采购单ID") @RequestParam(required = false) String purchaseId,
            @Parameter(description = "SKU ID") @RequestParam(required = false) String skuId) {
        log.info("[API] 分页查询采购单明细 page={}, size={}, purchaseId={}", page, size, purchaseId);
        Page<SupPurchaseOrderItem> result = purchaseOrderItemService.pageList(page, size, purchaseId, skuId);
        return ApiResponse.success(result);
    }

    @Operation(summary = "查询采购单明细列表", description = "查询指定采购单下的全部明细。")
    @GetMapping("/purchase/{purchaseId}")
    public ApiResponse<List<SupPurchaseOrderItem>> listByPurchaseId(@Parameter(description = "采购单ID") @PathVariable String purchaseId) {
        log.info("[API] 查询采购单明细列表 purchaseId={}", purchaseId);
        return ApiResponse.success(purchaseOrderItemService.listByPurchaseId(purchaseId));
    }
}

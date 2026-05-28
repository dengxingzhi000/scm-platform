package scm.supplier.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.frog.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import scm.supplier.domain.entity.SupPurchaseOrderItem;
import scm.supplier.service.ISupPurchaseOrderItemService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/purchase-order-items")
@Tag(name = "采购单明细管理", description = "采购单明细CRUD及查询接口")
public class SupPurchaseOrderItemController {

    @Autowired
    private ISupPurchaseOrderItemService purchaseOrderItemService;

    @GetMapping("/{id}")
    @Operation(summary = "查询采购单明细详情")
    public ApiResponse<SupPurchaseOrderItem> getById(@PathVariable UUID id) {
        log.info("[API] 查询采购单明细详情: id={}", id);
        SupPurchaseOrderItem item = purchaseOrderItemService.getById(id);
        if (item == null) {
            return ApiResponse.fail(404, "采购单明细不存在");
        }
        return ApiResponse.success(item);
    }

    @PostMapping
    @Operation(summary = "创建采购单明细")
    public ApiResponse<SupPurchaseOrderItem> create(@RequestBody SupPurchaseOrderItem item) {
        log.info("[API] 创建采购单明细: purchaseId={}, skuId={}", item.getPurchaseId(), item.getSkuId());
        item.setId(UUID.randomUUID());
        item.setCreateTime(LocalDateTime.now());
        if (item.getSubtotal() == null && item.getUnitPrice() != null && item.getQuantity() != null) {
            item.setSubtotal(item.getUnitPrice().multiply(new java.math.BigDecimal(item.getQuantity())));
        }
        purchaseOrderItemService.save(item);
        log.info("[API] 采购单明细创建成功: id={}", item.getId());
        return ApiResponse.success(item);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新采购单明细")
    public ApiResponse<SupPurchaseOrderItem> update(@PathVariable UUID id,
                                                    @RequestBody SupPurchaseOrderItem item) {
        log.info("[API] 更新采购单明细: id={}", id);
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

    @DeleteMapping("/{id}")
    @Operation(summary = "删除采购单明细")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        log.info("[API] 删除采购单明细: id={}", id);
        boolean success = purchaseOrderItemService.removeById(id);
        return success ? ApiResponse.success() : ApiResponse.fail(400, "删除失败");
    }

    @GetMapping
    @Operation(summary = "分页查询采购单明细列表")
    public ApiResponse<Page<SupPurchaseOrderItem>> pageList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String purchaseId,
            @RequestParam(required = false) String skuId) {
        log.info("[API] 分页查询采购单明细: page={}, size={}, purchaseId={}", page, size, purchaseId);
        Page<SupPurchaseOrderItem> result = purchaseOrderItemService.pageList(page, size, purchaseId, skuId);
        return ApiResponse.success(result);
    }

    @GetMapping("/purchase/{purchaseId}")
    @Operation(summary = "查询采购单的所有明细")
    public ApiResponse<List<SupPurchaseOrderItem>> listByPurchaseId(@PathVariable String purchaseId) {
        log.info("[API] 查询采购单明细列表: purchaseId={}", purchaseId);
        return ApiResponse.success(purchaseOrderItemService.listByPurchaseId(purchaseId));
    }
}

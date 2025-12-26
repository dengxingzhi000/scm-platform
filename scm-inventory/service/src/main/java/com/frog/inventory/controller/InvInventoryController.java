package com.frog.inventory.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.frog.inventory.domain.dto.InventoryAdjustRequest;
import com.frog.inventory.domain.dto.InventoryQueryRequest;
import com.frog.inventory.domain.dto.InventoryResponse;
import com.frog.inventory.domain.dto.InventoryStatsResponse;
import com.frog.inventory.domain.dto.InventoryTransferRequest;
import com.frog.inventory.service.IInvInventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 库存管理控制器
 *
 * <p>提供库存查询、调整、转移等REST API接口
 *
 * @author SCM Platform Team
 * @since 2025-12-26
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/inventory")
@Tag(name = "库存管理", description = "库存查询、调整、转移等接口")
public class InvInventoryController {

  @Autowired
  private IInvInventoryService inventoryService;

  /**
   * 查询单个SKU在指定仓库的库存
   */
  @GetMapping
  @Operation(summary = "查询库存", description = "根据SKU ID和仓库ID查询库存信息")
  public InventoryResponse getInventory(
      @Parameter(description = "SKU ID", required = true)
      @RequestParam @NotBlank(message = "SKU ID 不能为空") String skuId,
      @Parameter(description = "仓库 ID", required = true)
      @RequestParam @NotBlank(message = "仓库 ID 不能为空") String warehouseId) {

    log.info("📦 [API] 查询库存: skuId={}, warehouseId={}", skuId, warehouseId);

    InventoryResponse response = inventoryService.getInventory(skuId, warehouseId);

    if (response == null) {
      log.warn("⚠️  [API] 库存不存在: skuId={}, warehouseId={}", skuId, warehouseId);
    } else {
      log.info("✅ [API] 查询库存成功: skuId={}, availableStock={}",
          skuId, response.getAvailableStock());
    }

    return response;
  }

  /**
   * 批量查询库存
   */
  @PostMapping("/batch")
  @Operation(summary = "批量查询库存", description = "根据SKU ID列表批量查询库存")
  public List<InventoryResponse> batchGetInventory(
      @Parameter(description = "SKU ID列表", required = true)
      @RequestBody List<String> skuIds,
      @Parameter(description = "仓库 ID（可选）")
      @RequestParam(required = false) String warehouseId) {

    log.info("📦 [API] 批量查询库存: skuIds.size={}, warehouseId={}", skuIds.size(), warehouseId);

    List<InventoryResponse> responses = inventoryService.batchGetInventory(skuIds, warehouseId);

    log.info("✅ [API] 批量查询成功: 返回{}条库存记录", responses.size());

    return responses;
  }

  /**
   * 分页查询库存（支持多种过滤条件）
   */
  @PostMapping("/query")
  @Operation(summary = "分页查询库存", description = "支持SKU、仓库、库位、库存状态等多种条件过滤")
  public Page<InventoryResponse> queryInventory(
      @Parameter(description = "查询条件")
      @RequestBody @Valid InventoryQueryRequest request) {

    log.info("📦 [API] 分页查询库存: page={}, size={}, stockStatus={}",
        request.getPage(), request.getSize(), request.getStockStatus());

    Page<InventoryResponse> page = inventoryService.queryInventory(request);

    log.info("✅ [API] 分页查询成功: total={}, current={}",
        page.getTotal(), page.getCurrent());

    return page;
  }

  /**
   * 调整库存（入库、出库、盘点调整等）
   */
  @PostMapping("/adjust")
  @Operation(summary = "调整库存", description = "库存调整（入库、出库、盘点调整等）")
  public InventoryResponse adjustInventory(
      @Parameter(description = "调整请求")
      @RequestBody @Valid InventoryAdjustRequest request) {

    log.info("📝 [API] 调整库存: skuId={}, warehouseId={}, quantity={}, adjustType={}",
        request.getSkuId(), request.getWarehouseId(),
        request.getQuantity(), request.getAdjustType());

    try {
      InventoryResponse response = inventoryService.adjustInventory(request);

      log.info("✅ [API] 库存调整成功: skuId={}, availableStock={}",
          request.getSkuId(), response.getAvailableStock());

      return response;

    } catch (IllegalArgumentException e) {
      log.error("❌ [API] 库存调整失败: {}", e.getMessage());
      throw e;
    } catch (Exception e) {
      log.error("❌ [API] 库存调整异常: skuId={}, error={}",
          request.getSkuId(), e.getMessage(), e);
      throw new RuntimeException("库存调整失败: " + e.getMessage(), e);
    }
  }

  /**
   * 库存调拨（从一个仓库转移到另一个仓库）
   */
  @PostMapping("/transfer")
  @Operation(summary = "库存调拨", description = "仓库间库存转移")
  public boolean transferInventory(
      @Parameter(description = "调拨请求")
      @RequestBody @Valid InventoryTransferRequest request) {

    log.info("🔄 [API] 库存调拨: skuId={}, from={}, to={}, quantity={}",
        request.getSkuId(), request.getFromWarehouseId(),
        request.getToWarehouseId(), request.getQuantity());

    try {
      boolean success = inventoryService.transferInventory(request);

      if (success) {
        log.info("✅ [API] 库存调拨成功: skuId={}, transferNo={}",
            request.getSkuId(), request.getTransferNo());
      } else {
        log.warn("⚠️  [API] 库存调拨失败: skuId={}", request.getSkuId());
      }

      return success;

    } catch (IllegalArgumentException e) {
      log.error("❌ [API] 库存调拨失败: {}", e.getMessage());
      throw e;
    } catch (Exception e) {
      log.error("❌ [API] 库存调拨异常: skuId={}, error={}",
          request.getSkuId(), e.getMessage(), e);
      throw new RuntimeException("库存调拨失败: " + e.getMessage(), e);
    }
  }

  /**
   * 检查库存是否充足
   */
  @GetMapping("/check")
  @Operation(summary = "检查库存", description = "检查指定SKU在指定仓库的库存是否充足")
  public boolean checkStockAvailable(
      @Parameter(description = "SKU ID", required = true)
      @RequestParam @NotBlank(message = "SKU ID 不能为空") String skuId,
      @Parameter(description = "仓库 ID", required = true)
      @RequestParam @NotBlank(message = "仓库 ID 不能为空") String warehouseId,
      @Parameter(description = "需要的数量", required = true)
      @RequestParam @Positive(message = "数量必须大于0") Integer quantity) {

    log.debug("📦 [API] 检查库存: skuId={}, warehouseId={}, quantity={}",
        skuId, warehouseId, quantity);

    boolean available = inventoryService.checkStockAvailable(skuId, warehouseId, quantity);

    if (available) {
      log.debug("✅ [API] 库存充足: skuId={}, quantity={}", skuId, quantity);
    } else {
      log.warn("⚠️  [API] 库存不足: skuId={}, quantity={}", skuId, quantity);
    }

    return available;
  }

  /**
   * 获取库存统计信息
   */
  @GetMapping("/stats")
  @Operation(summary = "库存统计", description = "获取全局库存统计信息")
  public InventoryStatsResponse getInventoryStats() {
    log.info("📊 [API] 获取库存统计");

    InventoryStatsResponse stats = inventoryService.getInventoryStats();

    log.info("✅ [API] 库存统计成功: totalSku={}, totalStock={}, outOfStock={}",
        stats.getTotalSkuCount(), stats.getTotalStockQuantity(), stats.getOutOfStockCount());

    return stats;
  }

  /**
   * 初始化库存
   */
  @PostMapping("/init")
  @Operation(summary = "初始化库存", description = "为SKU在指定仓库初始化库存（如果不存在则创建）")
  public InventoryResponse initInventory(
      @Parameter(description = "SKU ID", required = true)
      @RequestParam @NotBlank(message = "SKU ID 不能为空") String skuId,
      @Parameter(description = "仓库 ID", required = true)
      @RequestParam @NotBlank(message = "仓库 ID 不能为空") String warehouseId,
      @Parameter(description = "初始库存数量（默认为0）")
      @RequestParam(required = false) Integer initialStock) {

    log.info("🆕 [API] 初始化库存: skuId={}, warehouseId={}, initialStock={}",
        skuId, warehouseId, initialStock);

    try {
      InventoryResponse response = inventoryService.initInventory(skuId, warehouseId, initialStock);

      log.info("✅ [API] 库存初始化成功: id={}, skuId={}", response.getId(), skuId);

      return response;

    } catch (Exception e) {
      log.error("❌ [API] 库存初始化失败: skuId={}, error={}", skuId, e.getMessage(), e);
      throw new RuntimeException("库存初始化失败: " + e.getMessage(), e);
    }
  }
}
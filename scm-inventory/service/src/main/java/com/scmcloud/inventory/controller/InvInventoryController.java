package com.scmcloud.inventory.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.common.lock.Idempotent;
import com.scmcloud.common.log.util.LogUtils;
import com.scmcloud.common.response.ApiResponse;
import com.scmcloud.common.response.ResultCode;
import com.scmcloud.inventory.dto.InventoryAdjustRequest;
import com.scmcloud.inventory.dto.InventoryQueryRequest;
import com.scmcloud.inventory.dto.InventoryResponse;
import com.scmcloud.inventory.dto.InventoryStatsResponse;
import com.scmcloud.inventory.dto.InventoryTransferRequest;
import com.scmcloud.inventory.service.IInvInventoryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 库存管理控制器
 *
 * <p>提供库存查询、调整、转移等REST API接口
 *
 * <p>优化要点：
 * <ul>
 *   <li>全部接口返回 {@link ApiResponse} 统一包装</li>
 *   <li>热点查询走 Caffeine + Redis 两级缓存</li>
 *   <li>写接口走 Redis 幂等保护（24h TTL）</li>
 *   <li>全部接口走 Sentinel 限流</li>
 *   <li>异常处理由 GlobalExceptionHandler 统一接管</li>
 * </ul>
 *
 * @author SCM Platform Team
 * @since 2025-12-26
 */
@RequiredArgsConstructor
@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/inventory")
public class InvInventoryController {

  private final IInvInventoryService inventoryService;

  /**
   * 查询单个SKU在指定仓库的库存
   */
  @GetMapping
  @Cacheable(value = "inventory", key = "#skuId + ':' + #warehouseId",
             unless = "#result == null || #result.data() == null")
  @SentinelResource(value = "inventory.get", blockHandler = "handleGetBlock")
  public ApiResponse<InventoryResponse> getInventory(
      @RequestParam @NotBlank(message = "SKU ID 不能为空") String skuId,
      @RequestParam @NotBlank(message = "仓库 ID 不能为空") String warehouseId) {

    InventoryResponse response = inventoryService.getInventory(skuId, warehouseId);
    return response == null
        ? ApiResponse.fail(ResultCode.NOT_FOUND.getCode(), "库存不存在")
        : ApiResponse.success(response);
  }

  public ApiResponse<InventoryResponse> handleGetBlock(String skuId, String warehouseId, BlockException e) {
    log.warn("Sentinel block: inventory.get skuId={}, warehouseId={}", skuId, warehouseId);
    return ApiResponse.fail(ResultCode.TOO_MANY_REQUESTS.getCode(), "请求过于频繁，请稍后再试");
  }

  /**
   * 批量查询库存
   */
  @PostMapping("/batch")
  @Cacheable(value = "inventory",
             key = "(#warehouseId ?: 'ALL') + ':' + (#skuIds?.toString() ?: '')",
             unless = "#result == null")
  @SentinelResource(value = "inventory.batchGet", blockHandler = "handleBatchGetBlock")
  public ApiResponse<List<InventoryResponse>> batchGetInventory(
      @RequestBody @NotEmpty(message = "SKU ID 列表不能为空")
      @Size(max = 100, message = "批量查询 SKU 数量不能超过 100") List<String> skuIds,
      @RequestParam(required = false) String warehouseId) {

    List<InventoryResponse> responses = inventoryService.batchGetInventory(skuIds, warehouseId);
    return ApiResponse.success(responses);
  }

  public ApiResponse<List<InventoryResponse>> handleBatchGetBlock(List<String> skuIds, String warehouseId, BlockException e) {
    int size = skuIds == null ? 0 : skuIds.size();
    log.warn("Sentinel block: inventory.batchGet size={}", size);
    return ApiResponse.fail(ResultCode.TOO_MANY_REQUESTS.getCode(), "请求过于频繁，请稍后再试");
  }

  /**
   * 分页查询库存（支持多种过滤条件）
   */
  @PostMapping("/query")
  @SentinelResource(value = "inventory.query", blockHandler = "handleQueryBlock")
  public ApiResponse<Page<InventoryResponse>> queryInventory(
      @RequestBody @Valid InventoryQueryRequest request) {

    Page<InventoryResponse> page = inventoryService.queryInventory(request);
    return ApiResponse.success(page);
  }

  public ApiResponse<Page<InventoryResponse>> handleQueryBlock(InventoryQueryRequest request, BlockException e) {
    log.warn("Sentinel block: inventory.query");
    return ApiResponse.fail(ResultCode.TOO_MANY_REQUESTS.getCode(), "请求过于频繁，请稍后再试");
  }

  /**
   * 调整库存（入库、出库、盘点调整等）
   */
  @PostMapping("/adjust")
  @Idempotent(key = "#request.referenceNo", ttl = 24, unit = TimeUnit.HOURS,
              errorMessage = "重复的库存调整请求，请勿重复提交")
  @SentinelResource(value = "inventory.adjust", blockHandler = "handleAdjustBlock")
  public ApiResponse<InventoryResponse> adjustInventory(
      @RequestBody @Valid InventoryAdjustRequest request) {

    LogUtils.business("inventory.adjust", "start", request);
    InventoryResponse response = inventoryService.adjustInventory(request);
    LogUtils.business("inventory.adjust", "success", response);
    return ApiResponse.success(response);
  }

  public ApiResponse<InventoryResponse> handleAdjustBlock(InventoryAdjustRequest request, BlockException e) {
    log.warn("Sentinel block: inventory.adjust skuId={}", request.getSkuId());
    return ApiResponse.fail(ResultCode.TOO_MANY_REQUESTS.getCode(), "请求过于频繁，请稍后再试");
  }

  /**
   * 库存调拨（从一个仓库转移到另一个仓库）
   */
  @PostMapping("/transfer")
  @Idempotent(key = "#request.transferNo", ttl = 24, unit = TimeUnit.HOURS,
              errorMessage = "重复的库存调拨请求，请勿重复提交")
  @SentinelResource(value = "inventory.transfer", blockHandler = "handleTransferBlock")
  public ApiResponse<Boolean> transferInventory(
      @RequestBody @Valid InventoryTransferRequest request) {

    LogUtils.business("inventory.transfer", "start", request);
    boolean success = inventoryService.transferInventory(request);
    LogUtils.business("inventory.transfer", success ? "success" : "fail", request);
    return ApiResponse.success(success);
  }

  public ApiResponse<Boolean> handleTransferBlock(InventoryTransferRequest request, BlockException e) {
    log.warn("Sentinel block: inventory.transfer skuId={}", request.getSkuId());
    return ApiResponse.fail(ResultCode.TOO_MANY_REQUESTS.getCode(), "请求过于频繁，请稍后再试");
  }

  /**
   * 检查库存是否充足
   */
  @GetMapping("/check")
  @Cacheable(value = "inventory", key = "#skuId + ':' + #warehouseId + ':' + #quantity",
             unless = "#result == null")
  @SentinelResource(value = "inventory.check", blockHandler = "handleCheckBlock")
  public ApiResponse<Boolean> checkStockAvailable(
      @RequestParam @NotBlank(message = "SKU ID 不能为空") String skuId,
      @RequestParam @NotBlank(message = "仓库 ID 不能为空") String warehouseId,
      @RequestParam @Positive(message = "数量必须大于0") Integer quantity) {

    boolean available = inventoryService.checkStockAvailable(skuId, warehouseId, quantity);
    return ApiResponse.success(available);
  }

  public ApiResponse<Boolean> handleCheckBlock(String skuId, String warehouseId, Integer quantity, BlockException e) {
    log.warn("Sentinel block: inventory.check skuId={}", skuId);
    return ApiResponse.fail(ResultCode.TOO_MANY_REQUESTS.getCode(), "请求过于频繁，请稍后再试");
  }

  /**
   * 获取库存统计信息
   */
  @GetMapping("/stats")
  @Cacheable(value = "inventoryStats", key = "'global'")
  @SentinelResource(value = "inventory.stats", blockHandler = "handleStatsBlock")
  public ApiResponse<InventoryStatsResponse> getInventoryStats() {
    return ApiResponse.success(inventoryService.getInventoryStats());
  }

  public ApiResponse<InventoryStatsResponse> handleStatsBlock(BlockException e) {
    log.warn("Sentinel block: inventory.stats");
    return ApiResponse.fail(ResultCode.TOO_MANY_REQUESTS.getCode(), "请求过于频繁，请稍后再试");
  }

  /**
   * 初始化库存
   */
  @PostMapping("/init")
  @Idempotent(key = "#skuId + ':' + #warehouseId", ttl = 24, unit = TimeUnit.HOURS,
              errorMessage = "重复的库存初始化请求，请勿重复提交")
  @SentinelResource(value = "inventory.init", blockHandler = "handleInitBlock")
  public ApiResponse<InventoryResponse> initInventory(
      @RequestParam @NotBlank(message = "SKU ID 不能为空") String skuId,
      @RequestParam @NotBlank(message = "仓库 ID 不能为空") String warehouseId,
      @RequestParam(required = false) Integer initialStock) {

    LogUtils.business("inventory.init", "start", new Object[]{skuId, warehouseId, initialStock});
    InventoryResponse response = inventoryService.initInventory(skuId, warehouseId, initialStock);
    LogUtils.business("inventory.init", "success", response);
    return ApiResponse.success(response);
  }

  public ApiResponse<InventoryResponse> handleInitBlock(String skuId, String warehouseId, Integer initialStock, BlockException e) {
    log.warn("Sentinel block: inventory.init skuId={}", skuId);
    return ApiResponse.fail(ResultCode.TOO_MANY_REQUESTS.getCode(), "请求过于频繁，请稍后再试");
  }
}
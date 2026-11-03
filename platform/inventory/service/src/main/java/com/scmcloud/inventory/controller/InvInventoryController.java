package com.scmcloud.inventory.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
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
import com.scmcloud.inventory.service.command.InvInventoryCommandService;
import com.scmcloud.inventory.service.query.InvInventoryQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
 *   <li>限流由全局 {@code SentinelAutoConfiguration.blockExceptionHandler} 统一处理</li>
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
@Tag(name = "库存管理", description = "提供库存查询、调整、调拨与初始化等库存管理接口。")
public class InvInventoryController {
  private final InvInventoryQueryService queryService;
  private final InvInventoryCommandService commandService;

  /**
   * 查询单个SKU在指定仓库的库存
   */
  @Operation(summary = "查询单仓库存", description = "查询指定SKU在指定仓库中的当前库存信息。")
  @GetMapping
  @Cacheable(value = "inventory", key = "#skuId + ':' + #warehouseId",
             unless = "#result == null || #result.data() == null")
  @SentinelResource("inventory.get")
  public ApiResponse<InventoryResponse> getInventory(
      @RequestParam @NotBlank(message = "SKU ID 不能为空") String skuId,
      @RequestParam @NotBlank(message = "仓库 ID 不能为空") String warehouseId) {

    InventoryResponse response = queryService.getInventory(skuId, warehouseId);
    return response == null
        ? ApiResponse.fail(ResultCode.NOT_FOUND.getCode(), "库存不存在")
        : ApiResponse.success(response);
  }

  /**
   * 批量查询库存
   */
  @Operation(summary = "批量查询库存", description = "根据SKU列表批量查询库存，可限定仓库，单次最多查询100个SKU。")
  @PostMapping("/batch")
  @Cacheable(value = "inventory",
             key = "(#warehouseId ?: 'ALL') + ':' + (#skuIds?.toString() ?: '')",
             unless = "#result == null")
  @SentinelResource("inventory.batchGet")
  public ApiResponse<List<InventoryResponse>> batchGetInventory(
      @RequestBody @NotEmpty(message = "SKU ID 列表不能为空")
      @Size(max = 100, message = "批量查询 SKU 数量不能超过 100") List<String> skuIds,
      @Parameter(description = "仓库ID，可选，不传时查询全部仓库的库存")
      @RequestParam(required = false) String warehouseId) {

    List<InventoryResponse> responses = queryService.batchGetInventory(skuIds, warehouseId);
    return ApiResponse.success(responses);
  }

  /**
   * 分页查询库存（支持多种过滤条件）
   */
  @Operation(summary = "分页查询库存", description = "按请求中的多种过滤条件分页查询库存记录。")
  @PostMapping("/query")
  @SentinelResource("inventory.query")
  public ApiResponse<Page<InventoryResponse>> queryInventory(
      @RequestBody @Valid InventoryQueryRequest request) {

    Page<InventoryResponse> page = queryService.queryInventory(request);
    return ApiResponse.success(page);
  }

  /**
   * 调整库存（入库、出库、盘点调整等）
   */
  @Operation(summary = "调整库存", description = "对库存执行入库、出库或盘点调整，请求按业务单号做幂等保护。")
  @PostMapping("/adjust")
  @Idempotent(key = "#request.referenceNo", ttl = 24, unit = TimeUnit.HOURS,
              errorMessage = "重复的库存调整请求，请勿重复提交")
  @SentinelResource("inventory.adjust")
  public ApiResponse<InventoryResponse> adjustInventory(
      @RequestBody @Valid InventoryAdjustRequest request) {

    LogUtils.business("inventory.adjust", "start", request);
    InventoryResponse response = commandService.adjustInventory(request);
    LogUtils.business("inventory.adjust", "success", response);
    return ApiResponse.success(response);
  }

  /**
   * 库存调拨（从一个仓库转移到另一个仓库）
   */
  @Operation(summary = "库存调拨", description = "将库存从一个仓库调拨到另一个仓库，请求按调拨单号做幂等保护。")
  @PostMapping("/transfer")
  @Idempotent(key = "#request.transferNo", ttl = 24, unit = TimeUnit.HOURS,
              errorMessage = "重复的库存调拨请求，请勿重复提交")
  @SentinelResource("inventory.transfer")
  public ApiResponse<Boolean> transferInventory(
      @RequestBody @Valid InventoryTransferRequest request) {

    LogUtils.business("inventory.transfer", "start", request);
    boolean success = commandService.transferInventory(request);
    LogUtils.business("inventory.transfer", success ? "success" : "fail", request);
    return ApiResponse.success(success);
  }

  /**
   * 检查库存是否充足
   */
  @Operation(summary = "检查库存是否充足", description = "判断指定SKU在指定仓库的可用库存是否满足所需数量。")
  @GetMapping("/check")
  @Cacheable(value = "inventory", key = "#skuId + ':' + #warehouseId + ':' + #quantity",
             unless = "#result == null")
  @SentinelResource("inventory.check")
  public ApiResponse<Boolean> checkStockAvailable(
      @RequestParam @NotBlank(message = "SKU ID 不能为空") String skuId,
      @RequestParam @NotBlank(message = "仓库 ID 不能为空") String warehouseId,
      @RequestParam @Positive(message = "数量必须大于0") Integer quantity) {

    boolean available = queryService.checkStockAvailable(skuId, warehouseId, quantity);
    return ApiResponse.success(available);
  }

  /**
   * 获取库存统计信息
   */
  @Operation(summary = "查询库存统计", description = "获取全局库存统计信息，用于总览与监控。")
  @GetMapping("/stats")
  @Cacheable(value = "inventoryStats", key = "'global'")
  @SentinelResource("inventory.stats")
  public ApiResponse<InventoryStatsResponse> getInventoryStats() {
    return ApiResponse.success(queryService.getInventoryStats());
  }

  /**
   * 初始化库存
   */
  @Operation(summary = "初始化库存", description = "为指定SKU和仓库初始化库存记录，可指定初始库存数量，重复请求会被幂等拦截。")
  @PostMapping("/init")
  @Idempotent(key = "#skuId + ':' + #warehouseId", ttl = 24, unit = TimeUnit.HOURS,
              errorMessage = "重复的库存初始化请求，请勿重复提交")
  @SentinelResource("inventory.init")
  public ApiResponse<InventoryResponse> initInventory(
      @RequestParam @NotBlank(message = "SKU ID 不能为空") String skuId,
      @RequestParam @NotBlank(message = "仓库 ID 不能为空") String warehouseId,
      @Parameter(description = "初始库存数量，可选，不传时按默认值初始化")
      @RequestParam(required = false) Integer initialStock) {

    LogUtils.business("inventory.init", "start", new Object[]{skuId, warehouseId, initialStock});
    InventoryResponse response = commandService.initInventory(skuId, warehouseId, initialStock);
    LogUtils.business("inventory.init", "success", response);
    return ApiResponse.success(response);
  }
}
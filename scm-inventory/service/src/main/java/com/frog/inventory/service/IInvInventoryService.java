package com.frog.inventory.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.frog.inventory.domain.dto.InventoryAdjustRequest;
import com.frog.inventory.domain.dto.InventoryQueryRequest;
import com.frog.inventory.domain.dto.InventoryResponse;
import com.frog.inventory.domain.dto.InventoryStatsResponse;
import com.frog.inventory.domain.dto.InventoryTransferRequest;
import com.frog.inventory.domain.entity.Inventory;

import java.util.List;

/**
 * 库存服务接口
 *
 * <p>提供库存的查询、调整、转移等核心功能
 *
 * @author SCM Platform Team
 * @since 2025-12-26
 */
public interface IInvInventoryService extends IService<Inventory> {

  /**
   * 查询单个SKU在指定仓库的库存
   *
   * @param skuId SKU ID
   * @param warehouseId 仓库 ID
   * @return 库存信息（如果不存在则返回null）
   */
  InventoryResponse getInventory(String skuId, String warehouseId);

  /**
   * 批量查询库存（根据SKU ID列表）
   *
   * @param skuIds SKU ID列表
   * @param warehouseId 仓库 ID（可选，不指定则查询所有仓库）
   * @return 库存列表
   */
  List<InventoryResponse> batchGetInventory(List<String> skuIds, String warehouseId);

  /**
   * 分页查询库存（支持多种过滤条件）
   *
   * @param request 查询请求
   * @return 分页结果
   */
  Page<InventoryResponse> queryInventory(InventoryQueryRequest request);

  /**
   * 调整库存（入库、出库、盘点调整等）
   *
   * @param request 调整请求
   * @return 调整后的库存信息
   */
  InventoryResponse adjustInventory(InventoryAdjustRequest request);

  /**
   * 库存调拨（从一个仓库转移到另一个仓库）
   *
   * @param request 调拨请求
   * @return 是否调拨成功
   */
  boolean transferInventory(InventoryTransferRequest request);

  /**
   * 检查库存是否充足
   *
   * @param skuId SKU ID
   * @param warehouseId 仓库 ID
   * @param quantity 需要的数量
   * @return true-库存充足，false-库存不足
   */
  boolean checkStockAvailable(String skuId, String warehouseId, Integer quantity);

  /**
   * 获取库存统计信息
   *
   * @return 统计结果
   */
  InventoryStatsResponse getInventoryStats();

  /**
   * 初始化库存（如果不存在则创建）
   *
   * @param skuId SKU ID
   * @param warehouseId 仓库 ID
   * @param initialStock 初始库存（可选，默认为0）
   * @return 库存信息
   */
  InventoryResponse initInventory(String skuId, String warehouseId, Integer initialStock);
}
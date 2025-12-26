package com.frog.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.frog.inventory.domain.dto.InventoryAdjustRequest;
import com.frog.inventory.domain.dto.InventoryQueryRequest;
import com.frog.inventory.domain.dto.InventoryResponse;
import com.frog.inventory.domain.dto.InventoryStatsResponse;
import com.frog.inventory.domain.dto.InventoryTransferRequest;
import com.frog.inventory.domain.entity.Inventory;
import com.frog.inventory.mapper.InvInventoryMapper;
import com.frog.inventory.service.IInvInventoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 库存服务实现类
 *
 * <p>实现库存的查询、调整、转移等核心功能
 *
 * @author SCM Platform Team
 * @since 2025-12-26
 */
@Slf4j
@Service
public class InvInventoryServiceImpl extends ServiceImpl<InvInventoryMapper, Inventory>
    implements IInvInventoryService {

  @Override
  public InventoryResponse getInventory(String skuId, String warehouseId) {
    log.debug("📦 查询库存: skuId={}, warehouseId={}", skuId, warehouseId);

    Inventory inventory = lambdaQuery()
        .eq(Inventory::getSkuId, skuId)
        .eq(Inventory::getWarehouseId, warehouseId)
        .eq(Inventory::getDeleted, false)
        .one();

    if (inventory == null) {
      log.warn("⚠️  库存不存在: skuId={}, warehouseId={}", skuId, warehouseId);
      return null;
    }

    return convertToResponse(inventory);
  }

  @Override
  public List<InventoryResponse> batchGetInventory(List<String> skuIds, String warehouseId) {
    if (CollectionUtils.isEmpty(skuIds)) {
      return List.of();
    }

    log.debug("📦 批量查询库存: skuIds={}, warehouseId={}", skuIds, warehouseId);

    LambdaQueryWrapper<Inventory> wrapper = Wrappers.lambdaQuery();
    wrapper.in(Inventory::getSkuId, skuIds);
    if (StringUtils.hasText(warehouseId)) {
      wrapper.eq(Inventory::getWarehouseId, warehouseId);
    }
    wrapper.eq(Inventory::getDeleted, false);

    List<Inventory> inventories = list(wrapper);

    return inventories.stream()
        .map(this::convertToResponse)
        .collect(Collectors.toList());
  }

  @Override
  public Page<InventoryResponse> queryInventory(InventoryQueryRequest request) {
    log.debug("📦 分页查询库存: request={}", request);

    LambdaQueryWrapper<Inventory> wrapper = buildQueryWrapper(request);

    Page<Inventory> page = new Page<>(request.getPage(), request.getSize());
    Page<Inventory> resultPage = page(page, wrapper);

    Page<InventoryResponse> responsePage = new Page<>();
    BeanUtils.copyProperties(resultPage, responsePage, "records");
    responsePage.setRecords(
        resultPage.getRecords().stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList())
    );

    return responsePage;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public InventoryResponse adjustInventory(InventoryAdjustRequest request) {
    log.info("📝 调整库存: request={}", request);

    Inventory inventory = lambdaQuery()
        .eq(Inventory::getSkuId, request.getSkuId())
        .eq(Inventory::getWarehouseId, request.getWarehouseId())
        .eq(Inventory::getDeleted, false)
        .one();

    if (inventory == null) {
      log.info("📝 库存不存在，创建新库存记录: skuId={}, warehouseId={}",
          request.getSkuId(), request.getWarehouseId());
      inventory = initInventoryEntity(request.getSkuId(), request.getWarehouseId(), 0);
    }

    int beforeStock = inventory.getAvailableStock();
    int afterStock = beforeStock + request.getQuantity();

    if (afterStock < 0) {
      throw new IllegalArgumentException(
          String.format("❌ 库存不足，无法扣减。当前库存: %d, 扣减数量: %d",
              beforeStock, Math.abs(request.getQuantity()))
      );
    }

    inventory.setAvailableStock(afterStock);
    inventory.setTotalStock(inventory.getTotalStock() + request.getQuantity());
    inventory.setUpdateTime(LocalDateTime.now());
    inventory.setUpdateBy(request.getOperatorId());

    if (request.getAdjustType() == 1) {
      inventory.setLastInboundAt(LocalDateTime.now());
    } else if (request.getAdjustType() == 2) {
      inventory.setLastOutboundAt(LocalDateTime.now());
    }

    boolean success = saveOrUpdate(inventory);

    if (!success) {
      throw new RuntimeException("❌ 库存调整失败");
    }

    log.info("✅ 库存调整成功: skuId={}, warehouseId={}, before={}, after={}",
        request.getSkuId(), request.getWarehouseId(), beforeStock, afterStock);

    return convertToResponse(inventory);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public boolean transferInventory(InventoryTransferRequest request) {
    log.info("🔄 库存调拨: request={}", request);

    if (request.getFromWarehouseId().equals(request.getToWarehouseId())) {
      throw new IllegalArgumentException("❌ 源仓库和目标仓库不能相同");
    }

    InventoryAdjustRequest deductRequest = new InventoryAdjustRequest();
    deductRequest.setSkuId(request.getSkuId());
    deductRequest.setWarehouseId(request.getFromWarehouseId());
    deductRequest.setQuantity(-request.getQuantity());
    deductRequest.setAdjustType(7);
    deductRequest.setReferenceNo(request.getTransferNo());
    deductRequest.setOperatorId(request.getOperatorId());
    deductRequest.setOperatorName(request.getOperatorName());
    deductRequest.setRemark("调拨出库: " + request.getRemark());

    adjustInventory(deductRequest);

    InventoryAdjustRequest addRequest = new InventoryAdjustRequest();
    addRequest.setSkuId(request.getSkuId());
    addRequest.setWarehouseId(request.getToWarehouseId());
    addRequest.setQuantity(request.getQuantity());
    addRequest.setAdjustType(7);
    addRequest.setReferenceNo(request.getTransferNo());
    addRequest.setOperatorId(request.getOperatorId());
    addRequest.setOperatorName(request.getOperatorName());
    addRequest.setRemark("调拨入库: " + request.getRemark());

    adjustInventory(addRequest);

    log.info("✅ 库存调拨成功: skuId={}, from={}, to={}, quantity={}",
        request.getSkuId(), request.getFromWarehouseId(),
        request.getToWarehouseId(), request.getQuantity());

    return true;
  }

  @Override
  public boolean checkStockAvailable(String skuId, String warehouseId, Integer quantity) {
    Inventory inventory = lambdaQuery()
        .eq(Inventory::getSkuId, skuId)
        .eq(Inventory::getWarehouseId, warehouseId)
        .eq(Inventory::getDeleted, false)
        .one();

    if (inventory == null) {
      log.warn("⚠️  库存不存在: skuId={}, warehouseId={}", skuId, warehouseId);
      return false;
    }

    boolean available = inventory.getAvailableStock() >= quantity;
    log.debug("📦 库存检查: skuId={}, warehouseId={}, required={}, available={}, result={}",
        skuId, warehouseId, quantity, inventory.getAvailableStock(), available);

    return available;
  }

  @Override
  public InventoryStatsResponse getInventoryStats() {
    log.debug("📊 获取库存统计信息");

    List<Inventory> allInventories = lambdaQuery()
        .eq(Inventory::getDeleted, false)
        .list();

    InventoryStatsResponse stats = new InventoryStatsResponse();
    stats.setTotalSkuCount((long) allInventories.stream()
        .map(Inventory::getSkuId)
        .distinct()
        .count());
    stats.setTotalWarehouseCount((long) allInventories.stream()
        .map(Inventory::getWarehouseId)
        .distinct()
        .count());
    stats.setTotalStockQuantity(allInventories.stream()
        .mapToLong(Inventory::getTotalStock)
        .sum());
    stats.setAvailableStockQuantity(allInventories.stream()
        .mapToLong(Inventory::getAvailableStock)
        .sum());
    stats.setLockedStockQuantity(allInventories.stream()
        .mapToLong(Inventory::getLockedStock)
        .sum());
    stats.setDamagedStockQuantity(allInventories.stream()
        .mapToLong(Inventory::getDamagedStock)
        .sum());
    stats.setTotalStockValue(allInventories.stream()
        .map(inv -> inv.getAverageCost().multiply(BigDecimal.valueOf(inv.getTotalStock())))
        .reduce(BigDecimal.ZERO, BigDecimal::add));

    stats.setOutOfStockCount(allInventories.stream()
        .filter(inv -> inv.getAvailableStock() == 0)
        .count());
    stats.setLowStockCount(allInventories.stream()
        .filter(inv -> inv.getAvailableStock() > 0 &&
            inv.getAvailableStock() <= inv.getSafetyStock())
        .count());
    stats.setNormalStockCount(allInventories.stream()
        .filter(inv -> inv.getAvailableStock() > inv.getSafetyStock())
        .count());

    log.debug("📊 库存统计结果: {}", stats);
    return stats;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public InventoryResponse initInventory(String skuId, String warehouseId, Integer initialStock) {
    log.info("🆕 初始化库存: skuId={}, warehouseId={}, initialStock={}",
        skuId, warehouseId, initialStock);

    Inventory existing = lambdaQuery()
        .eq(Inventory::getSkuId, skuId)
        .eq(Inventory::getWarehouseId, warehouseId)
        .eq(Inventory::getDeleted, false)
        .one();

    if (existing != null) {
      log.warn("⚠️  库存已存在，返回现有库存: skuId={}, warehouseId={}", skuId, warehouseId);
      return convertToResponse(existing);
    }

    Inventory inventory = initInventoryEntity(skuId, warehouseId,
        initialStock != null ? initialStock : 0);

    boolean success = save(inventory);
    if (!success) {
      throw new RuntimeException("❌ 初始化库存失败");
    }

    log.info("✅ 库存初始化成功: id={}, skuId={}, warehouseId={}",
        inventory.getId(), skuId, warehouseId);

    return convertToResponse(inventory);
  }

  // ==================== Private Methods ====================

  /**
   * 构建查询条件
   */
  private LambdaQueryWrapper<Inventory> buildQueryWrapper(InventoryQueryRequest request) {
    LambdaQueryWrapper<Inventory> wrapper = Wrappers.lambdaQuery();

    if (!CollectionUtils.isEmpty(request.getSkuIds())) {
      wrapper.in(Inventory::getSkuId, request.getSkuIds());
    }

    if (!CollectionUtils.isEmpty(request.getWarehouseIds())) {
      wrapper.in(Inventory::getWarehouseId, request.getWarehouseIds());
    }

    if (StringUtils.hasText(request.getLocationCode())) {
      wrapper.eq(Inventory::getLocationCode, request.getLocationCode());
    }

    if (request.getMinAvailableStock() != null) {
      wrapper.ge(Inventory::getAvailableStock, request.getMinAvailableStock());
    }
    if (request.getMaxAvailableStock() != null) {
      wrapper.le(Inventory::getAvailableStock, request.getMaxAvailableStock());
    }

    if (Boolean.TRUE.equals(request.getOnlyInStock())) {
      wrapper.gt(Inventory::getAvailableStock, 0);
    }

    if (StringUtils.hasText(request.getStockStatus())) {
      switch (request.getStockStatus()) {
        case "OUT_OF_STOCK" -> wrapper.eq(Inventory::getAvailableStock, 0);
        case "LOW_STOCK" ->
            wrapper.apply("available_stock > 0 AND available_stock <= safety_stock");
        case "NORMAL" -> wrapper.apply("available_stock > safety_stock");
      }
    }

    wrapper.eq(Inventory::getDeleted, false);

    if (StringUtils.hasText(request.getSortBy())) {
      boolean isAsc = "ASC".equalsIgnoreCase(request.getSortOrder());
      switch (request.getSortBy()) {
        case "available_stock" -> wrapper.orderBy(true, isAsc, Inventory::getAvailableStock);
        case "total_stock" -> wrapper.orderBy(true, isAsc, Inventory::getTotalStock);
        case "update_time" -> wrapper.orderBy(true, isAsc, Inventory::getUpdateTime);
        default -> wrapper.orderByDesc(Inventory::getUpdateTime);
      }
    } else {
      wrapper.orderByDesc(Inventory::getUpdateTime);
    }

    return wrapper;
  }

  /**
   * 创建库存实体
   */
  private Inventory initInventoryEntity(String skuId, String warehouseId, Integer initialStock) {
    Inventory inventory = new Inventory();
    inventory.setId(UUID.randomUUID().toString());
    inventory.setSkuId(skuId);
    inventory.setWarehouseId(warehouseId);
    inventory.setTotalStock(initialStock);
    inventory.setAvailableStock(initialStock);
    inventory.setLockedStock(0);
    inventory.setDamagedStock(0);
    inventory.setSafetyStock(10);
    inventory.setAverageCost(BigDecimal.ZERO);
    inventory.setVersion(0);
    inventory.setDeleted(false);
    inventory.setCreateTime(LocalDateTime.now());
    inventory.setUpdateTime(LocalDateTime.now());
    return inventory;
  }

  /**
   * 转换为响应对象
   */
  private InventoryResponse convertToResponse(Inventory inventory) {
    InventoryResponse response = new InventoryResponse();
    BeanUtils.copyProperties(inventory, response);

    if (inventory.getAvailableStock() == 0) {
      response.setStockStatus("OUT_OF_STOCK");
    } else if (inventory.getAvailableStock() <= inventory.getSafetyStock()) {
      response.setStockStatus("LOW_STOCK");
    } else {
      response.setStockStatus("NORMAL");
    }

    return response;
  }
}
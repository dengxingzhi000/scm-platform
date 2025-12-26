package com.frog.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.frog.inventory.domain.dto.InventoryReservationRequest;
import com.frog.inventory.domain.entity.Inventory;
import com.frog.inventory.lock.DistributedLock;
import com.frog.inventory.mapper.InvInventoryMapper;
import com.frog.inventory.service.IInvReservationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 库存预占服务实现
 *
 * <p>基于 Redis + MySQL 实现的库存预占机制
 *
 * @author SCM Platform Team
 * @since 2025-12-26
 */
@Slf4j
@Service
public class InvReservationServiceImpl implements IInvReservationService {

  @Autowired
  private RedisTemplate<String, Object> redisTemplate;

  @Autowired
  private InvInventoryMapper inventoryMapper;

  @Autowired
  private DistributedLock distributedLock;

  private static final String RESERVATION_KEY_PREFIX = "inventory:reservation:";
  private static final String RESERVATION_INDEX_PREFIX = "inventory:reservation:index:";
  private static final int DEFAULT_TIMEOUT_SECONDS = 900; // 15分钟

  @Override
  @Transactional(rollbackFor = Exception.class)
  public boolean reserveInventory(InventoryReservationRequest request) {
    log.info("🔵 预占库存: skuId={}, warehouseId={}, quantity={}, businessKey={}",
        request.getSkuId(), request.getWarehouseId(),
        request.getQuantity(), request.getBusinessKey());

    String reservationKey = buildReservationKey(request.getBusinessKey());

    // 1. 幂等性检查：预占是否已存在
    if (Boolean.TRUE.equals(redisTemplate.hasKey(reservationKey))) {
      log.warn("⚠️  预占已存在，幂等返回: businessKey={}", request.getBusinessKey());
      return true;
    }

    // 2. 使用分布式锁防止并发问题
    String lockKey = "reserve:" + request.getSkuId() + ":" + request.getWarehouseId();
    DistributedLock.LockHandle lock = distributedLock.tryLock(lockKey, 10, TimeUnit.SECONDS);

    if (lock == null) {
      log.error("❌ 获取分布式锁失败: lockKey={}", lockKey);
      throw new RuntimeException("系统繁忙，请稍后重试");
    }

    try {
      // 3. 查询库存（行锁）
      Inventory inventory = inventoryMapper.selectOne(
          new LambdaQueryWrapper<Inventory>()
              .eq(Inventory::getSkuId, request.getSkuId())
              .eq(Inventory::getWarehouseId, request.getWarehouseId())
              .eq(Inventory::getDeleted, false)
              .last("FOR UPDATE")
      );

      if (inventory == null) {
        log.error("❌ 库存不存在: skuId={}, warehouseId={}",
            request.getSkuId(), request.getWarehouseId());
        throw new IllegalArgumentException("商品不存在");
      }

      // 4. 检查库存是否充足
      if (inventory.getAvailableStock() < request.getQuantity()) {
        log.error("❌ 库存不足: skuId={}, available={}, required={}",
            request.getSkuId(), inventory.getAvailableStock(), request.getQuantity());
        throw new RuntimeException(
            String.format("库存不足: 可用 %d, 需要 %d",
                inventory.getAvailableStock(), request.getQuantity())
        );
      }

      // 5. 锁定库存（available_stock -> locked_stock）
      int updated = inventoryMapper.update(null,
          new LambdaUpdateWrapper<Inventory>()
              .setSql("available_stock = available_stock - " + request.getQuantity())
              .setSql("locked_stock = locked_stock + " + request.getQuantity())
              .eq(Inventory::getId, inventory.getId())
              .ge(Inventory::getAvailableStock, request.getQuantity())
      );

      if (updated == 0) {
        log.error("❌ 库存锁定失败（并发冲突）: skuId={}", request.getSkuId());
        throw new RuntimeException("库存锁定失败，请重试");
      }

      // 6. 在 Redis 中记录预占信息
      Map<String, Object> reservationData = new HashMap<>();
      reservationData.put("skuId", request.getSkuId());
      reservationData.put("warehouseId", request.getWarehouseId());
      reservationData.put("quantity", request.getQuantity());
      reservationData.put("businessKey", request.getBusinessKey());
      reservationData.put("operatorId", request.getOperatorId());
      reservationData.put("operatorName", request.getOperatorName());
      reservationData.put("remark", request.getRemark());
      reservationData.put("createTime", System.currentTimeMillis());

      int timeoutSeconds = request.getTimeoutSeconds() != null ?
          request.getTimeoutSeconds() : DEFAULT_TIMEOUT_SECONDS;

      redisTemplate.opsForHash().putAll(reservationKey, reservationData);
      redisTemplate.expire(reservationKey, timeoutSeconds, TimeUnit.SECONDS);

      // 7. 建立索引（sku:warehouse -> businessKey），方便按SKU查询预占
      String indexKey = buildIndexKey(request.getSkuId(), request.getWarehouseId());
      redisTemplate.opsForSet().add(indexKey, request.getBusinessKey());
      redisTemplate.expire(indexKey, timeoutSeconds, TimeUnit.SECONDS);

      log.info("✅ 库存预占成功: skuId={}, warehouseId={}, quantity={}, businessKey={}, timeout={}s",
          request.getSkuId(), request.getWarehouseId(),
          request.getQuantity(), request.getBusinessKey(), timeoutSeconds);

      return true;

    } finally {
      lock.release();
    }
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public boolean confirmReservation(String businessKey) {
    log.info("🟢 确认预占: businessKey={}", businessKey);

    String reservationKey = buildReservationKey(businessKey);

    // 1. 检查预占是否存在
    Map<Object, Object> reservationData = redisTemplate.opsForHash().entries(reservationKey);

    if (reservationData.isEmpty()) {
      log.warn("⚠️  预占不存在或已过期: businessKey={}", businessKey);
      return false;
    }

    String skuId = (String) reservationData.get("skuId");
    String warehouseId = (String) reservationData.get("warehouseId");
    Integer quantity = (Integer) reservationData.get("quantity");

    // 2. 使用分布式锁
    String lockKey = "confirm:" + skuId + ":" + warehouseId;
    DistributedLock.LockHandle lock = distributedLock.tryLock(lockKey, 10, TimeUnit.SECONDS);

    if (lock == null) {
      log.error("❌ 获取分布式锁失败: lockKey={}", lockKey);
      throw new RuntimeException("系统繁忙，请稍后重试");
    }

    try {
      // 3. 扣减锁定库存（locked_stock -> 扣减, total_stock -> 扣减）
      int updated = inventoryMapper.update(null,
          new LambdaUpdateWrapper<Inventory>()
              .setSql("locked_stock = locked_stock - " + quantity)
              .setSql("total_stock = total_stock - " + quantity)
              .eq(Inventory::getSkuId, skuId)
              .eq(Inventory::getWarehouseId, warehouseId)
              .ge(Inventory::getLockedStock, quantity)
      );

      if (updated == 0) {
        log.error("❌ 锁定库存不足: skuId={}, warehouseId={}, quantity={}",
            skuId, warehouseId, quantity);
        throw new RuntimeException("锁定库存不足");
      }

      // 4. 删除 Redis 预占记录
      redisTemplate.delete(reservationKey);

      // 5. 从索引中移除
      String indexKey = buildIndexKey(skuId, warehouseId);
      redisTemplate.opsForSet().remove(indexKey, businessKey);

      log.info("✅ 预占确认成功: skuId={}, warehouseId={}, quantity={}, businessKey={}",
          skuId, warehouseId, quantity, businessKey);

      return true;

    } finally {
      lock.release();
    }
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public boolean releaseReservation(String businessKey) {
    log.info("🔴 释放预占: businessKey={}", businessKey);

    String reservationKey = buildReservationKey(businessKey);

    // 1. 检查预占是否存在
    Map<Object, Object> reservationData = redisTemplate.opsForHash().entries(reservationKey);

    if (reservationData.isEmpty()) {
      log.warn("⚠️  预占不存在: businessKey={}", businessKey);
      return false;
    }

    String skuId = (String) reservationData.get("skuId");
    String warehouseId = (String) reservationData.get("warehouseId");
    Integer quantity = (Integer) reservationData.get("quantity");

    // 2. 使用分布式锁
    String lockKey = "release:" + skuId + ":" + warehouseId;
    DistributedLock.LockHandle lock = distributedLock.tryLock(lockKey, 10, TimeUnit.SECONDS);

    if (lock == null) {
      log.error("❌ 获取分布式锁失败: lockKey={}", lockKey);
      throw new RuntimeException("系统繁忙，请稍后重试");
    }

    try {
      // 3. 释放锁定库存为可用库存（locked_stock -> available_stock）
      int updated = inventoryMapper.update(null,
          new LambdaUpdateWrapper<Inventory>()
              .setSql("available_stock = available_stock + " + quantity)
              .setSql("locked_stock = locked_stock - " + quantity)
              .eq(Inventory::getSkuId, skuId)
              .eq(Inventory::getWarehouseId, warehouseId)
              .ge(Inventory::getLockedStock, quantity)
      );

      if (updated == 0) {
        log.warn("⚠️  锁定库存不足（可能已被释放）: skuId={}, warehouseId={}, quantity={}",
            skuId, warehouseId, quantity);
        // 不抛异常，继续删除 Redis 记录
      }

      // 4. 删除 Redis 预占记录
      redisTemplate.delete(reservationKey);

      // 5. 从索引中移除
      String indexKey = buildIndexKey(skuId, warehouseId);
      redisTemplate.opsForSet().remove(indexKey, businessKey);

      log.info("✅ 预占释放成功: skuId={}, warehouseId={}, quantity={}, businessKey={}",
          skuId, warehouseId, quantity, businessKey);

      return true;

    } finally {
      lock.release();
    }
  }

  @Override
  public boolean checkReservationExists(String businessKey) {
    String reservationKey = buildReservationKey(businessKey);
    return Boolean.TRUE.equals(redisTemplate.hasKey(reservationKey));
  }

  @Override
  public Integer getReservedQuantity(String businessKey) {
    String reservationKey = buildReservationKey(businessKey);
    Object quantity = redisTemplate.opsForHash().get(reservationKey, "quantity");
    return quantity != null ? (Integer) quantity : null;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public int releaseExpiredReservations() {
    log.info("🔄 开始扫描并释放过期预占");

    // 注意：Redis 的过期机制是惰性的，不会主动通知过期
    // 这里的实现是兜底机制，实际过期释放依赖 Redis 自动过期 + 监听器
    // 由于无法直接扫描所有过期的 key，这里只是一个示例实现
    // 生产环境建议使用 Redis Keyspace Notifications 或定时任务扫描数据库中的预占记录

    log.warn("⚠️  当前实现依赖 Redis 自动过期机制，无需手动扫描");
    return 0;
  }

  /**
   * 构建预占记录的 Redis 键
   */
  private String buildReservationKey(String businessKey) {
    return RESERVATION_KEY_PREFIX + businessKey;
  }

  /**
   * 构建索引键（sku:warehouse -> Set<businessKey>）
   */
  private String buildIndexKey(String skuId, String warehouseId) {
    return RESERVATION_INDEX_PREFIX + skuId + ":" + warehouseId;
  }
}

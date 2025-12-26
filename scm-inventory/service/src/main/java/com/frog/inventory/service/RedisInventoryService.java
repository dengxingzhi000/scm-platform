package com.frog.inventory.service;

import com.frog.inventory.domain.entity.Inventory;
import com.frog.inventory.mapper.InvInventoryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * Redis 库存服务
 *
 * <p>基于 Redis 实现的高性能库存管理，使用 Lua 脚本保证库存扣减的原子性，防止超卖
 *
 * @author SCM Platform Team
 * @since 2025-12-26
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisInventoryService {
  private final RedisTemplate<String, Object> redisTemplate;
  private final InvInventoryMapper inventoryMapper;

  private static final String INVENTORY_KEY_PREFIX = "inventory:stock:";
  private static final String INVENTORY_DETAIL_KEY_PREFIX = "inventory:detail:";
  private static final long INVENTORY_CACHE_TTL = 30; // 库存缓存30秒（热数据）

  /**
   * Lua 脚本：原子性扣减库存（防超卖）
   *
   * <p>逻辑：
   * 1. 检查库存是否存在
   * 2. 检查库存是否充足
   * 3. 扣减库存
   *
   * <p>返回值：
   * - 1: 扣减成功
   * - -1: 库存不存在
   * - -2: 库存不足
   */
  private static final String DEDUCT_STOCK_LUA_SCRIPT =
      "local stock = redis.call('GET', KEYS[1])\n" +
          "if not stock then\n" +
          "    return -1\n" +  // 库存不存在
          "end\n" +
          "if tonumber(stock) < tonumber(ARGV[1]) then\n" +
          "    return -2\n" +  // 库存不足
          "end\n" +
          "redis.call('DECRBY', KEYS[1], ARGV[1])\n" +
          "return 1";  // 扣减成功

  /**
   * Lua 脚本：原子性增加库存
   *
   * <p>用于退款、取消订单等场景
   *
   * <p>返回值：
   * - 增加后的库存数量
   */
  private static final String ADD_STOCK_LUA_SCRIPT =
      "local stock = redis.call('GET', KEYS[1])\n" +
          "if not stock then\n" +
          "    redis.call('SET', KEYS[1], ARGV[1])\n" +
          "    return tonumber(ARGV[1])\n" +
          "else\n" +
          "    return redis.call('INCRBY', KEYS[1], ARGV[1])\n" +
          "end";

  /**
   * 从数据库同步库存到 Redis
   *
   * @param skuId SKU ID
   * @param warehouseId 仓库 ID
   * @return true-同步成功，false-库存不存在
   */
  public boolean syncInventoryToRedis(String skuId, String warehouseId) {
    log.debug("🔄 同步库存到 Redis: skuId={}, warehouseId={}", skuId, warehouseId);

    Inventory inventory = inventoryMapper.selectOne(
        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Inventory>()
            .eq(Inventory::getSkuId, skuId)
            .eq(Inventory::getWarehouseId, warehouseId)
            .eq(Inventory::getDeleted, false)
    );

    if (inventory == null) {
      log.warn("⚠️  同步库存失败：库存不存在: skuId={}, warehouseId={}", skuId, warehouseId);
      return false;
    }

    String stockKey = buildStockKey(skuId, warehouseId);
    String detailKey = buildDetailKey(skuId, warehouseId);

    // 缓存可用库存数量
    redisTemplate.opsForValue().set(
        stockKey,
        inventory.getAvailableStock(),
        INVENTORY_CACHE_TTL,
        TimeUnit.SECONDS
    );

    // 缓存库存详情
    redisTemplate.opsForValue().set(
        detailKey,
        inventory,
        INVENTORY_CACHE_TTL,
        TimeUnit.SECONDS
    );

    log.info("✅ 同步库存成功: skuId={}, warehouseId={}, availableStock={}",
        skuId, warehouseId, inventory.getAvailableStock());

    return true;
  }

  /**
   * 扣减库存（原子操作，防超卖）
   *
   * @param skuId SKU ID
   * @param warehouseId 仓库 ID
   * @param quantity 扣减数量
   * @return true-扣减成功，false-扣减失败（库存不足或不存在）
   */
  public boolean deductStock(String skuId, String warehouseId, int quantity) {
    if (quantity <= 0) {
      throw new IllegalArgumentException("扣减数量必须大于0");
    }

    log.debug("📉 扣减库存: skuId={}, warehouseId={}, quantity={}", skuId, warehouseId, quantity);

    String stockKey = buildStockKey(skuId, warehouseId);

    // 执行 Lua 脚本（原子操作）
    DefaultRedisScript<Long> script = new DefaultRedisScript<>();
    script.setScriptText(DEDUCT_STOCK_LUA_SCRIPT);
    script.setResultType(Long.class);

    Long result = redisTemplate.execute(
        script,
        Collections.singletonList(stockKey),
        quantity
    );

    if (result == null) {
      log.error("❌ 扣减库存异常：Lua 脚本执行失败: skuId={}, warehouseId={}",
          skuId, warehouseId);
      return false;
    }

    if (result == -1L) {
      log.warn("⚠️  扣减库存失败：库存不存在（未同步）: skuId={}, warehouseId={}",
          skuId, warehouseId);

      // 尝试从数据库同步库存
      if (syncInventoryToRedis(skuId, warehouseId)) {
        // 重试扣减
        return deductStock(skuId, warehouseId, quantity);
      }

      return false;
    }

    if (result == -2L) {
      log.warn("⚠️  扣减库存失败：库存不足: skuId={}, warehouseId={}, quantity={}",
          skuId, warehouseId, quantity);
      return false;
    }

    // 刷新缓存过期时间
    redisTemplate.expire(stockKey, INVENTORY_CACHE_TTL, TimeUnit.SECONDS);

    log.info("✅ 扣减库存成功: skuId={}, warehouseId={}, quantity={}",
        skuId, warehouseId, quantity);

    return true;
  }

  /**
   * 增加库存（原子操作）
   *
   * <p>用于退款、取消订单、补货等场景
   *
   * @param skuId SKU ID
   * @param warehouseId 仓库 ID
   * @param quantity 增加数量
   * @return 增加后的库存数量（如果成功），null（如果失败）
   */
  public Integer addStock(String skuId, String warehouseId, int quantity) {
    if (quantity <= 0) {
      throw new IllegalArgumentException("增加数量必须大于0");
    }

    log.debug("📈 增加库存: skuId={}, warehouseId={}, quantity={}", skuId, warehouseId, quantity);

    String stockKey = buildStockKey(skuId, warehouseId);

    // 执行 Lua 脚本（原子操作）
    DefaultRedisScript<Long> script = new DefaultRedisScript<>();
    script.setScriptText(ADD_STOCK_LUA_SCRIPT);
    script.setResultType(Long.class);

    Long result = redisTemplate.execute(
        script,
        Collections.singletonList(stockKey),
        quantity
    );

    if (result == null) {
      log.error("❌ 增加库存异常：Lua 脚本执行失败: skuId={}, warehouseId={}",
          skuId, warehouseId);
      return null;
    }

    // 刷新缓存过期时间
    redisTemplate.expire(stockKey, INVENTORY_CACHE_TTL, TimeUnit.SECONDS);

    log.info("✅ 增加库存成功: skuId={}, warehouseId={}, quantity={}, afterStock={}",
        skuId, warehouseId, quantity, result);

    return result.intValue();
  }

  /**
   * 获取库存数量（从 Redis 缓存）
   *
   * @param skuId SKU ID
   * @param warehouseId 仓库 ID
   * @return 库存数量（如果缓存不存在则返回 null）
   */
  public Integer getStock(String skuId, String warehouseId) {
    String stockKey = buildStockKey(skuId, warehouseId);
    Object stock = redisTemplate.opsForValue().get(stockKey);

    if (stock == null) {
      log.debug("⚠️  库存缓存未命中: skuId={}, warehouseId={}", skuId, warehouseId);
      return null;
    }

    return Integer.parseInt(stock.toString());
  }

  /**
   * 检查库存是否充足
   *
   * @param skuId SKU ID
   * @param warehouseId 仓库 ID
   * @param requiredQuantity 需要的数量
   * @return true-库存充足，false-库存不足或不存在
   */
  public boolean checkStock(String skuId, String warehouseId, int requiredQuantity) {
    Integer stock = getStock(skuId, warehouseId);

    if (stock == null) {
      log.debug("⚠️  检查库存：缓存未命中，尝试从数据库同步: skuId={}, warehouseId={}",
          skuId, warehouseId);

      // 从数据库同步
      if (syncInventoryToRedis(skuId, warehouseId)) {
        stock = getStock(skuId, warehouseId);
      }
    }

    if (stock == null) {
      log.warn("⚠️  检查库存失败：库存不存在: skuId={}, warehouseId={}", skuId, warehouseId);
      return false;
    }

    boolean sufficient = stock >= requiredQuantity;

    log.debug("📦 检查库存: skuId={}, warehouseId={}, required={}, available={}, sufficient={}",
        skuId, warehouseId, requiredQuantity, stock, sufficient);

    return sufficient;
  }

  /**
   * 删除库存缓存
   *
   * <p>用于库存调整后，强制下次查询时从数据库同步最新数据
   *
   * @param skuId SKU ID
   * @param warehouseId 仓库 ID
   */
  public void deleteCache(String skuId, String warehouseId) {
    String stockKey = buildStockKey(skuId, warehouseId);
    String detailKey = buildDetailKey(skuId, warehouseId);

    redisTemplate.delete(stockKey);
    redisTemplate.delete(detailKey);

    log.debug("🗑️  删除库存缓存: skuId={}, warehouseId={}", skuId, warehouseId);
  }

  /**
   * 预热库存缓存（批量同步）
   *
   * <p>用于系统启动或流量高峰前，批量预热热门商品的库存数据
   *
   * @param skuId SKU ID
   * @param warehouseId 仓库 ID
   */
  public void warmUpCache(String skuId, String warehouseId) {
    syncInventoryToRedis(skuId, warehouseId);
  }

  /**
   * 构建库存数量缓存键
   */
  private String buildStockKey(String skuId, String warehouseId) {
    return INVENTORY_KEY_PREFIX + skuId + ":" + warehouseId;
  }

  /**
   * 构建库存详情缓存键
   */
  private String buildDetailKey(String skuId, String warehouseId) {
    return INVENTORY_DETAIL_KEY_PREFIX + skuId + ":" + warehouseId;
  }
}

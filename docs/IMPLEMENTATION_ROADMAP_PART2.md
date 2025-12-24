# SCM Platform 实施路线图（续）- Phase 3-6

> 这是 IMPLEMENTATION_ROADMAP.md 的续篇，包含 Phase 3-6 的详细实施计划

---

## Phase 3: 库存服务高并发实现（第 6-7 周）

### 🎯 阶段目标
实现高性能库存服务，支持 Redis Lua 原子扣减、库存预占与释放、分布式锁，达到 TPS > 10000 的并发能力。

### 📋 具体实施任务

#### Task 3.1: Redis Lua 原子库存扣减（2 天）

**实施内容**:

**1. Lua 脚本定义**:
```java
package com.frog.inventory.service.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisInventoryService {

    private final StringRedisTemplate redisTemplate;

    private DefaultRedisScript<Long> deductScript;
    private DefaultRedisScript<Long> reserveScript;
    private DefaultRedisScript<Long> releaseScript;

    @PostConstruct
    public void init() {
        // 加载库存扣减脚本
        deductScript = new DefaultRedisScript<>();
        deductScript.setScriptSource(
            new ResourceScriptSource(new ClassPathResource("lua/deduct_stock.lua"))
        );
        deductScript.setResultType(Long.class);

        // 加载库存预占脚本
        reserveScript = new DefaultRedisScript<>();
        reserveScript.setScriptSource(
            new ResourceScriptSource(new ClassPathResource("lua/reserve_stock.lua"))
        );
        reserveScript.setResultType(Long.class);

        // 加载库存释放脚本
        releaseScript = new DefaultRedisScript<>();
        releaseScript.setScriptSource(
            new ResourceScriptSource(new ClassPathResource("lua/release_stock.lua"))
        );
        releaseScript.setResultType(Long.class);
    }

    /**
     * 原子扣减库存
     *
     * @param skuId SKU ID
     * @param quantity 扣减数量
     * @return 扣减后的库存，-1 表示库存不足
     */
    public Long deductStock(UUID skuId, Integer quantity) {
        String key = "stock:" + skuId;

        Long result = redisTemplate.execute(
            deductScript,
            Collections.singletonList(key),
            String.valueOf(quantity)
        );

        if (result == null || result < 0) {
            log.warn("库存不足: skuId={}, 需要={}", skuId, quantity);
            return -1L;
        }

        log.info("库存扣减成功: skuId={}, 扣减={}, 剩余={}",
                 skuId, quantity, result);
        return result;
    }

    /**
     * 库存预占（用于订单超时自动释放）
     *
     * @param skuId SKU ID
     * @param quantity 预占数量
     * @param orderId 订单 ID
     * @param ttl 预占有效期（秒）
     * @return 是否预占成功
     */
    public boolean reserveStock(UUID skuId, Integer quantity, UUID orderId, int ttl) {
        String stockKey = "stock:" + skuId;
        String reserveKey = "stock:reserve:" + orderId;

        Long result = redisTemplate.execute(
            reserveScript,
            Arrays.asList(stockKey, reserveKey),
            String.valueOf(quantity),
            String.valueOf(ttl)
        );

        boolean success = result != null && result == 1;

        if (success) {
            log.info("库存预占成功: skuId={}, orderId={}, quantity={}, ttl={}s",
                     skuId, orderId, quantity, ttl);
        } else {
            log.warn("库存预占失败: skuId={}, orderId={}, quantity={}",
                     skuId, orderId, quantity);
        }

        return success;
    }

    /**
     * 释放预占库存
     *
     * @param skuId SKU ID
     * @param orderId 订单 ID
     * @return 是否释放成功
     */
    public boolean releaseStock(UUID skuId, UUID orderId) {
        String stockKey = "stock:" + skuId;
        String reserveKey = "stock:reserve:" + orderId;

        Long result = redisTemplate.execute(
            releaseScript,
            Arrays.asList(stockKey, reserveKey)
        );

        boolean success = result != null && result == 1;

        if (success) {
            log.info("库存释放成功: skuId={}, orderId={}", skuId, orderId);
        } else {
            log.warn("库存释放失败: skuId={}, orderId={}", skuId, orderId);
        }

        return success;
    }

    /**
     * 从数据库同步库存到 Redis
     */
    public void syncStockToRedis(UUID skuId, Integer quantity) {
        String key = "stock:" + skuId;
        redisTemplate.opsForValue().set(key, String.valueOf(quantity));
        log.info("库存同步到 Redis: skuId={}, quantity={}", skuId, quantity);
    }

    /**
     * 获取当前库存
     */
    public Integer getStock(UUID skuId) {
        String key = "stock:" + skuId;
        String value = redisTemplate.opsForValue().get(key);
        return value != null ? Integer.parseInt(value) : 0;
    }
}
```

**2. Lua 脚本文件**:

`resources/lua/deduct_stock.lua`:
```lua
-- 原子扣减库存
-- KEYS[1]: stock:skuId
-- ARGV[1]: 扣减数量

local key = KEYS[1]
local quantity = tonumber(ARGV[1])

-- 获取当前库存
local stock = tonumber(redis.call('GET', key) or '0')

-- 检查库存是否足够
if stock >= quantity then
    -- 扣减库存
    redis.call('DECRBY', key, quantity)
    -- 返回剩余库存
    return stock - quantity
else
    -- 库存不足，返回 -1
    return -1
end
```

`resources/lua/reserve_stock.lua`:
```lua
-- 库存预占（带超时自动释放）
-- KEYS[1]: stock:skuId
-- KEYS[2]: stock:reserve:orderId
-- ARGV[1]: 预占数量
-- ARGV[2]: 过期时间（秒）

local stockKey = KEYS[1]
local reserveKey = KEYS[2]
local quantity = tonumber(ARGV[1])
local ttl = tonumber(ARGV[2])

-- 检查是否已经预占过
if redis.call('EXISTS', reserveKey) == 1 then
    -- 已预占，返回失败
    return 0
end

-- 获取当前库存
local stock = tonumber(redis.call('GET', stockKey) or '0')

-- 检查库存是否足够
if stock >= quantity then
    -- 扣减库存
    redis.call('DECRBY', stockKey, quantity)

    -- 记录预占信息（包含预占数量和 SKU ID）
    redis.call('HMSET', reserveKey, 'skuId', stockKey, 'quantity', quantity)

    -- 设置过期时间（订单超时自动释放）
    redis.call('EXPIRE', reserveKey, ttl)

    return 1
else
    return 0
end
```

`resources/lua/release_stock.lua`:
```lua
-- 释放预占库存
-- KEYS[1]: stock:skuId
-- KEYS[2]: stock:reserve:orderId

local stockKey = KEYS[1]
local reserveKey = KEYS[2]

-- 检查预占记录是否存在
if redis.call('EXISTS', reserveKey) == 0 then
    -- 预占记录不存在（可能已过期），返回失败
    return 0
end

-- 获取预占数量
local quantity = tonumber(redis.call('HGET', reserveKey, 'quantity'))

-- 恢复库存
redis.call('INCRBY', stockKey, quantity)

-- 删除预占记录
redis.call('DEL', reserveKey)

return 1
```

**3. 集成测试**:
```java
@SpringBootTest
public class RedisInventoryServiceTest {

    @Autowired
    private RedisInventoryService redisInventoryService;

    @Test
    public void testDeductStock() {
        UUID skuId = UUID.randomUUID();

        // 初始化库存 100
        redisInventoryService.syncStockToRedis(skuId, 100);

        // 扣减 30
        Long result = redisInventoryService.deductStock(skuId, 30);
        assertEquals(70L, result);

        // 扣减 50
        result = redisInventoryService.deductStock(skuId, 50);
        assertEquals(20L, result);

        // 扣减 30（库存不足）
        result = redisInventoryService.deductStock(skuId, 30);
        assertEquals(-1L, result);
    }

    @Test
    public void testReserveAndRelease() throws InterruptedException {
        UUID skuId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        // 初始化库存 100
        redisInventoryService.syncStockToRedis(skuId, 100);

        // 预占 30，有效期 5 秒
        boolean reserved = redisInventoryService.reserveStock(skuId, 30, orderId, 5);
        assertTrue(reserved);
        assertEquals(70, redisInventoryService.getStock(skuId));

        // 重复预占应该失败
        reserved = redisInventoryService.reserveStock(skuId, 30, orderId, 5);
        assertFalse(reserved);

        // 释放库存
        boolean released = redisInventoryService.releaseStock(skuId, orderId);
        assertTrue(released);
        assertEquals(100, redisInventoryService.getStock(skuId));

        // 重复释放应该失败
        released = redisInventoryService.releaseStock(skuId, orderId);
        assertFalse(released);
    }

    @Test
    public void testReserveExpiration() throws InterruptedException {
        UUID skuId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        // 初始化库存 100
        redisInventoryService.syncStockToRedis(skuId, 100);

        // 预占 30，有效期 2 秒
        redisInventoryService.reserveStock(skuId, 30, orderId, 2);
        assertEquals(70, redisInventoryService.getStock(skuId));

        // 等待 3 秒让预占过期
        Thread.sleep(3000);

        // 预占记录应该已过期，释放应该失败
        boolean released = redisInventoryService.releaseStock(skuId, orderId);
        assertFalse(released);

        // 库存仍然是 70（需要定时任务清理过期预占）
        assertEquals(70, redisInventoryService.getStock(skuId));
    }
}
```

**验收标准**:
- ✅ Lua 脚本执行成功，原子性保证
- ✅ 并发扣减测试：1000 并发请求，库存准确率 100%
- ✅ 性能测试：单 SKU 扣减 TPS > 10000
- ✅ 库存预占成功，过期自动失效
- ✅ 库存释放正确恢复
- ✅ 幂等性保证：重复扣减、预占、释放不会重复执行

---

#### Task 3.2: 分布式锁实现（1 天）

**实施内容**:

**Redis 分布式锁（已在 common/data 模块实现）**:
```java
package com.frog.inventory.service;

import com.frog.common.redis.lock.DistributedLock;
import com.frog.common.redis.lock.LockHandle;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryLockService {

    private final DistributedLock distributedLock;
    private final InventoryMapper inventoryMapper;

    /**
     * 使用分布式锁保护库存扣减
     *
     * 适用场景：需要先查询再更新的业务逻辑
     */
    public boolean deductStockWithLock(UUID skuId, Integer quantity) {
        String lockKey = "lock:inventory:" + skuId;

        try (LockHandle lock = distributedLock.acquire(
            lockKey,
            Duration.ofSeconds(10), // 锁超时时间
            Duration.ofSeconds(3)   // 等待获取锁的时间
        )) {
            log.info("获取分布式锁成功: lockKey={}", lockKey);

            // 在锁保护下执行业务逻辑
            Inventory inventory = inventoryMapper.selectBySkuId(skuId);

            if (inventory == null) {
                throw new BusinessException("SKU 不存在");
            }

            if (inventory.getAvailableQuantity() < quantity) {
                log.warn("库存不足: available={}, required={}",
                         inventory.getAvailableQuantity(), quantity);
                return false;
            }

            // 扣减库存
            int updated = inventoryMapper.deductStock(skuId, quantity);

            if (updated > 0) {
                log.info("库存扣减成功: skuId={}, quantity={}", skuId, quantity);
                return true;
            } else {
                log.warn("库存扣减失败: skuId={}", skuId);
                return false;
            }

        } catch (Exception e) {
            log.error("库存扣减异常: skuId=" + skuId, e);
            return false;
        }
    }

    /**
     * 库存调拨（跨仓库转移）- 需要锁定两个仓库的库存
     */
    public boolean transferStock(
        UUID skuId,
        UUID fromWarehouseId,
        UUID toWarehouseId,
        Integer quantity
    ) {
        // 按 ID 排序避免死锁
        String lockKey1, lockKey2;
        if (fromWarehouseId.compareTo(toWarehouseId) < 0) {
            lockKey1 = "lock:warehouse:" + fromWarehouseId + ":sku:" + skuId;
            lockKey2 = "lock:warehouse:" + toWarehouseId + ":sku:" + skuId;
        } else {
            lockKey1 = "lock:warehouse:" + toWarehouseId + ":sku:" + skuId;
            lockKey2 = "lock:warehouse:" + fromWarehouseId + ":sku:" + skuId;
        }

        try (LockHandle lock1 = distributedLock.acquire(lockKey1, Duration.ofSeconds(10));
             LockHandle lock2 = distributedLock.acquire(lockKey2, Duration.ofSeconds(10))) {

            log.info("获取双仓库锁成功: from={}, to={}", fromWarehouseId, toWarehouseId);

            // 从源仓库扣减
            boolean deducted = inventoryMapper.deductWarehouseStock(
                skuId, fromWarehouseId, quantity
            ) > 0;

            if (!deducted) {
                log.warn("源仓库库存不足");
                return false;
            }

            // 向目标仓库增加
            inventoryMapper.addWarehouseStock(skuId, toWarehouseId, quantity);

            log.info("库存调拨成功: from={}, to={}, quantity={}",
                     fromWarehouseId, toWarehouseId, quantity);

            return true;

        } catch (Exception e) {
            log.error("库存调拨失败", e);
            return false;
        }
    }
}
```

**验收标准**:
- ✅ 分布式锁获取成功，超时自动释放
- ✅ 并发场景下库存扣减准确性 100%
- ✅ 避免死锁：按 ID 排序获取多个锁
- ✅ 锁自动续期（如果业务执行时间超过锁超时时间）
- ✅ 性能测试：锁获取延迟 < 10ms (p95)

---

#### Task 3.3: 库存服务 API 实现（2 天）

**实施内容**:

```java
package com.frog.inventory.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.UUID;

@Tag(name = "库存管理")
@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final IInventoryService inventoryService;

    @Operation(summary = "查询库存")
    @GetMapping("/{skuId}")
    public ApiResponse<InventoryDTO> getInventory(@PathVariable UUID skuId) {
        return ApiResponse.success(inventoryService.getInventory(skuId));
    }

    @Operation(summary = "扣减库存")
    @PostMapping("/deduct")
    @PreAuthorize("hasAuthority('inventory:deduct')")
    public ApiResponse<Void> deductStock(@Valid @RequestBody DeductStockRequest request) {
        inventoryService.deductStock(request);
        return ApiResponse.success();
    }

    @Operation(summary = "预占库存")
    @PostMapping("/reserve")
    public ApiResponse<Void> reserveStock(@Valid @RequestBody ReserveStockRequest request) {
        inventoryService.reserveStock(request);
        return ApiResponse.success();
    }

    @Operation(summary = "释放库存")
    @PostMapping("/release")
    public ApiResponse<Void> releaseStock(@Valid @RequestBody ReleaseStockRequest request) {
        inventoryService.releaseStock(request);
        return ApiResponse.success();
    }

    @Operation(summary = "库存调拨")
    @PostMapping("/transfer")
    @PreAuthorize("hasAuthority('inventory:transfer')")
    public ApiResponse<Void> transferStock(@Valid @RequestBody TransferStockRequest request) {
        inventoryService.transferStock(request);
        return ApiResponse.success();
    }

    @Operation(summary = "库存盘点")
    @PostMapping("/stocktaking")
    @PreAuthorize("hasAuthority('inventory:stocktaking')")
    public ApiResponse<Void> stocktaking(@Valid @RequestBody StocktakingRequest request) {
        inventoryService.stocktaking(request);
        return ApiResponse.success();
    }
}
```

**验收标准**:
- ✅ 所有 API 实现并测试通过
- ✅ 并发扣减准确性 100%
- ✅ API 响应时间 < 50ms (p95)
- ✅ Redis 和 MySQL 数据一致性 100%
- ✅ 库存流水记录完整

---

#### Task 3.4: 库存预警与补货（1 天）

**实施内容**:

```java
package com.frog.inventory.job;

import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryAlertJob {

    private final InventoryMapper inventoryMapper;
    private final NotificationService notificationService;

    /**
     * 库存预警任务
     *
     * 执行频率: 每小时一次
     * 预警规则: 库存低于安全库存的 30%
     */
    @XxlJob("inventoryAlertJob")
    public void execute() {
        log.info("开始执行库存预警任务");

        // 查询低库存商品
        List<Inventory> lowStockList = inventoryMapper.selectLowStock(0.3);

        log.info("发现低库存商品: {}", lowStockList.size());

        for (Inventory inventory : lowStockList) {
            // 发送预警通知
            String message = String.format(
                "库存告警：SKU %s 当前库存 %d，低于安全库存 %d 的 30%%",
                inventory.getSkuId(),
                inventory.getAvailableQuantity(),
                inventory.getSafetyStock()
            );

            notificationService.sendNotification(
                "admin",
                "admin@example.com",
                "inventory.alert",
                "库存预警",
                Map.of(
                    "skuId", inventory.getSkuId(),
                    "currentStock", inventory.getAvailableQuantity(),
                    "safetyStock", inventory.getSafetyStock()
                )
            );

            log.warn(message);
        }

        log.info("库存预警任务执行完成");
    }
}
```

**验收标准**:
- ✅ 库存预警任务定时执行
- ✅ 低库存商品准确识别
- ✅ 预警通知及时发送
- ✅ 预警规则可配置

---

### 📊 Phase 3 验收总结

**必须达成的指标**:
- [ ] Redis Lua 原子扣减准确率 100%
- [ ] 并发测试：1000 并发扣减，数据一致性 100%
- [ ] 性能测试：单 SKU 扣减 TPS > 10000
- [ ] 库存预占成功率 100%
- [ ] 库存释放成功率 100%
- [ ] 分布式锁获取延迟 < 10ms (p95)
- [ ] 库存 API 响应时间 < 50ms (p95)

**输出物清单**:
```
scm-platform/
├── scm-inventory/service/
│   ├── RedisInventoryService.java
│   ├── InventoryLockService.java
│   ├── InventoryServiceImpl.java
│   └── InventoryAlertJob.java
├── resources/lua/
│   ├── deduct_stock.lua
│   ├── reserve_stock.lua
│   └── release_stock.lua
├── docs/
│   ├── redis-lua-performance-test.md
│   └── distributed-lock-guide.md
└── tests/
    └── InventoryServiceConcurrencyTest.java
```

---

## Phase 4: 订单服务 + 状态机（第 8-9 周）

### 🎯 阶段目标
实现完整的订单服务，集成 Spring State Machine 实现订单状态流转，集成 Seata 保证分布式事务，达到订单创建 TPS > 10000。

### 📋 具体实施任务

#### Task 4.1: Spring State Machine 配置（2 天）

**实施内容**:

**1. 状态和事件定义**:
```java
package com.frog.order.statemachine;

/**
 * 订单状态枚举
 */
public enum OrderState {
    PENDING_PAYMENT,    // 待支付
    PAID,               // 已支付
    PENDING_SHIP,       // 待发货
    SHIPPED,            // 已发货
    IN_TRANSIT,         // 运输中
    OUT_FOR_DELIVERY,   // 派送中
    DELIVERED,          // 已送达
    COMPLETED,          // 已完成
    CANCELLED,          // 已取消
    REFUNDING,          // 退款中
    REFUNDED            // 已退款
}

/**
 * 订单事件枚举
 */
public enum OrderEvent {
    PAY,           // 支付
    SHIP,          // 发货
    RECEIVE,       // 收货
    COMPLETE,      // 完成
    CANCEL,        // 取消
    REFUND,        // 退款
    REFUND_SUCCESS // 退款成功
}
```

**2. 状态机配置**:
```java
package com.frog.order.statemachine;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.StateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineConfigurationConfigurer;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;
import org.springframework.statemachine.listener.StateMachineListenerAdapter;
import org.springframework.statemachine.state.State;

import java.util.EnumSet;

@Slf4j
@Configuration
@EnableStateMachineFactory(name = "orderStateMachineFactory")
public class OrderStateMachineConfig
    extends StateMachineConfigurerAdapter<OrderState, OrderEvent> {

    /**
     * 配置状态
     */
    @Override
    public void configure(StateMachineStateConfigurer<OrderState, OrderEvent> states)
        throws Exception {
        states
            .withStates()
            .initial(OrderState.PENDING_PAYMENT)
            .states(EnumSet.allOf(OrderState.class))
            .end(OrderState.COMPLETED)
            .end(OrderState.CANCELLED)
            .end(OrderState.REFUNDED);
    }

    /**
     * 配置状态转换
     */
    @Override
    public void configure(
        StateMachineTransitionConfigurer<OrderState, OrderEvent> transitions
    ) throws Exception {
        transitions
            // 待支付 → 已支付
            .withExternal()
                .source(OrderState.PENDING_PAYMENT)
                .target(OrderState.PAID)
                .event(OrderEvent.PAY)
                .action(context -> {
                    log.info("订单支付成功");
                    // 调用支付服务
                })
            .and()

            // 已支付 → 待发货
            .withExternal()
                .source(OrderState.PAID)
                .target(OrderState.PENDING_SHIP)
                .guard(context -> {
                    // 检查库存是否充足
                    return checkInventory(context);
                })
            .and()

            // 待发货 → 已发货
            .withExternal()
                .source(OrderState.PENDING_SHIP)
                .target(OrderState.SHIPPED)
                .event(OrderEvent.SHIP)
                .action(context -> {
                    log.info("订单已发货");
                    // 调用物流服务创建物流单
                })
            .and()

            // 已发货 → 运输中
            .withExternal()
                .source(OrderState.SHIPPED)
                .target(OrderState.IN_TRANSIT)
            .and()

            // 运输中 → 派送中
            .withExternal()
                .source(OrderState.IN_TRANSIT)
                .target(OrderState.OUT_FOR_DELIVERY)
            .and()

            // 派送中 → 已送达
            .withExternal()
                .source(OrderState.OUT_FOR_DELIVERY)
                .target(OrderState.DELIVERED)
                .action(context -> {
                    log.info("订单已送达");
                    // 发送收货提醒
                })
            .and()

            // 已送达 → 已完成
            .withExternal()
                .source(OrderState.DELIVERED)
                .target(OrderState.COMPLETED)
                .event(OrderEvent.RECEIVE)
                .action(context -> {
                    log.info("订单已完成");
                    // 增加用户积分、更新商品销量
                })
            .and()

            // 待支付 → 已取消
            .withExternal()
                .source(OrderState.PENDING_PAYMENT)
                .target(OrderState.CANCELLED)
                .event(OrderEvent.CANCEL)
                .action(context -> {
                    log.info("订单已取消");
                    // 释放库存
                })
            .and()

            // 已支付/待发货 → 退款中
            .withExternal()
                .source(OrderState.PAID)
                .target(OrderState.REFUNDING)
                .event(OrderEvent.REFUND)
                .action(context -> {
                    log.info("订单退款中");
                    // 调用支付服务退款
                })
            .and()

            .withExternal()
                .source(OrderState.PENDING_SHIP)
                .target(OrderState.REFUNDING)
                .event(OrderEvent.REFUND)
            .and()

            // 退款中 → 已退款
            .withExternal()
                .source(OrderState.REFUNDING)
                .target(OrderState.REFUNDED)
                .event(OrderEvent.REFUND_SUCCESS)
                .action(context -> {
                    log.info("订单退款成功");
                    // 恢复库存
                });
    }

    /**
     * 配置状态机监听器
     */
    @Override
    public void configure(
        StateMachineConfigurationConfigurer<OrderState, OrderEvent> config
    ) throws Exception {
        config
            .withConfiguration()
            .autoStartup(true)
            .listener(new StateMachineListenerAdapter<OrderState, OrderEvent>() {
                @Override
                public void stateChanged(State<OrderState, OrderEvent> from,
                                       State<OrderState, OrderEvent> to) {
                    log.info("订单状态变更: {} → {}",
                            from != null ? from.getId() : "INIT",
                            to.getId());
                }
            });
    }

    private boolean checkInventory(Object context) {
        // 实际业务中调用库存服务检查
        return true;
    }
}
```

**3. 状态机服务**:
```java
package com.frog.order.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.persist.StateMachinePersister;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderStateMachineService {

    private final StateMachineFactory<OrderState, OrderEvent> stateMachineFactory;
    private final StateMachinePersister<OrderState, OrderEvent, UUID> persister;
    private final OrderMapper orderMapper;

    /**
     * 发送事件触发状态转换
     */
    public boolean sendEvent(UUID orderId, OrderEvent event) {
        try {
            // 创建状态机实例
            StateMachine<OrderState, OrderEvent> stateMachine =
                stateMachineFactory.getStateMachine(orderId.toString());

            // 从数据库恢复状态
            persister.restore(stateMachine, orderId);

            // 发送事件
            Message<OrderEvent> message = MessageBuilder
                .withPayload(event)
                .setHeader("orderId", orderId)
                .build();

            boolean result = stateMachine.sendEvent(message);

            if (result) {
                // 保存新状态到数据库
                persister.persist(stateMachine, orderId);

                // 更新订单状态
                OrderState newState = stateMachine.getState().getId();
                orderMapper.updateStatus(orderId, newState.name());

                log.info("订单状态更新成功: orderId={}, event={}, newState={}",
                         orderId, event, newState);
            } else {
                log.warn("订单状态转换失败: orderId={}, event={}", orderId, event);
            }

            return result;

        } catch (Exception e) {
            log.error("订单状态机执行异常: orderId=" + orderId, e);
            return false;
        }
    }

    /**
     * 获取订单当前状态
     */
    public OrderState getOrderState(UUID orderId) {
        try {
            StateMachine<OrderState, OrderEvent> stateMachine =
                stateMachineFactory.getStateMachine(orderId.toString());
            persister.restore(stateMachine, orderId);
            return stateMachine.getState().getId();
        } catch (Exception e) {
            log.error("获取订单状态失败: orderId=" + orderId, e);
            return null;
        }
    }
}
```

**验收标准**:
- ✅ 状态机配置正确，所有状态转换有效
- ✅ 非法状态转换被拒绝（如已取消订单不能支付）
- ✅ Guard 条件生效（如库存不足拒绝发货）
- ✅ Action 正确执行（如发货时创建物流单）
- ✅ 状态持久化到数据库
- ✅ 并发状态变更准确性 100%

---

#### Task 4.2: 订单服务完整实现（3 天）

**实施内容**:

```java
package com.frog.order.service.impl;

import io.seata.spring.annotation.GlobalTransactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements IOrderService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final InventoryClient inventoryClient;
    private final PaymentClient paymentClient;
    private final OrderStateMachineService stateMachineService;

    /**
     * 创建订单 - Seata 分布式事务 + 状态机
     */
    @Override
    @GlobalTransactional(
        name = "create-order-tx",
        rollbackFor = Exception.class,
        timeoutMills = 60000
    )
    public OrderDTO createOrder(CreateOrderRequest request) {
        log.info("开始创建订单: userId={}, XID={}",
                 request.getUserId(), RootContext.getXID());

        try {
            // 1. 创建订单主记录
            Order order = new Order();
            order.setId(UUIDv7Util.generate());
            order.setOrderNo(generateOrderNo());
            order.setUserId(request.getUserId());
            order.setTotalAmount(request.getTotalAmount());
            order.setStatus(OrderState.PENDING_PAYMENT.name());
            order.setShippingAddress(request.getShippingAddress());
            orderMapper.insert(order);

            // 2. 创建订单明细
            for (OrderItemRequest item : request.getItems()) {
                OrderItem orderItem = new OrderItem();
                orderItem.setId(UUIDv7Util.generate());
                orderItem.setOrderId(order.getId());
                orderItem.setSkuId(item.getSkuId());
                orderItem.setProductName(item.getProductName());
                orderItem.setQuantity(item.getQuantity());
                orderItem.setPrice(item.getPrice());
                orderItem.setTotalAmount(
                    item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()))
                );
                orderItemMapper.insert(orderItem);
            }

            // 3. 预占库存（远程调用）
            for (OrderItemRequest item : request.getItems()) {
                ReserveStockRequest reserveRequest = ReserveStockRequest.builder()
                    .skuId(item.getSkuId())
                    .quantity(item.getQuantity())
                    .orderId(order.getId())
                    .ttl(1800) // 30 分钟
                    .build();

                ApiResponse<Void> result = inventoryClient.reserveStock(reserveRequest);

                if (!result.isSuccess()) {
                    throw new BusinessException("库存预占失败: " + item.getSkuId());
                }

                log.info("库存预占成功: skuId={}, quantity={}",
                         item.getSkuId(), item.getQuantity());
            }

            log.info("订单创建成功: orderId={}, orderNo={}",
                     order.getId(), order.getOrderNo());

            return OrderConverter.toDTO(order);

        } catch (Exception e) {
            log.error("订单创建失败，事务回滚: XID=" + RootContext.getXID(), e);
            throw e;
        }
    }

    /**
     * 支付订单
     */
    @Override
    @GlobalTransactional(rollbackFor = Exception.class)
    public void payOrder(UUID orderId, PaymentRequest paymentRequest) {
        log.info("开始支付订单: orderId={}", orderId);

        // 1. 查询订单
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }

        if (!OrderState.PENDING_PAYMENT.name().equals(order.getStatus())) {
            throw new BusinessException("订单状态不正确");
        }

        // 2. 调用支付服务
        CreatePaymentRequest request = CreatePaymentRequest.builder()
            .orderId(orderId)
            .amount(order.getTotalAmount())
            .paymentMethod(paymentRequest.getPaymentMethod())
            .build();

        ApiResponse<PaymentDTO> result = paymentClient.createPayment(request);

        if (!result.isSuccess()) {
            throw new BusinessException("支付失败");
        }

        // 3. 触发状态机 - 支付事件
        boolean stateChanged = stateMachineService.sendEvent(orderId, OrderEvent.PAY);

        if (!stateChanged) {
            throw new BusinessException("订单状态更新失败");
        }

        // 4. 确认库存扣减（从预占转为实际扣减）
        List<OrderItem> items = orderItemMapper.selectByOrderId(orderId);
        for (OrderItem item : items) {
            inventoryClient.confirmReserve(orderId, item.getSkuId());
        }

        // 5. 更新支付时间
        order.setPaymentTime(LocalDateTime.now());
        orderMapper.updateById(order);

        log.info("订单支付成功: orderId={}", orderId);
    }

    /**
     * 取消订单
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(UUID orderId, String reason) {
        log.info("开始取消订单: orderId={}, reason={}", orderId, reason);

        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }

        // 只有待支付状态可以取消
        if (!OrderState.PENDING_PAYMENT.name().equals(order.getStatus())) {
            throw new BusinessException("订单状态不允许取消");
        }

        // 触发状态机 - 取消事件
        boolean stateChanged = stateMachineService.sendEvent(orderId, OrderEvent.CANCEL);

        if (!stateChanged) {
            throw new BusinessException("订单取消失败");
        }

        // 释放预占库存
        List<OrderItem> items = orderItemMapper.selectByOrderId(orderId);
        for (OrderItem item : items) {
            inventoryClient.releaseStock(orderId, item.getSkuId());
        }

        order.setCancelTime(LocalDateTime.now());
        orderMapper.updateById(order);

        log.info("订单取消成功: orderId={}", orderId);
    }

    private String generateOrderNo() {
        return "ORD" + System.currentTimeMillis() +
               RandomStringUtils.randomNumeric(6);
    }
}
```

**验收标准**:
- ✅ 订单创建 TPS > 10000
- ✅ Seata 分布式事务成功率 100%
- ✅ 订单状态流转正确
- ✅ 库存预占 → 确认扣减流程正常
- ✅ 订单取消时库存正确释放
- ✅ 并发测试：1000 并发创建订单，数据一致性 100%

---

### 📊 Phase 4 验收总结

**必须达成的指标**:
- [ ] 订单创建 TPS > 10000
- [ ] 订单状态流转准确率 100%
- [ ] Seata 分布式事务成功率 100%
- [ ] 订单超时取消准确率 100%
- [ ] API 响应时间 < 100ms (p95)
- [ ] 并发场景数据一致性 100%

**输出物清单**:
```
scm-platform/
├── scm-order/service/
│   ├── OrderStateMachineConfig.java
│   ├── OrderStateMachineService.java
│   ├── OrderServiceImpl.java
│   └── OrderTimeoutJob.java
├── docs/
│   ├── order-state-machine-diagram.png
│   ├── order-service-performance-test.md
│   └── distributed-transaction-analysis.md
└── tests/
    └── OrderServiceConcurrencyTest.java
```

---

## Phase 5: 仓库与物流服务（第 10 周）

### 🎯 阶段目标
实现仓库管理和物流跟踪服务，完成出入库流程，集成第三方物流 API。

### 📋 具体实施任务

#### Task 5.1: 仓库服务实现（2 天）

**实施内容**:

```java
@Service
@RequiredArgsConstructor
public class WarehouseServiceImpl implements IWarehouseService {

    /**
     * 创建入库单
     */
    @Transactional
    public InboundOrderDTO createInboundOrder(CreateInboundOrderRequest request) {
        // 实现入库单创建逻辑
    }

    /**
     * 创建出库单（订单发货触发）
     */
    @Transactional
    public OutboundOrderDTO createOutboundOrder(CreateOutboundOrderRequest request) {
        // 实现出库单创建逻辑
    }

    /**
     * 库存调拨
     */
    @Transactional
    public void transferInventory(TransferInventoryRequest request) {
        // 实现跨仓库库存调拨
    }
}
```

**验收标准**:
- ✅ 入库单创建并更新库存
- ✅ 出库单创建并扣减库存
- ✅ 库存调拨流程正确
- ✅ API 响应时间 < 50ms (p95)

---

#### Task 5.2: 物流服务实现（3 天）

**实施内容**:

```java
@Service
@RequiredArgsConstructor
public class LogisticsServiceImpl implements ILogisticsService {

    private final LogisticsOrderMapper logisticsOrderMapper;
    private final LogisticsTrackMapper trackMapper;

    /**
     * 创建物流单
     */
    @Transactional
    public LogisticsOrderDTO createLogisticsOrder(CreateLogisticsOrderRequest request) {
        // 调用第三方物流 API 创建运单
        // 保存物流单信息
    }

    /**
     * 查询物流轨迹
     */
    public List<LogisticsTrackDTO> queryTrack(String trackingNo) {
        // 查询本地轨迹
        // 如果需要，调用第三方 API 同步最新轨迹
    }

    /**
     * 同步物流轨迹（定时任务）
     */
    @XxlJob("syncLogisticsTrackJob")
    public void syncTrack() {
        // 查询运输中的物流单
        // 调用第三方 API 获取最新轨迹
        // 保存到数据库
        // 如果状态变更为已送达，触发订单状态变更
    }
}
```

**验收标准**:
- ✅ 物流单创建成功
- ✅ 轨迹查询准确
- ✅ 定时同步轨迹无遗漏
- ✅ 物流状态变更触发订单状态变更

---

### 📊 Phase 5 验收总结

**必须达成的指标**:
- [ ] 仓库服务 API 响应时间 < 50ms (p95)
- [ ] 物流轨迹同步延迟 < 5 分钟
- [ ] 物流状态与订单状态联动准确率 100%

---

## Phase 6: 性能优化与压测（第 11-12 周）

### 🎯 阶段目标
全面优化系统性能，实施三级缓存、热点保护、限流降级，完成压力测试并优化至目标性能。

### 📋 具体实施任务

#### Task 6.1: 三级缓存优化（2 天）

**实施内容**:

**JVM (Caffeine) → Redis → MySQL**
```java
@Configuration
public class ThreeLevelCacheConfig {

    @Bean
    public Cache<String, Object> localCache() {
        return Caffeine.newBuilder()
            .maximumSize(10000)
            .expireAfterWrite(Duration.ofMinutes(5))
            .recordStats()
            .build();
    }
}

@Service
public class ProductCacheService {

    public ProductDTO getProduct(UUID id) {
        // L1: Caffeine
        ProductDTO product = localCache.getIfPresent("product:" + id);
        if (product != null) return product;

        // L2: Redis
        product = redisTemplate.opsForValue().get("product:" + id);
        if (product != null) {
            localCache.put("product:" + id, product);
            return product;
        }

        // L3: MySQL
        product = productMapper.selectById(id);
        if (product != null) {
            redisTemplate.opsForValue().set("product:" + id, product, 10, TimeUnit.MINUTES);
            localCache.put("product:" + id, product);
        }

        return product;
    }
}
```

**验收标准**:
- ✅ 缓存命中率 > 95%
- ✅ L1 缓存命中延迟 < 1ms
- ✅ L2 缓存命中延迟 < 5ms
- ✅ 缓存一致性保证

---

#### Task 6.2: 热点商品保护（1 天）

**实施内容**:

```java
@Service
public class HotProductProtectionService {

    @Sentinel(value = "deductStock", blockHandler = "blockHandler")
    @RateLimit(qps = 1000, warmUpPeriodSeconds = 10)
    public void deductStock(UUID skuId, Integer quantity) {
        // 热点参数限流
        // 降级策略：返回库存紧张提示
    }
}
```

**验收标准**:
- ✅ 热点商品识别准确
- ✅ 限流策略生效
- ✅ 降级响应友好

---

#### Task 6.3: JMeter 压力测试（3 天）

**测试场景**:
1. **商品搜索**: 1000 并发，QPS > 50000
2. **订单创建**: 500 并发，TPS > 10000
3. **库存扣减**: 1000 并发，TPS > 20000
4. **秒杀场景**: 10000 并发抢 100 件商品，超卖 = 0

**验收标准**:
- ✅ 网关吞吐量 > 100000 QPS
- ✅ 订单创建 TPS > 10000
- ✅ 库存扣减 TPS > 20000
- ✅ P99 延迟 < 100ms
- ✅ 错误率 < 0.1%
- ✅ CPU 使用率 < 70%
- ✅ 内存使用率 < 80%

---

### 📊 Phase 6 验收总结

**必须达成的指标**:
- [ ] 网关 QPS > 100000
- [ ] 订单创建 TPS > 10000
- [ ] 库存扣减 TPS > 20000
- [ ] 商品搜索 QPS > 50000
- [ ] P99 延迟 < 100ms
- [ ] 错误率 < 0.1%
- [ ] 秒杀场景超卖率 = 0%

---

## 总览：12 周交付里程碑

| 周次 | 阶段 | 关键目标 | 验收指标 |
|-----|------|---------|---------|
| W1 | Phase 0 | 基础设施准备 | 所有中间件就绪，数据库设计完成 |
| W2-3 | Phase 1 | 分布式事务与调度 | Seata 事务成功率 100%，XXL-Job 稳定运行 |
| W4-5 | Phase 2 | 商品服务 + ES 搜索 | 搜索响应 < 100ms，Canal 同步延迟 < 1s |
| W6-7 | Phase 3 | 库存服务高并发 | 库存扣减 TPS > 10000，准确率 100% |
| W8-9 | Phase 4 | 订单服务 + 状态机 | 订单创建 TPS > 10000，状态流转正确 |
| W10 | Phase 5 | 仓库与物流 | 仓库 API < 50ms，物流轨迹同步正常 |
| W11-12 | Phase 6 | 性能优化与压测 | 网关 QPS > 100000，P99 < 100ms |

---

## 附录：关键性能指标汇总

| 服务 | 指标 | 目标值 |
|-----|------|--------|
| 商品搜索 | QPS | > 50000 |
| 商品搜索 | P99 延迟 | < 100ms |
| 订单创建 | TPS | > 10000 |
| 订单创建 | P95 延迟 | < 100ms |
| 库存扣减 | TPS | > 20000 |
| 库存扣减 | P95 延迟 | < 50ms |
| API 网关 | QPS | > 100000 |
| API 网关 | P99 延迟 | < 100ms |
| Redis 缓存 | 命中率 | > 95% |
| Seata 事务 | 成功率 | 100% |
| Canal 同步 | 延迟 | < 1s |
| 秒杀场景 | 超卖率 | 0% |

---

**文档版本**: v1.0
**最后更新**: 2025-12-24
**负责人**: SCM Platform 开发团队

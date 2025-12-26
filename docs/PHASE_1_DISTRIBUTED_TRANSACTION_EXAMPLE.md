# Phase 1: 分布式事务实现示例

本文档提供订单创建分布式事务的完整实现示例，展示如何使用 Seata AT 模式协调跨服务事务。

## 📋 场景说明

**业务场景**: 用户下单购买商品

**涉及服务**:
1. **订单服务** (scm-order) - 创建订单记录
2. **库存服务** (scm-inventory) - 扣减商品库存

**事务要求**:
- 如果库存扣减成功，订单创建成功，提交全局事务
- 如果库存不足或扣减失败，订单回滚，不创建订单记录
- 保证强一致性，不允许超卖

---

## 实现步骤

### Step 1: 创建订单实体类

**文件**: `scm-order/service/src/main/java/com/frog/order/domain/entity/Order.java`

```java
package com.frog.order.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单实体
 */
@Data
@TableName("ord_order")
public class Order {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 订单号（业务主键）
     */
    private String orderNo;

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * SKU ID
     */
    private Long skuId;

    /**
     * SKU 名称
     */
    private String skuName;

    /**
     * 购买数量
     */
    private Integer quantity;

    /**
     * 单价
     */
    private BigDecimal unitPrice;

    /**
     * 总金额
     */
    private BigDecimal totalAmount;

    /**
     * 订单状态: PENDING_PAYMENT, PAID, CANCELLED
     */
    private String status;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间（分区键）
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 逻辑删除标记
     */
    @TableLogic
    private Integer deleted;
}
```

### Step 2: 创建库存实体类

**文件**: `scm-inventory/service/src/main/java/com/frog/inventory/domain/entity/Inventory.java`

```java
package com.frog.inventory.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 库存实体
 */
@Data
@TableName("inv_inventory")
public class Inventory {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * SKU ID
     */
    private Long skuId;

    /**
     * 可用库存
     */
    private Integer availableStock;

    /**
     * 锁定库存
     */
    private Integer lockedStock;

    /**
     * 仓库 ID
     */
    private Long warehouseId;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 逻辑删除标记
     */
    @TableLogic
    private Integer deleted;
}
```

### Step 3: 实现库存服务 (RM - Resource Manager)

**文件**: `scm-inventory/service/src/main/java/com/frog/inventory/service/impl/InventoryDubboServiceImpl.java`

```java
package com.frog.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.frog.inventory.api.InventoryDubboService;
import com.frog.inventory.domain.entity.Inventory;
import com.frog.inventory.mapper.InvInventoryMapper;
import io.seata.core.context.RootContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

/**
 * 库存服务实现
 *
 * <p>参与 Seata 分布式事务，无需添加 @GlobalTransactional 注解
 */
@Slf4j
@Service
@DubboService(version = "1.0.0", group = "scm")
public class InventoryDubboServiceImpl implements InventoryDubboService {

    @Autowired
    private InvInventoryMapper inventoryMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    /**
     * 扣减库存
     *
     * @param skuId SKU ID
     * @param quantity 扣减数量
     * @param requestId 幂等性请求 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deductStock(Long skuId, Integer quantity, String requestId) {
        String xid = RootContext.getXID();
        log.info("🔗 [库存服务] 开始扣减库存: SKU={}, Qty={}, RequestId={}, XID={}",
                skuId, quantity, requestId, xid);

        // 1. 幂等性检查
        String idempotentKey = "deduct:" + requestId;
        Boolean isFirstRequest = redisTemplate.opsForValue()
                .setIfAbsent(idempotentKey, "1", 24, TimeUnit.HOURS);

        if (Boolean.FALSE.equals(isFirstRequest)) {
            log.warn("⚠️  [库存服务] 重复请求，直接返回: RequestId={}", requestId);
            return;
        }

        // 2. 查询库存
        Inventory inventory = inventoryMapper.selectOne(
                new LambdaQueryWrapper<Inventory>()
                        .eq(Inventory::getSkuId, skuId)
                        .last("FOR UPDATE")  // 行锁
        );

        if (inventory == null) {
            log.error("❌ [库存服务] SKU 不存在: SKU={}", skuId);
            throw new IllegalArgumentException("商品不存在");
        }

        // 3. 检查库存是否充足
        if (inventory.getAvailableStock() < quantity) {
            log.error("❌ [库存服务] 库存不足: SKU={}, 可用={}, 需要={}",
                    skuId, inventory.getAvailableStock(), quantity);
            throw new InsufficientStockException(
                    String.format("库存不足: 可用 %d, 需要 %d",
                            inventory.getAvailableStock(), quantity)
            );
        }

        // 4. 扣减库存（会被 Seata 记录到 undo_log）
        int updated = inventoryMapper.update(null,
                new LambdaUpdateWrapper<Inventory>()
                        .setSql("available_stock = available_stock - " + quantity)
                        .eq(Inventory::getId, inventory.getId())
                        .ge(Inventory::getAvailableStock, quantity)  // 乐观锁
        );

        if (updated == 0) {
            log.error("❌ [库存服务] 扣减失败（并发冲突）: SKU={}", skuId);
            throw new InsufficientStockException("库存扣减失败，请重试");
        }

        log.info("✅ [库存服务] 库存扣减成功: SKU={}, 扣减={}, 剩余={}, XID={}",
                skuId, quantity, inventory.getAvailableStock() - quantity, xid);
    }

    @Override
    public Integer queryAvailableStock(Long skuId) {
        Inventory inventory = inventoryMapper.selectOne(
                new LambdaQueryWrapper<Inventory>()
                        .eq(Inventory::getSkuId, skuId)
        );
        return inventory != null ? inventory.getAvailableStock() : 0;
    }

    @Override
    public void batchDeductStock(BatchDeductStockRequest request) {
        // 实现批量扣减逻辑
        for (StockItem item : request.getItems()) {
            deductStock(item.getSkuId(), item.getQuantity(),
                    request.getRequestId() + ":" + item.getSkuId());
        }
    }

    @Override
    public void releaseStock(Long skuId, Integer quantity, String requestId) {
        // 实现库存释放逻辑（用于取消订单）
        log.info("🔄 [库存服务] 释放库存: SKU={}, Qty={}", skuId, quantity);
        inventoryMapper.update(null,
                new LambdaUpdateWrapper<Inventory>()
                        .setSql("available_stock = available_stock + " + quantity)
                        .eq(Inventory::getSkuId, skuId)
        );
    }
}
```

### Step 4: 实现订单服务 (TM - Transaction Manager)

**文件**: `scm-order/service/src/main/java/com/frog/order/service/impl/OrderDubboServiceImpl.java`

```java
package com.frog.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.frog.inventory.api.InventoryDubboService;
import com.frog.order.api.OrderDubboService;
import com.frog.order.domain.entity.Order;
import com.frog.order.mapper.OrdOrderMapper;
import io.seata.core.context.RootContext;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

/**
 * 订单服务实现
 *
 * <p>使用 @GlobalTransactional 标记全局事务边界
 */
@Slf4j
@Service
@DubboService(version = "1.0.0", group = "scm")
public class OrderDubboServiceImpl implements OrderDubboService {

    @Autowired
    private OrdOrderMapper orderMapper;

    @DubboReference(version = "1.0.0", group = "scm", check = false)
    private InventoryDubboService inventoryService;

    /**
     * 创建订单 - 全局事务入口
     *
     * @param request 创建订单请求
     * @return 订单 VO
     */
    @Override
    @GlobalTransactional(
            name = "create-order",
            rollbackFor = Exception.class,
            timeoutMills = 30000
    )
    public OrderVO createOrder(CreateOrderRequest request) {
        String xid = RootContext.getXID();
        log.info("🌐 [订单服务] 开始创建订单: UserId={}, SkuId={}, Qty={}, XID={}",
                request.getUserId(), request.getSkuId(), request.getQuantity(), xid);

        long startTime = System.currentTimeMillis();

        try {
            // 1. 创建订单记录（本地事务）
            Order order = new Order();
            order.setOrderNo(generateOrderNo());
            order.setUserId(request.getUserId());
            order.setSkuId(request.getSkuId());
            order.setSkuName(request.getSkuName());
            order.setQuantity(request.getQuantity());
            order.setUnitPrice(request.getUnitPrice());
            order.setTotalAmount(request.getTotalAmount());
            order.setStatus("PENDING_PAYMENT");
            order.setRemark(request.getRemark());
            order.setCreateTime(LocalDateTime.now());

            orderMapper.insert(order);
            log.info("✅ [订单服务] 订单创建成功: OrderNo={}, OrderId={}, XID={}",
                    order.getOrderNo(), order.getId(), xid);

            // 2. 扣减库存（远程 RPC - 参与全局事务）
            try {
                inventoryService.deductStock(
                        request.getSkuId(),
                        request.getQuantity(),
                        order.getOrderNo()  // 使用订单号作为幂等性 ID
                );
                log.info("✅ [订单服务] 库存扣减成功: OrderNo={}, SKU={}, Qty={}, XID={}",
                        order.getOrderNo(), request.getSkuId(), request.getQuantity(), xid);
            } catch (InventoryDubboService.InsufficientStockException e) {
                log.error("❌ [订单服务] 库存不足，订单创建失败: OrderNo={}, XID={}",
                        order.getOrderNo(), xid);
                throw new RuntimeException("库存不足: " + e.getMessage());
            }

            // 3. 转换为 VO 返回
            OrderVO vo = new OrderVO();
            BeanUtils.copyProperties(order, vo);

            long duration = System.currentTimeMillis() - startTime;
            log.info("🎉 [订单服务] 订单创建完成，全局事务提交: OrderNo={}, XID={}, 耗时={}ms",
                    order.getOrderNo(), xid, duration);

            return vo;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("💥 [订单服务] 订单创建失败，全局事务回滚: XID={}, 耗时={}ms, 原因={}",
                    xid, duration, e.getMessage());
            throw e;  // 重新抛出异常，触发 Seata 全局回滚
        }
    }

    @Override
    public OrderVO queryOrder(String orderNo) {
        Order order = orderMapper.selectOne(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getOrderNo, orderNo)
        );

        if (order == null) {
            return null;
        }

        OrderVO vo = new OrderVO();
        BeanUtils.copyProperties(order, vo);
        return vo;
    }

    @Override
    @GlobalTransactional(name = "cancel-order", rollbackFor = Exception.class)
    public void cancelOrder(String orderNo) {
        log.info("🚫 [订单服务] 取消订单: OrderNo={}, XID={}", orderNo, RootContext.getXID());

        // 1. 查询订单
        Order order = orderMapper.selectOne(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getOrderNo, orderNo)
        );

        if (order == null) {
            throw new IllegalArgumentException("订单不存在");
        }

        // 2. 更新订单状态
        order.setStatus("CANCELLED");
        orderMapper.updateById(order);

        // 3. 释放库存
        inventoryService.releaseStock(
                order.getSkuId(),
                order.getQuantity(),
                "CANCEL:" + orderNo
        );

        log.info("✅ [订单服务] 订单取消成功: OrderNo={}", orderNo);
    }

    /**
     * 生成订单号
     *
     * <p>格式: ORD + 时间戳 + 随机数
     *
     * @return 订单号
     */
    private String generateOrderNo() {
        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int random = new Random().nextInt(10000);
        return String.format("ORD%s%04d", timestamp, random);
    }
}
```

### Step 5: 创建 Mapper

**文件**: `scm-order/service/src/main/java/com/frog/order/mapper/OrdOrderMapper.java`

```java
package com.frog.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.frog.order.domain.entity.Order;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单 Mapper
 */
@Mapper
public interface OrdOrderMapper extends BaseMapper<Order> {
}
```

---

## 事务执行流程

### 成功场景

```
┌─────────────┐         ┌──────────────┐         ┌──────────────┐
│   Client    │         │ Order Service│         │Inventory Svc │
└──────┬──────┘         └──────┬───────┘         └──────┬───────┘
       │                       │                        │
       │  1. createOrder()     │                        │
       ├──────────────────────▶│                        │
       │                       │                        │
       │                       │ 2. Begin Global TX     │
       │                       │                        │
       │                       │ 3. INSERT order        │
       │                       │ (Branch TX 1)          │
       │                       │                        │
       │                       │ 4. deductStock(RPC)    │
       │                       ├───────────────────────▶│
       │                       │                        │
       │                       │                        │ 5. UPDATE inventory
       │                       │                        │ (Branch TX 2)
       │                       │                        │
       │                       │ 6. Success             │
       │                       │◀───────────────────────┤
       │                       │                        │
       │                       │ 7. Commit Global TX    │
       │                       │                        │
       │  8. OrderVO           │                        │
       │◀──────────────────────┤                        │
       │                       │                        │
```

**日志输出:**

```
🌐 [Seata] 开始全局事务: OrderDubboServiceImpl.createOrder
🌐 [订单服务] 开始创建订单: UserId=1001, SkuId=2001, Qty=5, XID=192.168.1.100:8091:2147483647
✅ [订单服务] 订单创建成功: OrderNo=ORD202512261030120001, OrderId=1, XID=192.168.1.100:8091:2147483647
🔗 [Seata] 加入全局事务: InventoryDubboServiceImpl.deductStock, XID: 192.168.1.100:8091:2147483647
🔗 [库存服务] 开始扣减库存: SKU=2001, Qty=5, RequestId=ORD202512261030120001, XID=192.168.1.100:8091:2147483647
✅ [库存服务] 库存扣减成功: SKU=2001, 扣减=5, 剩余=95, XID=192.168.1.100:8091:2147483647
✅ [订单服务] 库存扣减成功: OrderNo=ORD202512261030120001, SKU=2001, Qty=5, XID=192.168.1.100:8091:2147483647
🎉 [订单服务] 订单创建完成，全局事务提交: OrderNo=ORD202512261030120001, XID=192.168.1.100:8091:2147483647, 耗时=125ms
✅ [Seata] 全局事务提交成功: OrderDubboServiceImpl.createOrder, XID: 192.168.1.100:8091:2147483647, 耗时: 125ms
```

### 失败场景（库存不足）

```
┌─────────────┐         ┌──────────────┐         ┌──────────────┐
│   Client    │         │ Order Service│         │Inventory Svc │
└──────┬──────┘         └──────┬───────┘         └──────┬───────┘
       │                       │                        │
       │  1. createOrder()     │                        │
       ├──────────────────────▶│                        │
       │                       │                        │
       │                       │ 2. Begin Global TX     │
       │                       │                        │
       │                       │ 3. INSERT order        │
       │                       │ (Branch TX 1)          │
       │                       │                        │
       │                       │ 4. deductStock(RPC)    │
       │                       ├───────────────────────▶│
       │                       │                        │
       │                       │                        │ 5. Check stock
       │                       │                        │ ❌ Insufficient!
       │                       │                        │
       │                       │ 6. Exception           │
       │                       │◀───────────────────────┤
       │                       │                        │
       │                       │ 7. Rollback Global TX  │
       │                       │ (DELETE order)         │
       │                       │                        │
       │  8. Exception         │                        │
       │◀──────────────────────┤                        │
       │                       │                        │
```

**日志输出:**

```
🌐 [Seata] 开始全局事务: OrderDubboServiceImpl.createOrder
🌐 [订单服务] 开始创建订单: UserId=1001, SkuId=2001, Qty=200, XID=192.168.1.100:8091:2147483648
✅ [订单服务] 订单创建成功: OrderNo=ORD202512261031450002, OrderId=2, XID=192.168.1.100:8091:2147483648
🔗 [Seata] 加入全局事务: InventoryDubboServiceImpl.deductStock, XID: 192.168.1.100:8091:2147483648
🔗 [库存服务] 开始扣减库存: SKU=2001, Qty=200, RequestId=ORD202512261031450002, XID=192.168.1.100:8091:2147483648
❌ [库存服务] 库存不足: SKU=2001, 可用=100, 需要=200
❌ [订单服务] 库存不足，订单创建失败: OrderNo=ORD202512261031450002, XID=192.168.1.100:8091:2147483648
💥 [订单服务] 订单创建失败，全局事务回滚: XID=192.168.1.100:8091:2147483648, 耗时=78ms, 原因=库存不足: 可用 100, 需要 200
❌ [Seata] 全局事务回滚: OrderDubboServiceImpl.createOrder, XID: 192.168.1.100:8091:2147483648, 耗时: 78ms, 原因: 库存不足: 可用 100, 需要 200
```

---

## 测试验证

### 1. 准备测试数据

```sql
-- 初始化库存数据
INSERT INTO inv_inventory (sku_id, available_stock, locked_stock, warehouse_id, create_time, update_time)
VALUES (2001, 100, 0, 1, NOW(), NOW());
```

### 2. 成功场景测试

```java
@SpringBootTest
@Slf4j
public class DistributedTransactionTest {

    @DubboReference(version = "1.0.0", group = "scm")
    private OrderDubboService orderService;

    @Test
    public void testCreateOrderSuccess() {
        // 准备请求
        OrderDubboService.CreateOrderRequest request = new OrderDubboService.CreateOrderRequest();
        request.setUserId(1001L);
        request.setSkuId(2001L);
        request.setSkuName("测试商品");
        request.setQuantity(5);
        request.setUnitPrice(new BigDecimal("99.00"));
        request.setTotalAmount(new BigDecimal("495.00"));

        // 创建订单
        OrderDubboService.OrderVO order = orderService.createOrder(request);

        // 验证结果
        assertNotNull(order);
        assertNotNull(order.getOrderNo());
        assertEquals("PENDING_PAYMENT", order.getStatus());

        log.info("订单创建成功: {}", order.getOrderNo());
    }
}
```

### 3. 失败场景测试

```java
@Test
public void testCreateOrderInsufficientStock() {
    // 准备请求（数量超过库存）
    OrderDubboService.CreateOrderRequest request = new OrderDubboService.CreateOrderRequest();
    request.setUserId(1001L);
    request.setSkuId(2001L);
    request.setSkuName("测试商品");
    request.setQuantity(200);  // 库存只有 100
    request.setUnitPrice(new BigDecimal("99.00"));
    request.setTotalAmount(new BigDecimal("19800.00"));

    // 验证抛出异常
    assertThrows(RuntimeException.class, () -> {
        orderService.createOrder(request);
    });

    // 验证订单未创建
    List<Order> orders = orderMapper.selectList(
            new QueryWrapper<Order>().eq("user_id", 1001L)
    );
    assertEquals(0, orders.size(), "订单应该回滚，数据库中不应该有记录");

    log.info("库存不足场景测试通过 ✓");
}
```

---

## 关键要点

### ✅ 最佳实践

1. **全局事务注解位置**: 在服务入口方法添加 `@GlobalTransactional`
2. **异常处理**: 必须重新抛出异常，不能吞掉
3. **幂等性设计**: 使用 Redis SET NX 防止重复扣减
4. **行锁**: 查询库存时使用 `FOR UPDATE` 防止并发冲突
5. **日志记录**: 记录 XID 便于问题排查

### ⚠️ 常见陷阱

1. **不要吞掉异常**: 会导致事务不回滚
2. **避免过长事务**: 设置合理的 `timeoutMills`
3. **数据库连接池**: 确保连接池大小足够
4. **undo_log 清理**: 定期清理过期日志

---

**版本**: v1.0.0
**最后更新**: 2025-12-26
**维护者**: SCM Platform Team
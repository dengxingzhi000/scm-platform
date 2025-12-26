# Seata 分布式事务集成指南

本文档提供 SCM Platform 中 Seata 分布式事务的完整集成、配置和使用指南。

## 📋 目录

1. [架构概述](#架构概述)
2. [快速开始](#快速开始)
3. [详细配置](#详细配置)
4. [使用示例](#使用示例)
5. [验证测试](#验证测试)
6. [故障排查](#故障排查)
7. [最佳实践](#最佳实践)

---

## 架构概述

### Seata 架构图

```
┌─────────────────────────────────────────────────────────────┐
│                     Seata 分布式事务                          │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│  ┌─────────────┐      ┌──────────────┐      ┌─────────────┐ │
│  │   Order     │      │  Inventory   │      │   Payment   │ │
│  │  Service    │─────▶│   Service    │─────▶│   Service   │ │
│  │    (TM)     │      │     (RM)     │      │     (RM)     │ │
│  └─────────────┘      └──────────────┘      └─────────────┘ │
│         │                     │                     │         │
│         │                     │                     │         │
│         └─────────────────────┼─────────────────────┘         │
│                               ▼                               │
│                     ┌──────────────────┐                      │
│                     │  Seata Server    │                      │
│                     │      (TC)        │                      │
│                     └──────────────────┘                      │
│                               │                               │
│                               ▼                               │
│                     ┌──────────────────┐                      │
│                     │   PostgreSQL     │                      │
│                     │  (global_table,  │                      │
│                     │   branch_table,  │                      │
│                     │   lock_table)    │                      │
│                     └──────────────────┘                      │
│                                                               │
└─────────────────────────────────────────────────────────────┘
```

### 角色说明

- **TC (Transaction Coordinator)**: Seata Server - 事务协调者，维护全局和分支事务的状态
- **TM (Transaction Manager)**: 事务管理器 - 定义全局事务的范围（使用 `@GlobalTransactional` 注解）
- **RM (Resource Manager)**: 资源管理器 - 管理分支事务处理的资源，与 TC 交互注册分支事务和报告分支事务状态

### 事务模式

本项目使用 **AT 模式** (Automatic Transaction):
- 自动提交/回滚
- 基于 UNDO_LOG 实现
- 无业务侵入
- 适合绝大多数场景

---

## 快速开始

### 1. 启动基础设施

```bash
# 启动所有基础设施（包括 Seata Server）
docker-compose up -d

# 验证 Seata Server 启动
docker logs -f scm-seata

# 预期日志输出:
# Server started, listen port: 8091
```

### 2. 初始化 Seata 数据库

```bash
# 设置数据库密码
export PGPASSWORD=your-password  # Linux/Mac
# 或
set PGPASSWORD=your-password     # Windows

# 运行初始化脚本
cd scripts/db
bash init-seata.sh

# 脚本会自动完成:
# ✓ 创建 seata 数据库和 4 张表 (global_table, branch_table, lock_table, distributed_lock)
# ✓ 在所有业务数据库中添加 undo_log 表
```

### 3. 配置 Nacos

**上传 Seata Server 配置到 Nacos:**

```bash
# 方式 1: 使用 Nacos Open API
curl -X POST 'http://localhost:8848/nacos/v1/cs/configs' \
  -d 'dataId=seataServer.properties' \
  -d 'group=SEATA_GROUP' \
  -d 'content=...'  # config/seata/seataServer.properties 文件内容

# 方式 2: 通过 Nacos 控制台
# 1. 访问 http://localhost:8848/nacos
# 2. 登录 (nacos/nacos)
# 3. 配置管理 → 配置列表
# 4. 点击 "+" 创建配置:
#    - Data ID: seataServer.properties
#    - Group: SEATA_GROUP
#    - 配置格式: Properties
#    - 配置内容: 粘贴 config/seata/seataServer.properties 内容
```

**验证配置上传:**

```bash
curl -X GET 'http://localhost:8848/nacos/v1/cs/configs?dataId=seataServer.properties&group=SEATA_GROUP'
```

### 4. 验证 Seata Server 注册

```bash
# 查看 Nacos 服务列表
curl -X GET 'http://localhost:8848/nacos/v1/ns/instance/list?serviceName=seata-server&groupName=SEATA_GROUP'

# 预期响应:
# {
#   "name": "SEATA_GROUP@@seata-server",
#   "hosts": [{
#     "ip": "172.x.x.x",
#     "port": 8091,
#     "healthy": true
#   }]
# }
```

### 5. 启动微服务

```bash
# 启动订单服务
cd scm-order/service
mvn spring-boot:run

# 启动库存服务
cd scm-inventory/service
mvn spring-boot:run

# 验证服务注册到 Seata
# 查看服务日志，应该看到:
# ✓ Register TM success
# ✓ Register RM success
```

---

## 详细配置

### Seata Server 配置 (config/seata/application.yml)

```yaml
seata:
  config:
    type: nacos                          # 配置中心类型
    nacos:
      server-addr: nacos:8848
      group: SEATA_GROUP
      data-id: seataServer.properties

  registry:
    type: nacos                          # 注册中心类型
    nacos:
      application: seata-server
      server-addr: nacos:8848
      group: SEATA_GROUP

  store:
    mode: db                             # 存储模式: db (生产) / file (开发)
    db:
      datasource: druid
      db-type: postgresql
      url: jdbc:postgresql://postgres:5432/seata
```

### 客户端配置 (application.yml)

**每个微服务** 需要添加以下配置:

```yaml
seata:
  enabled: true
  application-id: ${spring.application.name}
  tx-service-group: ${spring.application.name}-tx-group  # 事务分组

  # 配置中心
  config:
    type: nacos
    nacos:
      server-addr: ${NACOS_SERVER:localhost:8848}
      group: SEATA_GROUP
      data-id: seataServer.properties

  # 注册中心
  registry:
    type: nacos
    nacos:
      application: seata-server
      server-addr: ${NACOS_SERVER:localhost:8848}
      group: SEATA_GROUP

  # 客户端配置
  client:
    undo:
      log-table: undo_log                # UNDO 日志表名
      log-serialization: jackson         # 序列化方式
```

### 数据库表结构

**Seata Server 数据库 (seata):**

| 表名 | 说明 |
|------|------|
| global_table | 全局事务表 - 存储全局事务信息 |
| branch_table | 分支事务表 - 存储分支事务信息 |
| lock_table | 锁表 - 分布式锁信息 |
| distributed_lock | 分布式锁辅助表 |

**业务数据库 (db_order, db_inventory 等):**

| 表名 | 说明 |
|------|------|
| undo_log | 回滚日志表 - AT 模式回滚数据快照 |

---

## 使用示例

### 示例 1: 订单创建分布式事务

**场景**: 创建订单时，需要同时完成:
1. 订单服务: 创建订单记录
2. 库存服务: 扣减商品库存
3. 支付服务: 创建支付记录

如果任一步骤失败，所有操作回滚。

**实现代码:**

```java
// ============================================================
// 1. 订单服务 (TM - 事务发起方)
// ============================================================
@Service
public class OrderServiceImpl {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private InventoryDubboService inventoryService;  // Dubbo RPC

    @Autowired
    private PaymentDubboService paymentService;      // Dubbo RPC

    /**
     * 创建订单 - 分布式事务入口
     *
     * @GlobalTransactional: 标记全局事务边界
     * - name: 事务名称（用于日志和监控）
     * - rollbackFor: 触发回滚的异常类型
     * - timeoutMills: 全局事务超时时间（默认 60 秒）
     */
    @GlobalTransactional(
        name = "create-order",
        rollbackFor = Exception.class,
        timeoutMills = 30000
    )
    public OrderVO createOrder(CreateOrderDTO dto) {
        log.info("开始创建订单: {}", dto);

        // 步骤 1: 创建订单记录（本地事务）
        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setUserId(dto.getUserId());
        order.setTotalAmount(dto.getTotalAmount());
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        orderMapper.insert(order);
        log.info("订单创建成功: {}", order.getOrderNo());

        // 步骤 2: 扣减库存（远程 RPC - 自动参与全局事务）
        try {
            inventoryService.deductStock(
                dto.getSkuId(),
                dto.getQuantity(),
                order.getOrderNo()  // 幂等性请求 ID
            );
            log.info("库存扣减成功: SKU={}, Qty={}", dto.getSkuId(), dto.getQuantity());
        } catch (InsufficientStockException e) {
            log.error("库存不足，订单创建失败");
            throw new BusinessException("库存不足");  // 触发回滚
        }

        // 步骤 3: 创建支付记录（远程 RPC - 自动参与全局事务）
        try {
            paymentService.createPayment(
                order.getId(),
                dto.getPaymentMethod(),
                dto.getTotalAmount()
            );
            log.info("支付记录创建成功: OrderId={}", order.getId());
        } catch (Exception e) {
            log.error("支付记录创建失败", e);
            throw new BusinessException("支付失败");  // 触发回滚
        }

        log.info("订单创建完成，全局事务提交: XID={}", RootContext.getXID());
        return convert(order);
    }
}

// ============================================================
// 2. 库存服务 (RM - 资源管理器)
// ============================================================
@Service
@Slf4j
public class InventoryServiceImpl implements InventoryDubboService {

    @Autowired
    private InventoryMapper inventoryMapper;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    /**
     * 扣减库存 - 参与分布式事务
     *
     * 注意: 不需要添加 @GlobalTransactional 注解
     * 该方法通过 Dubbo RPC 调用，会自动加入调用方的全局事务
     */
    @Override
    public void deductStock(Long skuId, Integer quantity, String requestId) {
        log.info("开始扣减库存: SKU={}, Qty={}, XID={}",
            skuId, quantity, RootContext.getXID());

        // 幂等性检查
        String idempotentKey = "deduct:" + requestId;
        Boolean isFirstRequest = redisTemplate.opsForValue()
            .setIfAbsent(idempotentKey, "1", 24, TimeUnit.HOURS);

        if (Boolean.FALSE.equals(isFirstRequest)) {
            log.info("重复请求，直接返回: {}", requestId);
            return;  // 已处理过，直接返回
        }

        // 查询库存
        Inventory inventory = inventoryMapper.selectBySkuId(skuId);
        if (inventory == null) {
            throw new BusinessException("商品不存在");
        }

        // 检查库存是否充足
        if (inventory.getAvailableStock() < quantity) {
            log.error("库存不足: 可用={}, 需要={}",
                inventory.getAvailableStock(), quantity);
            throw new InsufficientStockException("库存不足");
        }

        // 扣减库存（数据库操作会被 Seata 记录到 undo_log）
        int updated = inventoryMapper.deductStock(skuId, quantity);
        if (updated == 0) {
            throw new BusinessException("库存扣减失败");
        }

        log.info("库存扣减成功: SKU={}, 剩余={}",
            skuId, inventory.getAvailableStock() - quantity);
    }
}

// ============================================================
// 3. 支付服务 (RM - 资源管理器)
// ============================================================
@Service
@Slf4j
public class PaymentServiceImpl implements PaymentDubboService {

    @Autowired
    private PaymentRecordMapper paymentMapper;

    /**
     * 创建支付记录 - 参与分布式事务
     */
    @Override
    public Long createPayment(Long orderId, String paymentMethod, BigDecimal amount) {
        log.info("创建支付记录: OrderId={}, Amount={}, XID={}",
            orderId, amount, RootContext.getXID());

        PaymentRecord payment = new PaymentRecord();
        payment.setOrderId(orderId);
        payment.setPaymentMethod(paymentMethod);
        payment.setAmount(amount);
        payment.setStatus(PaymentStatus.PENDING);

        paymentMapper.insert(payment);
        log.info("支付记录创建成功: PaymentId={}", payment.getId());

        return payment.getId();
    }
}
```

### 示例 2: 事务回滚场景

```java
@GlobalTransactional(name = "test-rollback", rollbackFor = Exception.class)
public void testRollback() {
    // 步骤 1: 插入订单
    orderMapper.insert(new Order(...));

    // 步骤 2: 扣减库存
    inventoryService.deductStock(...);

    // 步骤 3: 模拟异常
    throw new RuntimeException("测试回滚");  // 所有操作都会回滚
}
```

**日志输出:**

```
🌐 [Seata] 开始全局事务: OrderServiceImpl.testRollback
🔗 [Seata] 加入全局事务: InventoryServiceImpl.deductStock, XID: 192.168.1.100:8091:2147483647
❌ [Seata] 全局事务回滚: OrderServiceImpl.testRollback, XID: 192.168.1.100:8091:2147483647, 耗时: 125ms, 原因: 测试回滚
```

### 示例 3: 手动事务管理

```java
@Service
public class ManualTransactionService {

    @Autowired
    private GlobalTransactionExecutor executor;

    public void manualTransaction() throws Exception {
        // 手动开启全局事务
        GlobalTransaction tx = GlobalTransactionContext.getCurrentOrCreate();

        try {
            tx.begin(30000, "manual-tx");

            // 业务逻辑
            orderMapper.insert(...);
            inventoryService.deductStock(...);

            // 手动提交
            tx.commit();
        } catch (Exception e) {
            // 手动回滚
            tx.rollback();
            throw e;
        }
    }
}
```

---

## 验证测试

### 1. 验证 Seata Server 启动

```bash
# 检查 Seata Server 容器状态
docker ps | grep seata

# 查看 Seata Server 日志
docker logs scm-seata | grep "Server started"

# 验证 Seata Server 注册到 Nacos
curl http://localhost:8848/nacos/v1/ns/instance/list?serviceName=seata-server&groupName=SEATA_GROUP
```

### 2. 验证客户端注册

```bash
# 启动微服务后查看日志
# 应该看到以下日志:
# ✓ register success, cost xxx ms, version:2.2.0, role:TMROLE
# ✓ register success, cost xxx ms, version:2.2.0, role:RMROLE
```

### 3. 验证数据库表

```sql
-- Seata Server 数据库
\c seata
SELECT COUNT(*) FROM global_table;     -- 应该返回 0 (初始状态)
SELECT COUNT(*) FROM branch_table;     -- 应该返回 0 (初始状态)
SELECT COUNT(*) FROM lock_table;       -- 应该返回 0 (初始状态)

-- 业务数据库
\c db_order
SELECT COUNT(*) FROM undo_log;         -- 应该返回 0 (初始状态)

\c db_inventory
SELECT COUNT(*) FROM undo_log;         -- 应该返回 0 (初始状态)
```

### 4. 集成测试

创建测试用例验证分布式事务:

```java
@SpringBootTest
@Slf4j
public class SeataIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private InventoryMapper inventoryMapper;

    @Test
    @Transactional
    @Rollback
    public void testDistributedTransactionCommit() {
        // 准备测试数据
        Long skuId = 1001L;
        Integer initialStock = 100;

        // 初始化库存
        Inventory inventory = new Inventory();
        inventory.setSkuId(skuId);
        inventory.setAvailableStock(initialStock);
        inventoryMapper.insert(inventory);

        // 创建订单（会扣减库存）
        CreateOrderDTO dto = new CreateOrderDTO();
        dto.setSkuId(skuId);
        dto.setQuantity(10);
        dto.setUserId(1L);

        OrderVO order = orderService.createOrder(dto);

        // 验证订单创建成功
        assertNotNull(order);
        assertNotNull(order.getOrderNo());

        // 验证库存扣减成功
        Inventory updated = inventoryMapper.selectBySkuId(skuId);
        assertEquals(initialStock - 10, updated.getAvailableStock().intValue());

        log.info("分布式事务提交测试通过 ✓");
    }

    @Test
    public void testDistributedTransactionRollback() {
        // 准备测试数据
        Long skuId = 1002L;
        Integer initialStock = 5;  // 库存不足

        // 初始化库存
        Inventory inventory = new Inventory();
        inventory.setSkuId(skuId);
        inventory.setAvailableStock(initialStock);
        inventoryMapper.insert(inventory);

        // 尝试创建订单（库存不足，应该失败）
        CreateOrderDTO dto = new CreateOrderDTO();
        dto.setSkuId(skuId);
        dto.setQuantity(10);  // 需要 10，但只有 5
        dto.setUserId(1L);

        // 验证抛出异常
        assertThrows(InsufficientStockException.class, () -> {
            orderService.createOrder(dto);
        });

        // 验证订单未创建
        Long orderCount = orderMapper.selectCount(
            new QueryWrapper<Order>().eq("user_id", 1L)
        );
        assertEquals(0L, orderCount.longValue());

        // 验证库存未扣减（回滚成功）
        Inventory unchanged = inventoryMapper.selectBySkuId(skuId);
        assertEquals(initialStock, unchanged.getAvailableStock());

        log.info("分布式事务回滚测试通过 ✓");
    }
}
```

---

## 故障排查

### 问题 1: 服务启动时报 "can not get cluster name in registry config"

**原因**: Nacos 配置未上传或配置错误

**解决**:
```bash
# 1. 检查 Nacos 配置是否存在
curl -X GET 'http://localhost:8848/nacos/v1/cs/configs?dataId=seataServer.properties&group=SEATA_GROUP'

# 2. 如果不存在，手动上传配置
# 参考 "快速开始 -> 3. 配置 Nacos"
```

### 问题 2: 事务不生效，数据未回滚

**可能原因**:
1. 未添加 `@GlobalTransactional` 注解
2. 异常被 catch 且未抛出
3. `rollbackFor` 配置错误

**解决**:
```java
// ✓ 正确
@GlobalTransactional(rollbackFor = Exception.class)
public void method() {
    try {
        // 业务逻辑
    } catch (Exception e) {
        log.error("错误", e);
        throw e;  // 必须重新抛出异常
    }
}

// ✗ 错误
@GlobalTransactional(rollbackFor = Exception.class)
public void method() {
    try {
        // 业务逻辑
    } catch (Exception e) {
        log.error("错误", e);
        // 吞掉异常，事务不会回滚
    }
}
```

### 问题 3: undo_log 表不存在

**错误日志**: `ERROR 42P01: relation "undo_log" does not exist`

**解决**:
```bash
# 运行 undo_log 表初始化脚本
cd scripts/db
psql -U admin -d db_order -f microservices/020_undo_log_tables.sql
psql -U admin -d db_inventory -f microservices/020_undo_log_tables.sql
# ... 其他业务数据库
```

### 问题 4: 全局事务超时

**错误日志**: `Global transaction timeout, XID: xxx`

**解决**:
```java
// 增加超时时间
@GlobalTransactional(
    name = "long-running-task",
    timeoutMills = 120000  // 2 分钟
)
public void longTask() {
    // 业务逻辑
}
```

### 问题 5: 查看 Seata 事务日志

```sql
-- 查看全局事务
SELECT * FROM seata.global_table
ORDER BY gmt_create DESC LIMIT 10;

-- 查看分支事务
SELECT * FROM seata.branch_table
WHERE xid = 'your-xid'
ORDER BY gmt_create DESC;

-- 查看锁信息
SELECT * FROM seata.lock_table
WHERE xid = 'your-xid';

-- 查看 undo_log
SELECT * FROM db_order.undo_log
WHERE xid = 'your-xid';
```

---

## 最佳实践

### 1. 事务粒度控制

✓ **推荐**: 在服务入口方法添加 `@GlobalTransactional`

```java
// 服务层 - 添加全局事务
@Service
public class OrderService {
    @GlobalTransactional
    public void createOrder() {
        // 调用其他服务
    }
}
```

✗ **不推荐**: 在 Controller 层添加

```java
// Controller 层 - 不推荐
@RestController
public class OrderController {
    @GlobalTransactional  // 粒度过大
    public ApiResponse createOrder() {
        // ...
    }
}
```

### 2. 幂等性设计

**必须实现幂等性**，防止重复扣减:

```java
public void deductStock(Long skuId, Integer quantity, String requestId) {
    // 使用 Redis SET NX 实现幂等
    String key = "deduct:" + requestId;
    Boolean success = redisTemplate.opsForValue()
        .setIfAbsent(key, "1", 24, TimeUnit.HOURS);

    if (Boolean.FALSE.equals(success)) {
        return;  // 已处理过
    }

    // 执行扣减逻辑
}
```

### 3. 异常处理

**明确 rollbackFor**，避免遗漏:

```java
// ✓ 明确指定回滚异常
@GlobalTransactional(rollbackFor = Exception.class)

// ✗ 使用默认值（仅回滚 RuntimeException）
@GlobalTransactional
```

### 4. 超时设置

根据业务场景设置合理超时:

```java
// 快速操作: 10 秒
@GlobalTransactional(timeoutMills = 10000)

// 复杂操作: 60 秒
@GlobalTransactional(timeoutMills = 60000)

// 批处理: 5 分钟
@GlobalTransactional(timeoutMills = 300000)
```

### 5. 日志监控

利用 `GlobalTransactionalAspect` 自动记录事务日志:

```
🌐 [Seata] 开始全局事务: OrderServiceImpl.createOrder
🔗 [Seata] 加入全局事务: InventoryServiceImpl.deductStock, XID: xxx
✅ [Seata] 全局事务提交成功: OrderServiceImpl.createOrder, XID: xxx, 耗时: 125ms
```

### 6. 性能优化

- **批量操作**: 尽量合并多次 RPC 调用
- **异步化**: 非关键路径使用消息队列
- **缓存**: 减少数据库查询

```java
// ✓ 批量操作
@GlobalTransactional
public void batchCreateOrders(List<OrderDTO> orders) {
    // 批量插入订单
    orderMapper.insertBatch(orders);

    // 批量扣减库存（一次 RPC）
    Map<Long, Integer> stockMap = ...;
    inventoryService.batchDeductStock(stockMap);
}

// ✗ 逐个操作
@GlobalTransactional
public void createOrders(List<OrderDTO> orders) {
    for (OrderDTO order : orders) {
        orderMapper.insert(order);
        inventoryService.deductStock(...);  // N 次 RPC
    }
}
```

### 7. 清理 undo_log

定期清理过期的 undo_log:

```sql
-- 创建定时任务（每天凌晨 2 点执行）
CREATE EXTENSION IF NOT EXISTS pg_cron;

SELECT cron.schedule(
    'cleanup-undo-log',
    '0 2 * * *',
    $$DELETE FROM undo_log WHERE log_created < NOW() - INTERVAL '7 days' AND log_status = 1$$
);
```

---

## 监控指标

### Prometheus 指标

Seata 自动暴露以下指标（端口 9898）:

```bash
# 查看所有指标
curl http://localhost:9898/metrics

# 关键指标:
seata_transaction_total          # 事务总数
seata_transaction_committed      # 提交数
seata_transaction_rollbacked     # 回滚数
seata_transaction_timeout        # 超时数
seata_branch_transaction_total   # 分支事务总数
```

### Grafana 面板

导入 Seata 官方 Grafana Dashboard:

```bash
# Dashboard ID: 11981
# 导入后可查看:
# - 事务 TPS
# - 成功率
# - 平均响应时间
# - 失败原因分析
```

---

## 参考资料

- [Seata 官方文档](https://seata.io/zh-cn/docs/overview/what-is-seata.html)
- [Seata AT 模式原理](https://seata.io/zh-cn/docs/dev/mode/at-mode.html)
- [Spring Cloud Alibaba Seata](https://github.com/alibaba/spring-cloud-alibaba/wiki/Seata)
- [SCM Platform CLAUDE.md](../CLAUDE.md)

---

**版本**: v1.0.0
**最后更新**: 2025-12-26
**维护者**: SCM Platform Team
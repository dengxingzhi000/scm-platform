# XXL-Job 分布式任务调度集成指南

本文档提供 SCM Platform 中 XXL-Job 分布式任务调度的完整集成、配置和使用指南。

## 📋 目录

1. [架构概述](#架构概述)
2. [快速开始](#快速开始)
3. [详细配置](#详细配置)
4. [任务开发](#任务开发)
5. [任务管理](#任务管理)
6. [监控告警](#监控告警)
7. [最佳实践](#最佳实践)

---

## 架构概述

### XXL-Job 架构图

```
┌─────────────────────────────────────────────────────────────┐
│                    XXL-Job 架构                              │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│  ┌──────────────────┐         ┌──────────────────┐          │
│  │  XXL-Job Admin   │────────▶│   MySQL 8.0      │          │
│  │  (调度中心)       │         │   (任务元数据)    │          │
│  └────────┬─────────┘         └──────────────────┘          │
│           │                                                  │
│           │ HTTP 调度                                        │
│           │                                                  │
│    ┌──────┴──────┬────────────────┬────────────────┐        │
│    │             │                │                │        │
│    ▼             ▼                ▼                ▼        │
│  ┌────┐       ┌────┐          ┌────┐          ┌────┐       │
│  │订单│       │库存│          │仓储│          │物流│       │
│  │服务│       │服务│          │服务│          │服务│       │
│  └────┘       └────┘          └────┘          └────┘       │
│   9999         9998            9997            9996         │
│  (Executor)   (Executor)     (Executor)     (Executor)     │
│                                                               │
└─────────────────────────────────────────────────────────────┘
```

### 组件说明

- **XXL-Job Admin**: 调度中心，管理任务配置、调度策略、执行日志
- **XXL-Job Executor**: 执行器，嵌入微服务，接收调度指令执行任务
- **MySQL**: 存储任务元数据、执行日志、调度锁

### 任务类型

1. **BEAN 模式**: Java 类任务（推荐）
2. **GLUE 模式**: 在线编辑脚本任务（Java/Shell/Python/PHP/NodeJS）

---

## 快速开始

### 1. 启动 XXL-Job Admin

```bash
# 通过 Docker Compose 启动
docker-compose up -d xxl-job-admin mysql-xxljob

# 验证启动
docker logs -f scm-xxl-job

# 预期日志:
# xxl-job remoting server start success, nettype = class com.xxl.job.core.server.EmbedServer, port = 8080
```

### 2. 访问 XXL-Job 控制台

```
URL: http://localhost:8088/xxl-job-admin
用户名: admin
密码: 123456
```

### 3. 验证执行器注册

启动微服务后，在控制台查看执行器:

```
执行器管理 → 在线执行器列表
应该看到:
- scm-order-executor (订单服务)
- scm-inventory-executor (库存服务)
```

### 4. 启动任务

```
任务管理 → 选择执行器 → 操作列 → 启动
```

---

## 详细配置

### XXL-Job Admin 配置

**Docker Compose 配置** (`docker-compose.yml`):

```yaml
xxl-job-admin:
  image: xuxueli/xxl-job-admin:2.4.3
  container_name: scm-xxl-job
  ports:
    - "8088:8080"
  environment:
    PARAMS: >
      --spring.datasource.url=jdbc:mysql://mysql-xxljob:3306/xxl_job
      --spring.datasource.username=root
      --spring.datasource.password=root
  volumes:
    - xxljob_data:/data/applogs
  depends_on:
    - mysql-xxljob
```

### 执行器配置 (application.yml)

**订单服务** (`scm-order/service/src/main/resources/application.yml`):

```yaml
xxl:
  job:
    admin:
      addresses: http://localhost:8088/xxl-job-admin  # Admin 地址
    executor:
      appname: scm-order-executor                      # 执行器名称（必须与 Admin 中配置一致）
      address:                                         # 执行器地址（留空自动获取）
      ip:                                              # 执行器 IP（留空自动获取）
      port: 9999                                       # 执行器端口
      logpath: /data/applogs/xxl-job/jobhandler       # 日志路径
      logretentiondays: 30                             # 日志保留天数
    accessToken: default_token                         # 访问令牌（需与 Admin 一致）
```

**库存服务** (`scm-inventory/service/src/main/resources/application.yml`):

```yaml
xxl:
  job:
    admin:
      addresses: http://localhost:8088/xxl-job-admin
    executor:
      appname: scm-inventory-executor                  # 不同的执行器名称
      port: 9998                                       # 不同的端口
      logpath: /data/applogs/xxl-job/jobhandler
      logretentiondays: 30
    accessToken: default_token
```

### Java 配置类

**XxlJobConfig.java**:

```java
@Slf4j
@Configuration
public class XxlJobConfig {

    @Value("${xxl.job.admin.addresses}")
    private String adminAddresses;

    @Value("${xxl.job.executor.appname}")
    private String appname;

    @Value("${xxl.job.executor.port}")
    private int port;

    @Bean
    public XxlJobSpringExecutor xxlJobExecutor() {
        log.info("🚀 [XXL-Job] 初始化执行器: appname={}", appname);

        XxlJobSpringExecutor executor = new XxlJobSpringExecutor();
        executor.setAdminAddresses(adminAddresses);
        executor.setAppname(appname);
        executor.setPort(port);
        // ... 其他配置

        return executor;
    }
}
```

---

## 任务开发

### 示例 1: 订单超时取消任务

**业务场景**: 每分钟扫描超时未支付订单，自动取消并释放库存

**实现代码**:

```java
@Slf4j
@Component
public class OrderTimeoutCancelJobHandler {

    @Autowired
    private OrderMapper orderMapper;

    @DubboReference
    private InventoryDubboService inventoryService;

    /**
     * 订单超时自动取消任务
     *
     * @XxlJob: 任务标识，必须与 Admin 中配置的 JobHandler 一致
     */
    @XxlJob("orderTimeoutCancelJobHandler")
    public void execute() throws Exception {
        XxlJobHelper.log("⏰ [订单超时取消] 开始执行任务");

        // 1. 获取任务参数（超时分钟数）
        String param = XxlJobHelper.getJobParam();
        int timeoutMinutes = Integer.parseInt(param != null && !param.isEmpty() ? param : "30");

        // 2. 查询超时订单
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(timeoutMinutes);
        List<Order> timeoutOrders = orderMapper.selectList(
            new LambdaQueryWrapper<Order>()
                .eq(Order::getStatus, "PENDING_PAYMENT")
                .lt(Order::getCreateTime, threshold)
                .last("LIMIT 1000")
        );

        XxlJobHelper.log("📋 发现超时订单: count={}", timeoutOrders.size());

        // 3. 批量取消订单
        int successCount = 0;
        for (Order order : timeoutOrders) {
            try {
                cancelOrder(order);  // 分布式事务取消订单
                successCount++;
                XxlJobHelper.log("  ✓ 取消成功: orderNo={}", order.getOrderNo());
            } catch (Exception e) {
                XxlJobHelper.log("  ✗ 取消失败: orderNo={}", order.getOrderNo());
            }
        }

        // 4. 返回任务结果
        XxlJobHelper.handleSuccess(String.format("成功取消 %d 个订单", successCount));
    }

    @GlobalTransactional(rollbackFor = Exception.class)
    public void cancelOrder(Order order) {
        // 更新订单状态
        orderMapper.updateById(order.setStatus("CANCELLED_TIMEOUT"));

        // 释放库存（RPC 调用，参与分布式事务）
        inventoryService.releaseStock(order.getSkuId(), order.getQuantity(), "TIMEOUT:" + order.getOrderNo());
    }
}
```

### 示例 2: 库存快照同步任务

**业务场景**: 每天凌晨 1 点生成库存快照，用于数据分析

**实现代码**:

```java
@Slf4j
@Component
public class InventorySnapshotJobHandler {

    @Autowired
    private InventoryMapper inventoryMapper;

    @Autowired
    private SnapshotMapper snapshotMapper;

    @XxlJob("inventorySnapshotJobHandler")
    @Transactional(rollbackFor = Exception.class)
    public void execute() throws Exception {
        XxlJobHelper.log("📸 [库存快照] 开始执行任务");

        // 1. 查询所有库存
        List<Inventory> inventories = inventoryMapper.selectList(null);
        XxlJobHelper.log("📋 库存记录数: {}", inventories.size());

        // 2. 生成快照
        int successCount = 0;
        LocalDateTime snapshotTime = LocalDateTime.now();

        for (Inventory inv : inventories) {
            Snapshot snapshot = new Snapshot();
            snapshot.setSkuId(inv.getSkuId());
            snapshot.setAvailableStock(inv.getAvailableStock());
            snapshot.setLockedStock(inv.getLockedStock());
            snapshot.setSnapshotTime(snapshotTime);

            snapshotMapper.insert(snapshot);
            successCount++;

            if (successCount % 100 == 0) {
                XxlJobHelper.log("  已生成快照: {}/{}", successCount, inventories.size());
            }
        }

        XxlJobHelper.handleSuccess(String.format("生成快照: %d 条", successCount));
    }
}
```

### 任务开发规范

#### ✅ 必须遵守

1. **使用 @XxlJob 注解**: 方法必须使用 `@XxlJob("handlerName")` 注解
2. **使用 XxlJobHelper 记录日志**: 日志会显示在 Admin 控制台
3. **返回任务结果**: 使用 `XxlJobHelper.handleSuccess()` 或 `handleFail()`
4. **异常处理**: 捕获异常并记录详细信息
5. **参数获取**: 使用 `XxlJobHelper.getJobParam()` 获取任务参数
6. **幂等性设计**: 任务必须支持重复执行

#### ⚠️ 注意事项

1. **避免长时间运行**: 任务超时默认 5 分钟，可配置
2. **批量处理使用分页**: 避免一次性查询大量数据
3. **记录处理进度**: 使用 `XxlJobHelper.log()` 记录关键节点
4. **使用分片执行**: 大数据量任务使用分片参数

---

## 任务管理

### 在 Admin 控制台配置任务

**1. 添加执行器** (`执行器管理` → `新增`):

```
AppName: scm-order-executor
名称: 订单服务执行器
注册方式: 自动注册
```

**2. 添加任务** (`任务管理` → `新增`):

```
执行器: scm-order-executor
任务描述: 订单超时自动取消
路由策略: FIRST (第一个)
Cron: 0 */1 * * * ?  (每分钟执行)
运行模式: BEAN
JobHandler: orderTimeoutCancelJobHandler
任务参数: 30  (30 分钟超时)
阻塞处理策略: 单机串行
子任务: (留空)
任务超时时间: 300 秒
失败重试次数: 3
```

**3. 启动任务**:

点击任务列表中的 "启动" 按钮

### 调度策略说明

| Cron 表达式 | 说明 | 示例 |
|------------|------|------|
| `0 */1 * * * ?` | 每分钟执行 | 订单超时取消 |
| `0 0 1 * * ?` | 每天凌晨 1 点 | 库存快照 |
| `0 0 */2 * * ?` | 每 2 小时执行 | 数据同步 |
| `0 0 0 * * ?` | 每天零点执行 | 日报统计 |
| `0 0 12 * * ?` | 每天中午 12 点 | 午间数据汇总 |

**Cron 在线生成器**: https://cron.qqe2.com/

### 路由策略

| 策略 | 说明 | 使用场景 |
|------|------|---------|
| FIRST | 第一个执行器 | 单机任务 |
| LAST | 最后一个执行器 | - |
| ROUND | 轮询 | 负载均衡 |
| RANDOM | 随机 | 负载均衡 |
| CONSISTENT_HASH | 一致性哈希 | 按参数路由 |
| SHARDING_BROADCAST | 分片广播 | 大数据量处理 |

### 阻塞处理策略

| 策略 | 说明 |
|------|------|
| 单机串行 | 同一时刻只允许一个任务实例运行 |
| 丢弃后续调度 | 新任务被触发时，如果上一个任务还在运行，则丢弃新任务 |
| 覆盖之前调度 | 新任务被触发时，如果上一个任务还在运行，则终止上一个任务 |

---

## 监控告警

### 查看任务执行日志

```
任务管理 → 操作 → 执行日志
```

日志包含:
- 调度时间
- 调度结果（成功/失败）
- 执行时间
- 执行日志（XxlJobHelper.log() 输出的内容）
- 执行结果（handleSuccess/handleFail 的消息）

### 告警配置

**1. 邮件告警** (`任务管理` → `编辑任务`):

```
报警邮件: admin@example.com,team@example.com
```

任务执行失败时，会自动发送邮件通知。

**2. 钉钉告警** (通过 WebHook):

在任务代码中添加钉钉通知:

```java
@XxlJob("criticalJobHandler")
public void execute() {
    try {
        // 业务逻辑
    } catch (Exception e) {
        XxlJobHelper.log("任务执行失败: {}", e.getMessage());
        sendDingTalkAlert("订单超时取消任务失败", e.getMessage());
        throw e;
    }
}

private void sendDingTalkAlert(String title, String content) {
    // 调用钉钉 WebHook API
}
```

### 监控指标

**1. 任务执行报表** (`调度报表`):
- 每日执行总数
- 成功率
- 平均执行时间

**2. 执行器健康检查**:
- 在线执行器数量
- 注册时间
- 心跳时间

---

## 最佳实践

### 1. 任务幂等性设计

```java
@XxlJob("orderSyncJobHandler")
public void execute() {
    String jobId = XxlJobHelper.getJobId() + "_" + System.currentTimeMillis();

    // 使用 Redis 防止重复执行
    Boolean locked = redisTemplate.opsForValue()
        .setIfAbsent("job:lock:" + jobId, "1", 10, TimeUnit.MINUTES);

    if (Boolean.FALSE.equals(locked)) {
        XxlJobHelper.log("任务已在执行中，跳过");
        return;
    }

    try {
        // 业务逻辑
    } finally {
        redisTemplate.delete("job:lock:" + jobId);
    }
}
```

### 2. 分片执行大任务

```java
@XxlJob("largeDataJobHandler")
public void execute() {
    // 获取分片参数
    int shardIndex = XxlJobHelper.getShardIndex();  // 当前分片索引 (0-based)
    int shardTotal = XxlJobHelper.getShardTotal();  // 总分片数

    XxlJobHelper.log("分片执行: {}/{}", shardIndex + 1, shardTotal);

    // 根据分片参数查询数据
    List<Order> orders = orderMapper.selectList(
        new LambdaQueryWrapper<Order>()
            .apply("MOD(id, {0}) = {1}", shardTotal, shardIndex)
            .last("LIMIT 10000")
    );

    // 处理数据
    for (Order order : orders) {
        // 处理逻辑
    }

    XxlJobHelper.handleSuccess(String.format("分片 %d/%d 完成，处理 %d 条",
        shardIndex + 1, shardTotal, orders.size()));
}
```

### 3. 任务参数化

```java
@XxlJob("configJobHandler")
public void execute() {
    // 从任务参数中读取配置
    String param = XxlJobHelper.getJobParam();
    JSONObject config = JSON.parseObject(param);

    int batchSize = config.getIntValue("batchSize");
    int timeoutMinutes = config.getIntValue("timeoutMinutes");
    String targetStatus = config.getString("targetStatus");

    // 使用参数执行业务逻辑
}
```

**Admin 中配置任务参数**:

```json
{
  "batchSize": 1000,
  "timeoutMinutes": 30,
  "targetStatus": "CANCELLED"
}
```

### 4. 异常处理与重试

```java
@XxlJob("retryJobHandler")
public void execute() {
    int maxRetries = 3;
    int retryCount = 0;
    Exception lastException = null;

    while (retryCount < maxRetries) {
        try {
            // 业务逻辑
            performTask();
            XxlJobHelper.handleSuccess("执行成功");
            return;
        } catch (Exception e) {
            retryCount++;
            lastException = e;
            XxlJobHelper.log("执行失败，重试 {}/{}: {}",
                retryCount, maxRetries, e.getMessage());

            if (retryCount < maxRetries) {
                Thread.sleep(5000);  // 等待 5 秒后重试
            }
        }
    }

    XxlJobHelper.handleFail("重试 " + maxRetries + " 次后仍失败: " + lastException.getMessage());
}
```

### 5. 任务执行日志规范

```java
@XxlJob("standardJobHandler")
public void execute() {
    long startTime = System.currentTimeMillis();

    // 1. 任务开始
    XxlJobHelper.log("🚀 [任务名称] 开始执行");

    // 2. 参数解析
    String param = XxlJobHelper.getJobParam();
    XxlJobHelper.log("📝 任务参数: {}", param);

    // 3. 关键步骤记录
    XxlJobHelper.log("📊 步骤 1: 查询数据");
    // ... 业务逻辑

    XxlJobHelper.log("✅ 步骤 1 完成: 查询到 {} 条记录", count);

    // 4. 执行结果
    long duration = System.currentTimeMillis() - startTime;
    XxlJobHelper.handleSuccess(String.format(
        "任务完成: 处理=%d, 成功=%d, 失败=%d, 耗时=%dms",
        total, success, failed, duration
    ));
}
```

### 6. 任务依赖（子任务）

```java
// 父任务: 数据导入
@XxlJob("dataImportJobHandler")
public void importData() {
    // 导入数据
    XxlJobHelper.log("数据导入完成");
    // 子任务会自动触发
}

// 子任务: 数据校验（在 Admin 中配置为父任务的子任务）
@XxlJob("dataValidationJobHandler")
public void validateData() {
    // 校验数据
    XxlJobHelper.log("数据校验完成");
}
```

**Admin 配置**:
- 父任务 `dataImportJobHandler` 的子任务 ID 填写子任务的 ID
- 父任务执行成功后，子任务自动触发

---

## 常见问题

### 1. 执行器未注册

**错误**: 控制台看不到执行器

**解决**:
```bash
# 检查服务是否启动
curl http://localhost:8203/actuator/health

# 检查执行器配置
cat scm-order/service/src/main/resources/application.yml | grep -A 10 "xxl.job"

# 查看服务日志
# 应该看到: [XXL-Job] 执行器初始化完成
```

### 2. 任务执行失败

**错误**: 任务状态显示失败

**解决**:
1. 查看执行日志中的错误信息
2. 检查 JobHandler 名称是否与 `@XxlJob` 注解一致
3. 检查任务超时时间是否足够
4. 查看服务日志中的详细堆栈

### 3. 任务重复执行

**原因**: 未配置阻塞处理策略

**解决**:
- 任务配置中选择 "单机串行"
- 在代码中实现幂等性检查（使用分布式锁）

---

## 性能优化

### 1. 数据库优化

```sql
-- 为 xxl_job_log 表添加索引
CREATE INDEX idx_trigger_time ON xxl_job_log(trigger_time);
CREATE INDEX idx_job_id ON xxl_job_log(job_id);

-- 定期清理历史日志（保留 30 天）
DELETE FROM xxl_job_log WHERE trigger_time < DATE_SUB(NOW(), INTERVAL 30 DAY);
```

### 2. 任务分片

- 单个任务处理数据量超过 10000 条时，使用分片执行
- 分片数建议设置为执行器数量的 2-4 倍

### 3. 异步执行

```java
@XxlJob("asyncJobHandler")
public void execute() {
    // 主任务快速返回
    CompletableFuture.runAsync(() -> {
        // 异步执行耗时操作
        performHeavyTask();
    });

    XxlJobHelper.handleSuccess("任务已提交异步执行");
}
```

---

## 参考资料

- [XXL-Job 官方文档](https://www.xuxueli.com/xxl-job/)
- [XXL-Job GitHub](https://github.com/xuxueli/xxl-job)
- [SCM Platform CLAUDE.md](../CLAUDE.md)

---

**版本**: v1.0.0
**最后更新**: 2025-12-26
**维护者**: SCM Platform Team
# Phase 1 性能测试计划

**文档版本**: v1.0
**创建日期**: 2025-12-26
**作者**: SCM Platform Team
**目标**: 验证 Seata 分布式事务和 XXL-Job 任务调度的性能表现

---

## 1. 测试目标

### 1.1 核心目标
- 验证 **Seata AT 模式**在高并发场景下的 TPS 和响应时间
- 验证 **Seata TCC 模式**在高并发场景下的 TPS 和响应时间
- 对比 AT 模式和 TCC 模式的性能差异
- 验证 **XXL-Job** 任务调度的稳定性和执行效率
- 识别系统性能瓶颈和资源消耗

### 1.2 性能指标

| 指标类型 | 指标名称 | 目标值 | 说明 |
|---------|---------|-------|-----|
| **吞吐量** | Seata AT 模式 TPS | ≥ 500 TPS | 订单创建 + 库存扣减 |
| **吞吐量** | Seata TCC 模式 TPS | ≥ 300 TPS | 订单创建 + 库存预留 |
| **响应时间** | P50 响应时间 | ≤ 100ms | 50% 请求在 100ms 内完成 |
| **响应时间** | P95 响应时间 | ≤ 500ms | 95% 请求在 500ms 内完成 |
| **响应时间** | P99 响应时间 | ≤ 1000ms | 99% 请求在 1000ms 内完成 |
| **成功率** | 事务成功率 | ≥ 99.9% | 全局事务提交成功率 |
| **资源** | CPU 使用率 | ≤ 80% | 峰值时 CPU 使用率 |
| **资源** | 内存使用率 | ≤ 70% | 峰值时内存使用率 |
| **资源** | 数据库连接池 | ≤ 80% | 连接池使用率 |
| **任务调度** | XXL-Job 执行延迟 | ≤ 5s | 任务触发到执行的延迟 |

---

## 2. 测试环境

### 2.1 硬件配置

| 组件 | 配置 |
|-----|-----|
| **应用服务器** | 4 Core CPU, 8GB RAM |
| **数据库服务器** | 4 Core CPU, 16GB RAM, SSD |
| **Seata Server** | 2 Core CPU, 4GB RAM |
| **Redis** | 2 Core CPU, 4GB RAM |
| **负载生成器** | 4 Core CPU, 8GB RAM |

### 2.2 软件环境

| 组件 | 版本 |
|-----|------|
| **JDK** | 21 |
| **Spring Boot** | 3.2.0 |
| **Seata** | 2.2.0 |
| **PostgreSQL** | 16 |
| **Redis** | 7.2 |
| **XXL-Job** | 2.4.3 |
| **Nacos** | 2.3.2 |

### 2.3 网络环境
- **延迟**: < 1ms（本地网络）
- **带宽**: 1 Gbps

---

## 3. 测试场景

### 3.1 Seata AT 模式性能测试

#### 场景 1: 订单创建（正常流程）
- **描述**: 用户创建订单，扣减库存，全局事务提交
- **并发用户**: 100, 200, 500, 1000
- **持续时间**: 5 分钟
- **请求参数**:
  ```json
  {
    "userId": "{randomUserId}",
    "skuId": 1001,
    "quantity": 1,
    "unitPrice": 99.00,
    "totalAmount": 99.00
  }
  ```

#### 场景 2: 订单创建（库存不足回滚）
- **描述**: 库存不足，全局事务回滚
- **并发用户**: 50, 100, 200
- **持续时间**: 3 分钟
- **预期**: 验证回滚性能和一致性

#### 场景 3: 混合流程（成功 + 失败）
- **描述**: 80% 成功，20% 库存不足失败
- **并发用户**: 200
- **持续时间**: 10 分钟
- **预期**: 验证真实场景下的性能表现

### 3.2 Seata TCC 模式性能测试

#### 场景 1: 订单创建（Try-Confirm 流程）
- **描述**: 订单创建，库存预留，全局事务提交，Confirm
- **并发用户**: 100, 200, 500
- **持续时间**: 5 分钟
- **预期**: 对比 AT 模式性能差异

#### 场景 2: 订单创建（Try-Cancel 流程）
- **描述**: 库存不足，全局事务回滚，Cancel
- **并发用户**: 50, 100, 200
- **持续时间**: 3 分钟
- **预期**: 验证 Cancel 补偿性能

#### 场景 3: 幂等性压力测试
- **描述**: 相同 businessKey 并发调用
- **并发用户**: 50
- **持续时间**: 2 分钟
- **预期**: 验证幂等机制性能

### 3.3 XXL-Job 任务调度性能测试

#### 场景 1: 订单超时取消任务
- **描述**: 批量处理超时订单
- **数据量**: 1000, 5000, 10000 个超时订单
- **执行频率**: 每分钟
- **预期**: 验证批量处理性能

#### 场景 2: 库存快照任务
- **描述**: 创建库存快照
- **数据量**: 10000, 50000, 100000 SKU
- **执行频率**: 每小时
- **预期**: 验证大数据量处理性能

---

## 4. 测试工具

### 4.1 JMeter 测试脚本

#### AT 模式测试脚本
```xml
<!-- JMeter Test Plan: Seata AT Mode Load Test -->
<jmeterTestPlan version="1.2">
  <hashTree>
    <TestPlan guiclass="TestPlanGui" testname="Seata AT Mode Test">
      <elementProp name="TestPlan.user_defined_variables" elementType="Arguments">
        <collectionProp name="Arguments.arguments">
          <elementProp name="BASE_URL" elementType="Argument">
            <stringProp name="Argument.value">http://localhost:8203</stringProp>
          </elementProp>
          <elementProp name="THREADS" elementType="Argument">
            <stringProp name="Argument.value">100</stringProp>
          </elementProp>
        </collectionProp>
      </elementProp>
    </TestPlan>

    <ThreadGroup guiclass="ThreadGroupGui" testname="Order Creation Thread Group">
      <stringProp name="ThreadGroup.num_threads">${THREADS}</stringProp>
      <stringProp name="ThreadGroup.ramp_time">60</stringProp>
      <stringProp name="ThreadGroup.duration">300</stringProp>

      <HTTPSamplerProxy>
        <stringProp name="HTTPSampler.domain">localhost</stringProp>
        <stringProp name="HTTPSampler.port">8203</stringProp>
        <stringProp name="HTTPSampler.path">/api/v1/orders/create</stringProp>
        <stringProp name="HTTPSampler.method">POST</stringProp>
        <stringProp name="HTTPSampler.postBodyRaw">
{
  "userId": ${__Random(1000,9999)},
  "skuId": 1001,
  "quantity": 1,
  "unitPrice": 99.00,
  "totalAmount": 99.00
}
        </stringProp>
      </HTTPSamplerProxy>
    </ThreadGroup>

    <ResultCollector guiclass="SummaryReport" testname="Summary Report"/>
    <ResultCollector guiclass="GraphVisualizer" testname="Response Time Graph"/>
  </hashTree>
</jmeterTestPlan>
```

### 4.2 Gatling 测试脚本 (Scala)

```scala
package com.scm.performance

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

class SeataAtModeLoadTest extends Simulation {

  val httpProtocol = http
    .baseUrl("http://localhost:8203")
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")

  val createOrderScenario = scenario("Create Order AT Mode")
    .exec(
      http("Create Order")
        .post("/api/v1/orders/create")
        .body(StringBody("""{
          "userId": ${randomUserId},
          "skuId": 1001,
          "quantity": 1,
          "unitPrice": 99.00,
          "totalAmount": 99.00
        }""")).asJson
        .check(status.is(200))
    )

  setUp(
    createOrderScenario.inject(
      rampUsers(100) during (60 seconds),
      constantUsersPerSec(50) during (5 minutes)
    )
  ).protocols(httpProtocol)
   .assertions(
     global.responseTime.percentile3.lt(1000),
     global.successfulRequests.percent.gt(99.9)
   )
}
```

### 4.3 自定义 Java 性能测试

详见: `scm-order/service/src/test/java/com/frog/order/PerformanceTest.java`

---

## 5. 监控指标采集

### 5.1 应用层监控

#### Prometheus + Grafana
- **JVM 指标**: Heap 使用率、GC 次数、GC 耗时
- **Tomcat 指标**: 线程池使用率、请求队列长度
- **Seata 指标**: 全局事务数量、分支事务数量、回滚次数
- **业务指标**: 订单创建 TPS、库存扣减 TPS

#### Micrometer Metrics
```java
@Timed(value = "order.create.at.mode", description = "Order creation with AT mode")
public OrderVO createOrder(CreateOrderRequest request) {
    // ...
}
```

### 5.2 数据库监控

#### PostgreSQL pg_stat_statements
```sql
-- 查询慢 SQL
SELECT query, calls, mean_exec_time, stddev_exec_time
FROM pg_stat_statements
WHERE mean_exec_time > 100
ORDER BY mean_exec_time DESC
LIMIT 20;

-- 查询连接数
SELECT count(*) FROM pg_stat_activity;

-- 查询锁等待
SELECT * FROM pg_stat_activity WHERE wait_event IS NOT NULL;
```

### 5.3 Redis 监控

```bash
# Redis 性能指标
redis-cli INFO stats
redis-cli INFO memory
redis-cli SLOWLOG GET 100
```

### 5.4 Seata Server 监控

- **控制台**: http://localhost:7091
- **Metrics**: http://localhost:7091/metrics
- **日志分析**: 全局事务提交/回滚比率

---

## 6. 性能瓶颈分析

### 6.1 常见瓶颈

| 瓶颈类型 | 症状 | 排查方法 | 优化建议 |
|---------|-----|---------|---------|
| **数据库连接池** | 连接池耗尽，请求阻塞 | 检查 HikariCP 配置 | 增加连接池大小，优化慢 SQL |
| **Seata undo_log** | undo_log 表锁竞争 | 检查 undo_log 表大小 | 定期清理，增加索引 |
| **网络延迟** | RPC 调用延迟高 | 抓包分析网络延迟 | 优化网络拓扑，启用 Dubbo 长连接 |
| **GC 压力** | Full GC 频繁 | Heap Dump 分析 | 调整堆大小，优化对象创建 |
| **Redis 热 Key** | Redis CPU 100% | MONITOR 命令分析 | 拆分热 Key，使用本地缓存 |
| **TCC 预留表** | inv_tcc_reservation 表锁 | 检查索引和查询计划 | 增加索引，分区表 |

### 6.2 性能优化建议

#### 数据库优化
```sql
-- 为 undo_log 表增加复合索引
CREATE INDEX idx_undo_log_xid_branch ON undo_log(xid, branch_id);

-- 定期清理 undo_log（保留 7 天）
DELETE FROM undo_log WHERE log_created < NOW() - INTERVAL '7 days';

-- 为 TCC 预留表增加索引
CREATE INDEX idx_tcc_reservation_business_key ON inv_tcc_reservation(business_key);
CREATE INDEX idx_tcc_reservation_xid ON inv_tcc_reservation(xid);
```

#### 连接池优化
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 50      # 增加连接池大小
      minimum-idle: 10
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
```

#### Seata 优化
```yaml
seata:
  client:
    rm:
      async-commit-buffer-limit: 10000    # 异步提交缓冲区
      report-retry-count: 5
    tm:
      commit-retry-count: 5
      rollback-retry-count: 5
```

#### Redis 优化
```yaml
spring:
  redis:
    lettuce:
      pool:
        max-active: 50
        max-idle: 20
        min-idle: 10
```

---

## 7. 测试执行计划

### 7.1 测试阶段

| 阶段 | 测试内容 | 持续时间 | 负责人 |
|-----|---------|---------|-------|
| **Phase 1** | 基线测试（单用户） | 1 小时 | 测试团队 |
| **Phase 2** | 低负载测试（100 并发） | 2 小时 | 测试团队 |
| **Phase 3** | 中负载测试（500 并发） | 4 小时 | 测试团队 |
| **Phase 4** | 高负载测试（1000 并发） | 4 小时 | 测试团队 |
| **Phase 5** | 稳定性测试（200 并发，24 小时） | 24 小时 | 测试团队 |
| **Phase 6** | 性能调优 | 1 周 | 开发团队 |
| **Phase 7** | 回归测试 | 1 天 | 测试团队 |

### 7.2 测试检查点

- [ ] **环境准备**: 所有服务正常启动
- [ ] **数据准备**: 初始化测试数据（库存、用户）
- [ ] **监控配置**: Prometheus、Grafana、日志采集
- [ ] **基线测试**: 单用户功能验证
- [ ] **负载测试**: 按阶段执行并发测试
- [ ] **性能分析**: 识别瓶颈，记录优化建议
- [ ] **优化实施**: 应用优化方案
- [ ] **回归测试**: 验证优化效果
- [ ] **报告生成**: 生成性能测试报告

---

## 8. 测试报告模板

### 8.1 报告结构

```
# Seata 分布式事务性能测试报告

## 1. 测试概述
- 测试目标
- 测试环境
- 测试日期

## 2. 测试结果摘要

### 2.1 AT 模式性能
| 并发数 | TPS | P50 | P95 | P99 | 成功率 |
|-------|-----|-----|-----|-----|-------|
| 100   | 512 | 85ms| 320ms| 780ms| 99.95%|
| 500   | 485 | 120ms| 520ms| 980ms| 99.85%|
| 1000  | 420 | 180ms| 680ms| 1200ms| 99.50%|

### 2.2 TCC 模式性能
| 并发数 | TPS | P50 | P95 | P99 | 成功率 |
|-------|-----|-----|-----|-----|-------|
| 100   | 320 | 120ms| 450ms| 920ms| 99.90%|
| 500   | 285 | 180ms| 680ms| 1350ms| 99.70%|

### 2.3 资源使用
- CPU: 最高 72%
- 内存: 最高 65%
- 数据库连接池: 最高 78%

## 3. 性能瓶颈分析
- undo_log 表写入成为瓶颈
- 数据库连接池在 1000 并发时接近饱和
- Redis 延迟在可接受范围内

## 4. 优化建议
- 增加 undo_log 定期清理任务
- 调整数据库连接池大小
- 优化慢 SQL

## 5. 结论
系统在 500 并发下性能表现良好，满足预期目标。
```

---

## 9. 附录

### 9.1 测试数据准备脚本

```sql
-- 初始化 1000 个 SKU 库存
INSERT INTO inv_inventory (sku_id, available_stock, locked_stock, warehouse_id)
SELECT
    generate_series(1, 1000) AS sku_id,
    10000 AS available_stock,
    0 AS locked_stock,
    1 AS warehouse_id;

-- 初始化 10000 个测试用户
INSERT INTO sys_user (user_id, username, mobile, status)
SELECT
    generate_series(1, 10000) AS user_id,
    'user_' || generate_series(1, 10000) AS username,
    '138' || LPAD(generate_series(1, 10000)::TEXT, 8, '0') AS mobile,
    'ACTIVE' AS status;
```

### 9.2 监控脚本

```bash
#!/bin/bash
# monitor.sh - 实时监控系统资源

echo "=== System Monitor ==="
while true; do
    echo "$(date '+%Y-%m-%d %H:%M:%S')"

    # CPU 使用率
    echo "CPU: $(top -bn1 | grep "Cpu(s)" | awk '{print $2}')%"

    # 内存使用率
    echo "Memory: $(free | grep Mem | awk '{printf "%.2f%%", $3/$2 * 100.0}')"

    # 数据库连接数
    echo "DB Connections: $(psql -U admin -d db_order -t -c "SELECT count(*) FROM pg_stat_activity")"

    # Redis 内存
    echo "Redis Memory: $(redis-cli INFO memory | grep used_memory_human | cut -d: -f2)"

    echo "---"
    sleep 5
done
```

---

**文档结束**
# 性能基准测试计划 (Performance Benchmark Plan)
# SCM Platform - Load Testing & Optimization

> **文档类型**: 性能基准测试计划
> **性能工程师**: 测试组 + DevOps
> **测试工具**: JMeter, Gatling, wrk
> **版本**: v1.0
> **最后更新**: 2025-12-24

---

## 一、性能目标

### 1.1 整体目标

| 维度 | 指标 | 目标值 | 当前值 | 基线要求 |
|-----|------|--------|--------|---------|
| **响应时间** | API P99延迟 | < 100ms | - | < 200ms |
| **吞吐量** | 订单创建 TPS | > 10,000 | - | > 5,000 |
| **吞吐量** | 库存扣减 TPS | > 20,000 | - | > 10,000 |
| **吞吐量** | 商品搜索 QPS | > 50,000 | - | > 20,000 |
| **并发** | 最大并发用户数 | > 10,000 | - | > 5,000 |
| **可用性** | 系统可用性 | > 99.9% | - | > 99.5% |
| **资源** | CPU使用率 | < 70% | - | < 80% |
| **资源** | 内存使用率 | < 80% | - | < 90% |

### 1.2 场景目标

| 场景 | 并发用户 | 持续时间 | 目标TPS/QPS | 目标延迟(P99) | 错误率 |
|-----|---------|---------|-------------|--------------|--------|
| **商品搜索** | 5,000 | 30min | 50,000 QPS | < 100ms | < 0.1% |
| **订单创建** | 500 | 30min | 10,000 TPS | < 100ms | < 0.1% |
| **库存扣减** | 1,000 | 30min | 20,000 TPS | < 50ms | < 0.01% |
| **秒杀场景** | 10,000 | 1min | 峰值 100,000 QPS | < 200ms | < 1% |

---

## 二、测试环境

### 2.1 硬件配置

**应用服务器** (3台):
```
CPU: 16 Core (Intel Xeon)
内存: 64GB
硬盘: 500GB SSD
网络: 10Gbps
```

**数据库服务器** (3台 - 主从):
```
CPU: 32 Core
内存: 128GB
硬盘: 1TB NVMe SSD
网络: 10Gbps
```

**中间件服务器** (6台):
```
Redis: 3台 (主从 + Sentinel)
Elasticsearch: 3台 (集群)
Kafka: 3台 (集群)
```

### 2.2 软件配置

**应用层**:
```yaml
JVM参数:
  -Xms: 16G
  -Xmx: 16G
  -XX:+UseG1GC
  -XX:MaxGCPauseMillis: 200
  -XX:ParallelGCThreads: 16
```

**数据库**:
```ini
# PostgreSQL
max_connections = 500
shared_buffers = 16GB
effective_cache_size = 48GB
work_mem = 64MB
```

**Redis**:
```conf
maxmemory 32gb
maxmemory-policy allkeys-lru
```

---

## 三、测试场景设计

### 3.1 场景1: 商品搜索（高并发读）

**业务流程**:
```
用户搜索 → Elasticsearch查询 → 返回结果（分页）
```

**JMeter 配置**:
```xml
<ThreadGroup>
  <stringProp name="ThreadGroup.num_threads">5000</stringProp>
  <stringProp name="ThreadGroup.ramp_time">300</stringProp>  <!-- 5分钟预热 -->
  <stringProp name="ThreadGroup.duration">1800</stringProp>  <!-- 持续30分钟 -->
</ThreadGroup>

<HTTPSamplerProxy>
  <stringProp name="HTTPSampler.domain">${__P(host,api.scm.com)}</stringProp>
  <stringProp name="HTTPSampler.path">/api/v1/products/search</stringProp>
  <stringProp name="HTTPSampler.method">GET</stringProp>
  <stringProp name="Arguments">
    keyword=${keyword}
    &page=${__Random(1,100)}
    &size=20
    &sort=sales
  </stringProp>
</HTTPSamplerProxy>

<!-- 关键词随机化（CSV Data Set） -->
<CSVDataSet>
  <stringProp name="filename">keywords.csv</stringProp>
  <stringProp name="variableNames">keyword</stringProp>
</CSVDataSet>
```

**监控指标**:
```
- Elasticsearch QPS
- Elasticsearch P99延迟
- JVM GC次数 + GC时间
- Caffeine缓存命中率
- Redis缓存命中率
```

**验收标准**:
- [x] QPS > 50,000
- [x] P99延迟 < 100ms
- [x] 错误率 < 0.1%
- [x] GC时间 < 5%

### 3.2 场景2: 订单创建（分布式事务）

**业务流程**:
```
下单 → Seata全局事务开始
  ├─ 创建订单（本地事务）
  ├─ 扣减库存（Dubbo RPC）
  └─ 创建支付单（Dubbo RPC）
→ Seata全局事务提交/回滚
```

**JMeter 配置**:
```xml
<ThreadGroup>
  <stringProp name="ThreadGroup.num_threads">500</stringProp>
  <stringProp name="ThreadGroup.ramp_time">60</stringProp>
  <stringProp name="ThreadGroup.duration">1800</stringProp>
</ThreadGroup>

<HTTPSamplerProxy>
  <stringProp name="HTTPSampler.path">/api/v1/orders</stringProp>
  <stringProp name="HTTPSampler.method">POST</stringProp>
  <stringProp name="HTTPSampler.postBodyRaw">
{
  "userId": ${__Random(1,100000)},
  "items": [
    {
      "skuId": ${__Random(1,10000)},
      "quantity": ${__Random(1,5)},
      "price": 99.00
    }
  ],
  "shippingAddress": { ... }
}
  </stringProp>
</HTTPSamplerProxy>

<!-- 添加JWT Token -->
<HeaderManager>
  <collectionProp name="Headers">
    <elementProp>
      <stringProp name="Header.name">Authorization</stringProp>
      <stringProp name="Header.value">Bearer ${token}</stringProp>
    </elementProp>
  </collectionProp>
</HeaderManager>
```

**监控指标**:
```
- 订单创建 TPS
- Seata 事务提交/回滚比例
- 数据库连接池使用率
- Dubbo 调用延迟
- Redis 库存扣减延迟
```

**验收标准**:
- [x] TPS > 10,000
- [x] P99延迟 < 100ms
- [x] Seata 事务成功率 > 99.9%
- [x] 数据库连接数 < 500

### 3.3 场景3: 秒杀场景（极限压测）

**业务流程**:
```
10,000用户同时抢购 → Redis Lua原子扣减 → MQ削峰 → 异步创建订单
```

**wrk 压测脚本**:
```lua
-- seckill.lua
wrk.method = "POST"
wrk.headers["Content-Type"] = "application/json"
wrk.headers["Authorization"] = "Bearer <token>"

request = function()
   local body = string.format([[
   {
     "userId": %d,
     "skuId": 123,
     "quantity": 1
   }
   ]], math.random(1, 100000))

   return wrk.format(nil, "/api/v1/seckill/order", nil, body)
end
```

**执行命令**:
```bash
wrk -t 100 -c 10000 -d 60s -s seckill.lua http://api.scm.com
```

**验收标准**:
- [x] 峰值 QPS > 100,000
- [x] P99延迟 < 200ms
- [x] 不超卖、不少卖
- [x] 系统不崩溃

---

## 四、测试执行

### 4.1 测试阶段

| 阶段 | 时间 | 目标 | 负载 |
|-----|------|------|------|
| **Smoke Test** | 10min | 验证功能正常 | 10% 负载 |
| **Load Test** | 30min | 验证正常负载 | 100% 负载 |
| **Stress Test** | 60min | 找到系统极限 | 150% → 200% 负载 |
| **Spike Test** | 5min | 验证突发流量 | 0% → 300% → 0% |
| **Soak Test** | 24小时 | 验证长时间稳定性 | 80% 负载 |

### 4.2 执行步骤

**Step 1: 准备测试数据**
```bash
# 生成 100,000 用户数据
python generate_users.py --count 100000

# 生成 10,000 商品数据
python generate_products.py --count 10000

# 预热 Redis 缓存
python warm_cache.py

# 预热 Elasticsearch 索引
python warm_es_index.py
```

**Step 2: 启动监控**
```bash
# Prometheus + Grafana
docker-compose -f monitoring/docker-compose.yml up -d

# SkyWalking APM
docker run -d -p 8080:8080 apache/skywalking-oap-server
docker run -d -p 8080:8080 apache/skywalking-ui
```

**Step 3: 执行压测**
```bash
# 场景1: 商品搜索
jmeter -n -t tests/product_search.jmx -l results/search_$(date +%Y%m%d_%H%M%S).jtl

# 场景2: 订单创建
jmeter -n -t tests/order_create.jmx -l results/order_$(date +%Y%m%d_%H%M%S).jtl

# 场景3: 秒杀
wrk -t 100 -c 10000 -d 60s -s scripts/seckill.lua http://api.scm.com
```

**Step 4: 分析结果**
```bash
# 生成 HTML 报告
jmeter -g results/search_20251224_100000.jtl -o reports/search_report/

# 打开报告
open reports/search_report/index.html
```

---

## 五、性能调优

### 5.1 应用层优化

**1. 缓存优化**
```java
// L1: Caffeine 本地缓存（热点数据）
LoadingCache<Long, Product> localCache = Caffeine.newBuilder()
    .maximumSize(10000)
    .expireAfterWrite(5, TimeUnit.MINUTES)
    .recordStats()
    .build(this::loadFromRedis);

// L2: Redis 缓存
@Cacheable(value = "product", key = "#id", unless = "#result == null")
public Product loadFromRedis(Long id) {
    return productMapper.selectById(id);
}
```

**2. 限流降级**
```yaml
# Sentinel 规则
- resource: /api/v1/products/search
  grade: QPS
  count: 50000
  strategy: WARM_UP  # 预热模式

- resource: /api/v1/orders
  grade: QPS
  count: 10000
  strategy: RATE_LIMITER
```

**3. 异步化**
```java
// 订单创建异步化（MQ削峰）
@PostMapping("/orders/async")
public ApiResponse<String> createOrderAsync(@RequestBody OrderDTO dto) {
    // 写入 Kafka
    kafkaTemplate.send("order.create.async", dto);
    return ApiResponse.success("订单已提交，正在处理中");
}
```

### 5.2 数据库优化

**1. 索引优化**
```sql
-- 查询慢SQL
SELECT query, mean_exec_time, calls
FROM pg_stat_statements
ORDER BY mean_exec_time DESC
LIMIT 10;

-- 添加索引
CREATE INDEX idx_order_user_created ON t_order(user_id, created_at DESC);
```

**2. 连接池调优**
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 200
      minimum-idle: 50
      connection-timeout: 30000
      idle-timeout: 600000
```

**3. 读写分离**
```yaml
# 90% 查询走从库
spring:
  shardingsphere:
    rules:
      readwrite-splitting:
        data-sources:
          scm_order:
            write-data-source-name: master
            read-data-source-names: slave1,slave2
            load-balancer-name: round-robin
```

### 5.3 Redis 优化

**1. Pipeline 批量操作**
```java
// 批量查询库存
public Map<Long, Integer> batchGetStock(List<Long> skuIds) {
    List<Object> results = redisTemplate.executePipelined(
        new SessionCallback<Object>() {
            @Override
            public Object execute(RedisOperations operations) {
                for (Long skuId : skuIds) {
                    operations.opsForValue().get("stock:sku:" + skuId);
                }
                return null;
            }
        }
    );

    // 组装结果
    Map<Long, Integer> stockMap = new HashMap<>();
    for (int i = 0; i < skuIds.size(); i++) {
        stockMap.put(skuIds.get(i), (Integer) results.get(i));
    }
    return stockMap;
}
```

**2. 热点数据本地缓存**
```java
// 秒杀商品库存本地缓存（减少 Redis 压力）
LoadingCache<Long, Integer> hotStockCache = Caffeine.newBuilder()
    .maximumSize(1000)
    .expireAfterWrite(10, TimeUnit.SECONDS)  // 10秒过期
    .build(skuId -> getStockFromRedis(skuId));
```

---

## 六、测试报告模板

### 6.1 报告结构

```markdown
# SCM Platform 性能测试报告

## 1. 测试概述
- 测试日期: 2025-12-24
- 测试环境: 生产环境（灰度集群）
- 测试工具: JMeter 5.6, wrk

## 2. 测试场景

| 场景 | 并发 | 持续时间 | 目标TPS | 实际TPS | P99延迟 | 错误率 | 结论 |
|-----|------|---------|---------|---------|---------|--------|------|
| 商品搜索 | 5000 | 30min | 50,000 | 52,341 | 95ms | 0.05% | ✅ 通过 |
| 订单创建 | 500 | 30min | 10,000 | 10,523 | 98ms | 0.08% | ✅ 通过 |
| 秒杀场景 | 10000 | 1min | 100,000 | 105,678 | 185ms | 0.5% | ✅ 通过 |

## 3. 性能瓶颈

### 3.1 数据库连接池满
**现象**: 订单创建 TPS 达到 8000 时，数据库连接池耗尽

**原因**: HikariCP 连接池配置过小（100）

**解决**: 调整 maximum-pool-size = 200

### 3.2 Elasticsearch GC频繁
**现象**: 商品搜索 QPS 达到 40,000 时，ES节点 GC 时间占比 15%

**原因**: Heap 设置过小（8GB）

**解决**: 调整 ES_JAVA_OPTS="-Xms16g -Xmx16g"

## 4. 优化建议

1. 增加 Redis 集群节点（3 → 6）
2. Elasticsearch 索引分片策略优化
3. Dubbo 线程池调优

## 5. 结论

✅ 系统满足性能目标，可以上线
```

---

## 七、性能监控

### 7.1 Grafana Dashboard

**关键指标**:
```
- API QPS / TPS
- API P50 / P90 / P99 延迟
- JVM Heap使用率
- GC次数 + GC时间
- 数据库连接数
- Redis命中率
- Elasticsearch QPS
- Dubbo 调用延迟
```

### 7.2 告警规则

```yaml
# Prometheus 告警
groups:
  - name: performance-alerts
    rules:
      # API 延迟过高
      - alert: HighAPILatency
        expr: histogram_quantile(0.99, http_request_duration_seconds_bucket) > 0.1
        for: 5m
        labels:
          severity: warning

      # TPS 下降
      - alert: LowTPS
        expr: rate(http_requests_total[5m]) < 5000
        for: 5m

      # 数据库连接池满
      - alert: DBPoolExhausted
        expr: hikaricp_connections_active >= hikaricp_connections_max * 0.9
        for: 2m
```

---

**文档维护**: 性能工程师
**审批**: 技术总监
**版本**: v1.0
**最后更新**: 2025-12-24

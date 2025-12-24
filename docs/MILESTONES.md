# SCM Platform 开发里程碑速查表

> **快速参考**: 12 周开发计划的关键里程碑和验收标准

## 📅 开发时间线

```
Week 1     Week 2-3        Week 4-5         Week 6-7          Week 8-9         Week 10       Week 11-12
  │            │               │                │                 │               │              │
  ├─────┬──────┼───────┬───────┼────────┬───────┼─────────┬───────┼───────┬───────┼──────┬───────┤
  │基础 │分布式事务│商品+搜索│库存高并发│订单+状态机│仓库物流│性能优化压测│
  │设施 │  +调度  │         │         │         │        │           │
  └─────┴─────────┴──────────┴──────────┴──────────┴────────┴────────────┘
```

---

## 🎯 六大阶段总览

### Phase 0: 基础设施准备（W1）
**目标**: 搭建完整的开发环境，确保团队可以开始编码

| 任务 | 工作量 | 关键产出 |
|-----|--------|---------|
| 中间件部署 | 2天 | Nacos, Redis, ES, Seata, XXL-Job 全部就绪 |
| 数据库设计 | 2天 | 30+ 张表设计完成，ER 图绘制 |
| CI/CD 配置 | 1天 | GitHub Actions 可用，Docker 镜像构建成功 |
| 开发规范 | 1天 | 代码规范、API 设计指南、Git 工作流文档 |

**验收门槛**:
- [ ] `docker-compose ps` 显示所有服务 healthy
- [ ] PostgreSQL 可连接，30+ 张表创建成功
- [ ] GitHub Actions 至少成功运行 1 次
- [ ] 代码覆盖率基线 > 60%

---

### Phase 1: 分布式事务与调度（W2-3）
**目标**: Seata + XXL-Job 集成完成，分布式事务可用

| 任务 | 工作量 | 关键产出 |
|-----|--------|---------|
| Seata Server 配置 | 1天 | Seata Server 启动，Nacos 注册成功 |
| 订单服务集成 Seata | 2天 | 订单创建分布式事务成功 |
| 库存服务集成 Seata | 1天 | 库存扣减参与全局事务 |
| XXL-Job 集成 | 2天 | 订单超时取消任务定时执行 |
| Seata TCC 模式 | 2天 | TCC 库存预占实现 |

**验收门槛**:
- [ ] Seata AT 模式事务成功率 100%
- [ ] 订单创建失败时库存回滚成功率 100%
- [ ] XXL-Job 任务调度稳定运行，无遗漏
- [ ] TCC 模式 TPS > AT 模式 50%

**核心代码示例**:
```java
@GlobalTransactional(name = "create-order-tx", rollbackFor = Exception.class)
public OrderDTO createOrder(CreateOrderRequest request) {
    // 1. 创建订单（本地）
    orderMapper.insert(order);
    // 2. 扣减库存（远程 RPC）
    inventoryClient.deductStock(request);
    // 3. 创建支付单（远程 RPC）
    paymentClient.createPayment(request);
}
```

---

### Phase 2: 商品服务 + Elasticsearch 搜索（W4-5）
**目标**: 商品 CRUD 完成，ES 搜索上线，Canal 实时同步

| 任务 | 工作量 | 关键产出 |
|-----|--------|---------|
| 商品服务 CRUD | 2天 | 商品增删改查 API 完成 |
| Elasticsearch 集成 | 3天 | IK 分词器安装，搜索 API 实现 |
| Canal 实时同步 | 2天 | MySQL → ES 自动同步 |

**验收门槛**:
- [ ] 商品 CRUD API 全部测试通过
- [ ] ES 搜索准确率 > 95%
- [ ] ES 搜索响应时间 < 100ms (p99)
- [ ] Canal 同步延迟 < 1 秒
- [ ] MySQL 与 ES 数据一致性 100%

**核心代码示例**:
```java
// Canal 监听器自动同步
private void handleInsert(RowData rowData) {
    UUID productId = extractProductId(rowData);
    Product product = productMapper.selectById(productId);
    ProductDocument doc = ProductConverter.toDocument(product);
    searchService.saveDocument(doc); // 保存到 ES
}
```

---

### Phase 3: 库存服务高并发实现（W6-7）
**目标**: Redis Lua 原子扣减，TPS > 10000

| 任务 | 工作量 | 关键产出 |
|-----|--------|---------|
| Redis Lua 原子扣减 | 2天 | 库存扣减、预占、释放 Lua 脚本 |
| 分布式锁 | 1天 | 库存调拨、盘点使用分布式锁 |
| 库存服务 API | 2天 | 库存查询、扣减、调拨 API |
| 库存预警 | 1天 | 低库存自动预警 |

**验收门槛**:
- [ ] Redis Lua 原子扣减准确率 100%
- [ ] 并发测试：1000 并发扣减，数据一致性 100%
- [ ] 性能测试：单 SKU 扣减 TPS > 10000
- [ ] 库存预占成功率 100%
- [ ] 分布式锁获取延迟 < 10ms (p95)

**核心代码示例**:
```lua
-- deduct_stock.lua
local stock = tonumber(redis.call('GET', KEYS[1]) or '0')
if stock >= tonumber(ARGV[1]) then
    redis.call('DECRBY', KEYS[1], ARGV[1])
    return stock - tonumber(ARGV[1])
else
    return -1
end
```

---

### Phase 4: 订单服务 + 状态机（W8-9）
**目标**: Spring State Machine 实现订单流转，TPS > 10000

| 任务 | 工作量 | 关键产出 |
|-----|--------|---------|
| Spring State Machine 配置 | 2天 | 11 种状态 + 7 种事件定义 |
| 订单服务完整实现 | 3天 | 创建、支付、发货、取消、退款 |

**验收门槛**:
- [ ] 订单创建 TPS > 10000
- [ ] 订单状态流转准确率 100%
- [ ] Seata 分布式事务成功率 100%
- [ ] 订单超时取消准确率 100%
- [ ] API 响应时间 < 100ms (p95)

**状态流转**:
```
PENDING_PAYMENT → PAY → PAID → SHIP → SHIPPED → DELIVERED → RECEIVE → COMPLETED
                    ↓
                 CANCEL
                    ↓
                CANCELLED
```

---

### Phase 5: 仓库与物流服务（W10）
**目标**: 仓库管理和物流跟踪完成

| 任务 | 工作量 | 关键产出 |
|-----|--------|---------|
| 仓库服务 | 2天 | 入库单、出库单、库存调拨 |
| 物流服务 | 3天 | 物流单创建、轨迹查询、定时同步 |

**验收门槛**:
- [ ] 仓库服务 API 响应时间 < 50ms (p95)
- [ ] 物流轨迹同步延迟 < 5 分钟
- [ ] 物流状态与订单状态联动准确率 100%

---

### Phase 6: 性能优化与压测（W11-12）
**目标**: 全链路性能优化，达到生产级性能指标

| 任务 | 工作量 | 关键产出 |
|-----|--------|---------|
| 三级缓存优化 | 2天 | Caffeine → Redis → MySQL |
| 热点商品保护 | 1天 | Sentinel 热点参数限流 |
| JMeter 压力测试 | 3天 | 4 大场景压测报告 |

**验收门槛**:
- [ ] 网关 QPS > 100000
- [ ] 订单创建 TPS > 10000
- [ ] 库存扣减 TPS > 20000
- [ ] 商品搜索 QPS > 50000
- [ ] P99 延迟 < 100ms
- [ ] 错误率 < 0.1%
- [ ] 秒杀场景超卖率 = 0%

---

## 📊 关键性能指标 KPI

| 服务/场景 | 指标类型 | 目标值 | 当前值 | 达标状态 |
|----------|---------|--------|--------|---------|
| **商品搜索** | QPS | > 50000 | - | ⏳ 待测试 |
| **商品搜索** | P99 延迟 | < 100ms | - | ⏳ 待测试 |
| **订单创建** | TPS | > 10000 | - | ⏳ 待测试 |
| **订单创建** | P95 延迟 | < 100ms | - | ⏳ 待测试 |
| **库存扣减** | TPS | > 20000 | - | ⏳ 待测试 |
| **库存扣减** | P95 延迟 | < 50ms | - | ⏳ 待测试 |
| **API 网关** | QPS | > 100000 | - | ⏳ 待测试 |
| **API 网关** | P99 延迟 | < 100ms | - | ⏳ 待测试 |
| **Redis 缓存** | 命中率 | > 95% | - | ⏳ 待测试 |
| **Seata 事务** | 成功率 | 100% | - | ⏳ 待测试 |
| **Canal 同步** | 延迟 | < 1s | - | ⏳ 待测试 |
| **秒杀场景** | 超卖率 | 0% | - | ⏳ 待测试 |

---

## 🚀 快速开始

### 开发环境准备
```bash
# 1. 启动基础设施
cd scm-platform
docker-compose -f docker-compose-infra.yml up -d

# 2. 初始化数据库
psql -U postgres -d scm_platform -f scripts/init-db.sql

# 3. 启动服务（按顺序）
cd scm-auth && mvn spring-boot:run       # Auth 服务
cd scm-product && mvn spring-boot:run    # 商品服务
cd scm-inventory && mvn spring-boot:run  # 库存服务
cd scm-order && mvn spring-boot:run      # 订单服务
cd scm-gateway && mvn spring-boot:run    # 网关
```

### 验证环境
```bash
# 检查 Nacos 服务注册
curl http://localhost:8848/nacos/v1/ns/service/list

# 检查网关健康
curl http://localhost:9095/actuator/health

# 查看 API 文档
open http://localhost:9095/doc.html
```

---

## 📁 文档索引

| 文档 | 描述 | 链接 |
|-----|------|-----|
| 详细实施计划（Phase 0-2） | 基础设施、分布式事务、商品搜索 | [IMPLEMENTATION_ROADMAP.md](./IMPLEMENTATION_ROADMAP.md) |
| 详细实施计划（Phase 3-6） | 库存、订单、仓库物流、压测 | [IMPLEMENTATION_ROADMAP_PART2.md](./IMPLEMENTATION_ROADMAP_PART2.md) |
| 架构设计方案 | 完整的系统设计文档 | [../NewNearSync/SCM_DESIGN_PLAN.md](../../NewNearSync/SCM_DESIGN_PLAN.md) |
| 项目 README | 项目介绍和快速开始 | [../README.md](../README.md) |

---

## ✅ 每周交付检查清单

### Week 1 交付物
- [ ] `docker-compose-infra.yml` (中间件配置)
- [ ] `scripts/init-db.sql` (数据库初始化脚本)
- [ ] `docs/infrastructure-setup.md` (部署文档)
- [ ] `docs/database-design.md` (数据库设计文档)
- [ ] `.github/workflows/ci.yml` (CI 配置)

### Week 2-3 交付物
- [ ] `scm-order/OrderServiceImpl.java` (AT 模式)
- [ ] `scm-order/OrderTccServiceImpl.java` (TCC 模式)
- [ ] `scm-order/OrderTimeoutJob.java` (XXL-Job)
- [ ] `docs/seata-integration-guide.md`
- [ ] `docs/distributed-transaction-test-report.md`

### Week 4-5 交付物
- [ ] `scm-product/ProductSearchService.java`
- [ ] `scm-product/ProductCanalListener.java`
- [ ] `resources/lua/*.lua` (Lua 脚本)
- [ ] `docs/elasticsearch-integration.md`
- [ ] `docs/canal-setup-guide.md`

### Week 6-7 交付物
- [ ] `scm-inventory/RedisInventoryService.java`
- [ ] `scm-inventory/InventoryLockService.java`
- [ ] `docs/redis-lua-performance-test.md`

### Week 8-9 交付物
- [ ] `scm-order/OrderStateMachineConfig.java`
- [ ] `scm-order/OrderStateMachineService.java`
- [ ] `docs/order-state-machine-diagram.png`
- [ ] `docs/order-service-performance-test.md`

### Week 10 交付物
- [ ] `scm-warehouse/WarehouseServiceImpl.java`
- [ ] `scm-logistics/LogisticsServiceImpl.java`

### Week 11-12 交付物
- [ ] `ThreeLevelCacheConfig.java`
- [ ] `HotProductProtectionService.java`
- [ ] `docs/jmeter-test-report.md`
- [ ] `docs/performance-optimization-summary.md`

---

## 🎓 团队技能要求

| 角色 | 人数 | 技能要求 |
|-----|------|---------|
| **架构师** | 1 | Spring Cloud、Seata、Elasticsearch、Redis、性能优化 |
| **后端开发** | 2-3 | Java 21、Spring Boot 4、MyBatis-Plus、分布式系统 |
| **测试工程师** | 1 | JMeter、自动化测试、性能测试 |
| **DevOps** | 0.5 | Docker、Kubernetes、CI/CD、监控告警 |

---

## 📞 联系方式

- **项目仓库**: https://github.com/dengxingzhi000/scm-platform
- **问题反馈**: https://github.com/dengxingzhi000/scm-platform/issues
- **文档网站**: 待部署

---

**最后更新**: 2025-12-24
**文档版本**: v1.0
**维护者**: SCM Platform Team
# 架构决策记录 (Architecture Decision Records)

> 本文档记录 SCM Platform 项目的重要架构决策，包括技术选型、架构模式、权衡分析。
>
> **参考标准**: ThoughtWorks ADR Template + 阿里技术架构评审规范

---

## ADR-001: 选择 Seata AT 模式作为主要分布式事务方案

**状态**: ✅ 已采纳
**决策日期**: 2025-12-24
**决策人**: 架构组
**影响范围**: 订单服务、库存服务、支付服务

### 背景 (Context)

SCM 平台需要保证订单创建时跨服务的数据一致性：
- 订单服务创建订单记录
- 库存服务扣减库存
- 支付服务创建支付单

在分布式环境下，这些操作分布在不同的微服务和数据库中，需要分布式事务方案。

### 决策 (Decision)

选择 **Seata AT 模式** 作为主要分布式事务方案，同时在高并发场景（如秒杀）使用 **Seata TCC 模式** 作为补充。

### 理由 (Rationale)

**为什么选择 Seata？**
1. **开源社区活跃**: 阿里开源，Star 数 24k+，企业级实践验证
2. **Spring Cloud 原生支持**: 与 Spring Cloud Alibaba 深度集成
3. **多种事务模式**: AT、TCC、Saga、XA 四种模式可选
4. **性能优异**: AT 模式对业务代码侵入小，TCC 模式高性能

**为什么选择 AT 模式作为主要方案？**
1. **业务侵入小**: 只需在入口方法加 `@GlobalTransactional` 注解
2. **开发效率高**: 不需要手动编写补偿逻辑
3. **适合 CRUD 场景**: 订单创建、库存扣减都是典型 CRUD 操作
4. **自动回滚**: 通过 undo_log 自动回滚，无需额外代码

**为什么在高并发场景使用 TCC 模式？**
1. **性能更高**: TCC 模式不依赖数据库锁，并发性能更好
2. **灵活性强**: Try-Confirm-Cancel 三阶段可精细控制库存锁定
3. **适合秒杀**: 高并发场景下性能提升 50%+

**对比其他方案**:

| 方案 | 优点 | 缺点 | 是否采纳 |
|-----|------|------|---------|
| **Seata AT** | 业务侵入小，开发效率高 | 依赖数据库行锁，高并发性能受限 | ✅ 主要方案 |
| **Seata TCC** | 高性能，灵活性强 | 需要手动编写 Try/Confirm/Cancel | ✅ 高并发补充 |
| **本地消息表** | 最终一致性，性能好 | 不保证强一致性，开发复杂 | ❌ 不适合订单场景 |
| **Saga** | 适合长事务 | 补偿逻辑复杂，不保证隔离性 | ❌ 订单是短事务 |
| **XA** | 强一致性 | 性能差（2PC），锁时间长 | ❌ 性能不满足 |

### 后果 (Consequences)

**正面影响**:
- ✅ 订单创建的数据一致性得到保证
- ✅ 开发效率高，团队学习成本低
- ✅ 性能满足业务要求（TPS > 10000）

**负面影响**:
- ⚠️ 依赖 Seata Server，需要保证 Seata 高可用
- ⚠️ 每个数据库需要创建 undo_log 表
- ⚠️ 全局事务超时时间需要合理配置（建议 30s）

**风险缓解措施**:
1. Seata Server 部署 3 节点集群，注册到 Nacos
2. undo_log 表定期清理（保留 7 天）
3. 全局事务超时监控告警
4. 关键业务路径增加本地事务补偿兜底

### 参考资料

- Seata 官方文档: https://seata.io/zh-cn/docs/overview/what-is-seata.html
- 阿里云 Seata 最佳实践: https://help.aliyun.com/document_detail/xxx.html
- 内部分享：《Seata 在订单系统的应用》（待补充）

---

## ADR-002: 选择 Elasticsearch + Canal 实现商品搜索

**状态**: ✅ 已采纳
**决策日期**: 2025-12-24
**决策人**: 架构组
**影响范围**: 商品服务、搜索服务

### 背景

商品搜索需求：
- 支持全文搜索（商品名称、描述）
- 支持多条件筛选（分类、价格区间、品牌）
- 支持复杂排序（相关性、销量、价格）
- 响应时间 < 100ms (p99)
- QPS > 50000

MySQL 无法满足性能要求，需要引入搜索引擎。

### 决策

采用 **Elasticsearch 8.11** + **Canal 1.1.7** 实现商品搜索：
- Elasticsearch 作为搜索引擎
- Canal 监听 MySQL binlog 实时同步数据到 ES
- IK 分词器处理中文分词

### 理由

**为什么选择 Elasticsearch？**
1. **性能优异**: 基于 Lucene，倒排索引，支持亿级数据毫秒级查询
2. **功能强大**: 全文搜索、聚合分析、地理位置搜索
3. **生态成熟**: Kibana 可视化、Logstash 数据采集
4. **大厂验证**: 京东、携程、美团等都在使用

**为什么使用 Canal 同步数据？**
1. **实时性高**: 基于 binlog，延迟 < 1 秒
2. **对业务无侵入**: 不需要修改业务代码
3. **可靠性高**: 阿里开源，经过双十一验证
4. **支持多种目标**: 可同步到 ES、Redis、MQ

**对比其他方案**:

| 方案 | 优点 | 缺点 | 是否采纳 |
|-----|------|------|---------|
| **ES + Canal** | 实时性高，无侵入，可靠 | 需要维护 Canal | ✅ 采纳 |
| **ES + 双写** | 简单直接 | 可能数据不一致，代码侵入 | ❌ 风险高 |
| **ES + MQ** | 解耦，可靠 | 多一层 MQ，复杂度高 | ❌ 过度设计 |
| **Solr** | 功能类似 ES | 社区活跃度不如 ES | ❌ ES 生态更好 |
| **MySQL 全文索引** | 简单 | 性能差，中文支持差 | ❌ 性能不满足 |

### 后果

**正面影响**:
- ✅ 搜索性能满足要求（QPS > 50000，P99 < 100ms）
- ✅ 支持复杂搜索场景（全文、筛选、排序、聚合）
- ✅ 数据实时同步，用户体验好

**负面影响**:
- ⚠️ 增加 ES 和 Canal 两个中间件，运维复杂度提升
- ⚠️ 需要保证 MySQL 与 ES 数据一致性
- ⚠️ ES 集群需要专业运维（堆内存、JVM 调优）

**风险缓解措施**:
1. ES 部署 3 节点集群（1 master + 2 data），保证高可用
2. Canal 部署主备模式，避免单点故障
3. 定期数据对账任务（每天凌晨 3 点）
4. ES 索引设置合理的 shards（3 分片）和 replicas（2 副本）
5. 监控 Canal 同步延迟，超过 5s 告警

### 索引设计

```json
{
  "settings": {
    "number_of_shards": 3,
    "number_of_replicas": 2,
    "refresh_interval": "1s",
    "analysis": {
      "analyzer": {
        "ik_analyzer": {
          "type": "custom",
          "tokenizer": "ik_max_word"
        }
      }
    }
  },
  "mappings": {
    "properties": {
      "productName": {
        "type": "text",
        "analyzer": "ik_max_word",
        "search_analyzer": "ik_smart"
      },
      "categoryName": {
        "type": "keyword"
      },
      "price": {
        "type": "double"
      },
      "salesCount": {
        "type": "long"
      }
    }
  }
}
```

### 参考资料

- Elasticsearch 官方文档: https://www.elastic.co/guide/en/elasticsearch/reference/8.11/index.html
- Canal 官方文档: https://github.com/alibaba/canal/wiki
- 京东商品搜索架构演进: https://...
- 美团搜索架构实践: https://...

---

## ADR-003: 选择 Redis Lua 实现原子库存扣减

**状态**: ✅ 已采纳
**决策日期**: 2025-12-24
**决策人**: 架构组
**影响范围**: 库存服务

### 背景

库存扣减是高并发场景，需要：
- 原子性保证（不能超卖）
- 高性能（TPS > 10000）
- 支持库存预占与超时释放

MySQL 乐观锁方案在高并发下性能不足。

### 决策

使用 **Redis + Lua 脚本** 实现库存扣减：
- 库存数据存储在 Redis（`stock:skuId`）
- 使用 Lua 脚本保证原子性
- 定期同步 Redis 库存到 MySQL（异步）

### 理由

**为什么选择 Redis Lua？**
1. **原子性保证**: Lua 脚本在 Redis 中原子执行，不会被其他命令打断
2. **高性能**: Redis 单线程模型，无锁竞争，TPS > 100000
3. **灵活性强**: Lua 脚本可实现复杂逻辑（库存预占、超时释放）
4. **成熟方案**: 阿里、京东、美团等大厂都在使用

**对比其他方案**:

| 方案 | 优点 | 缺点 | 是否采纳 |
|-----|------|------|---------|
| **Redis Lua** | 原子性，高性能，灵活 | 需要定期同步到 MySQL | ✅ 采纳 |
| **MySQL 悲观锁** | 强一致性 | 性能差，锁竞争严重 | ❌ 性能不满足 |
| **MySQL 乐观锁** | 无锁，性能较好 | 高并发下冲突多，重试开销大 | ❌ 性能不满足 |
| **分布式锁** | 灵活 | 性能不如 Lua，实现复杂 | ❌ 过度复杂 |

### Lua 脚本示例

```lua
-- 原子扣减库存
local key = KEYS[1]
local quantity = tonumber(ARGV[1])
local stock = tonumber(redis.call('GET', key) or '0')

if stock >= quantity then
    redis.call('DECRBY', key, quantity)
    return stock - quantity
else
    return -1
end
```

### 后果

**正面影响**:
- ✅ 库存扣减 TPS > 10000
- ✅ 绝对不会超卖
- ✅ 支持库存预占与超时自动释放

**负面影响**:
- ⚠️ Redis 与 MySQL 数据不一致风险
- ⚠️ Redis 宕机会导致库存数据丢失

**风险缓解措施**:
1. Redis 持久化配置：AOF + RDB 双重保障
2. Redis 哨兵模式：1 主 2 从，自动故障转移
3. 定期同步 Redis → MySQL（每 10 分钟）
4. 库存数据对账任务（每天凌晨 2 点）
5. Redis 宕机时降级到 MySQL 乐观锁

### 参考资料

- Redis Lua 官方文档: https://redis.io/commands/eval
- 阿里双十一库存扣减方案: https://...
- 美团库存系统架构: https://...

---

## ADR-004: 选择 Spring State Machine 实现订单状态流转

**状态**: ✅ 已采纳
**决策日期**: 2025-12-24
**决策人**: 架构组
**影响范围**: 订单服务

### 背景

订单生命周期复杂，涉及 11 种状态：
```
PENDING_PAYMENT → PAID → PENDING_SHIP → SHIPPED → IN_TRANSIT
  → OUT_FOR_DELIVERY → DELIVERED → COMPLETED
```

需要：
- 状态转换规则清晰
- 防止非法状态转换
- 状态变更时触发业务逻辑（如发货时创建物流单）

### 决策

使用 **Spring State Machine** 实现订单状态机。

### 理由

**为什么选择 Spring State Machine？**
1. **Spring 生态**: 与 Spring Boot 深度集成，配置简单
2. **功能完善**: 支持 Guard（前置条件）、Action（动作）、Listener（监听器）
3. **持久化支持**: 可持久化状态到 Redis/MySQL
4. **可视化**: 可生成状态转换图

**对比其他方案**:

| 方案 | 优点 | 缺点 | 是否采纳 |
|-----|------|------|---------|
| **Spring State Machine** | Spring 生态，功能完善 | 学习曲线陡峭 | ✅ 采纳 |
| **手写状态机** | 简单直接 | 代码冗余，难维护 | ❌ 可维护性差 |
| **if-else 判断** | 最简单 | 代码混乱，容易出错 | ❌ 不可接受 |
| **Camunda/Activiti** | 功能强大 | 过度重量级 | ❌ 过度设计 |

### 状态转换配置示例

```java
@Configuration
@EnableStateMachineFactory
public class OrderStateMachineConfig {

    @Override
    public void configure(StateMachineTransitionConfigurer transitions) {
        transitions
            .withExternal()
                .source(OrderState.PENDING_PAYMENT)
                .target(OrderState.PAID)
                .event(OrderEvent.PAY)
                .action(context -> {
                    // 支付成功后的业务逻辑
                    paymentService.processPayment();
                })
            .and()
            .withExternal()
                .source(OrderState.PAID)
                .target(OrderState.PENDING_SHIP)
                .guard(context -> {
                    // 检查库存是否充足
                    return inventoryService.checkStock();
                });
    }
}
```

### 后果

**正面影响**:
- ✅ 状态转换逻辑清晰，易于维护
- ✅ 防止非法状态转换
- ✅ 业务逻辑与状态机解耦

**负面影响**:
- ⚠️ 学习成本较高
- ⚠️ 配置较为复杂

**风险缓解措施**:
1. 团队培训：组织 Spring State Machine 专题培训
2. 文档完善：绘制状态转换图，编写配置文档
3. 单元测试：覆盖所有状态转换路径

---

## ADR-005: 选择 XXL-Job 作为分布式任务调度框架

**状态**: ✅ 已采纳
**决策日期**: 2025-12-24
**决策人**: 架构组
**影响范围**: 订单服务、库存服务、物流服务

### 背景

需要分布式任务调度功能：
- 订单超时自动取消（每 5 分钟）
- 库存预警（每小时）
- 物流轨迹同步（每 10 分钟）

需要支持：
- 分布式执行（多实例负载均衡）
- 任务失败重试
- 任务执行日志
- 动态调整 Cron 表达式

### 决策

使用 **XXL-Job 2.4.3** 作为分布式任务调度框架。

### 理由

**为什么选择 XXL-Job？**
1. **轻量级**: 部署简单，无额外依赖
2. **功能完善**: 支持 Cron、固定频率、固定延迟等多种调度策略
3. **管理界面**: Web 控制台，任务管理、日志查看
4. **分布式支持**: 路由策略、故障转移、负载均衡
5. **国内流行**: 大量中小企业使用，社区活跃

**对比其他方案**:

| 方案 | 优点 | 缺点 | 是否采纳 |
|-----|------|------|---------|
| **XXL-Job** | 轻量级，功能完善，国内流行 | 文档中文为主 | ✅ 采纳 |
| **Quartz** | 老牌，稳定 | 不支持分布式，功能有限 | ❌ 不支持分布式 |
| **Elastic-Job** | 功能强大 | 依赖 ZooKeeper，复杂 | ❌ 过度复杂 |
| **Spring @Scheduled** | 简单 | 不支持分布式，不支持动态调整 | ❌ 功能不足 |
| **SchedulerX（阿里云）** | 功能最强 | 商业产品，成本高 | ❌ 成本考虑 |

### 后果

**正面影响**:
- ✅ 任务调度稳定可靠
- ✅ 管理界面友好，运维方便
- ✅ 支持分布式执行，可水平扩展

**负面影响**:
- ⚠️ 需要部署 XXL-Job Admin
- ⚠️ 任务数量多时管理复杂

**风险缓解措施**:
1. XXL-Job Admin 部署 2 节点（主备）
2. 任务分类管理（按服务分组）
3. 任务执行日志定期清理（保留 30 天）

---

## ADR-006: 选择 Caffeine + Redis 两级缓存

**状态**: ✅ 已采纳
**决策日期**: 2025-12-24
**决策人**: 架构组
**影响范围**: 商品服务、库存服务

### 背景

热点数据访问频繁：
- 热门商品信息查询 QPS > 10000
- 库存查询 QPS > 5000

需要缓存方案降低数据库压力。

### 决策

采用 **Caffeine（JVM 本地缓存）+ Redis（分布式缓存）** 两级缓存架构。

### 理由

**为什么使用两级缓存？**
1. **性能**: Caffeine 本地缓存延迟 < 1ms，比 Redis 快 10 倍
2. **高可用**: Caffeine 不依赖网络，Redis 宕机不影响 L1 缓存
3. **成本**: 降低 Redis 网络开销，节省带宽

**为什么选择 Caffeine？**
1. **性能最优**: 基于 ConcurrentHashMap + LRU，比 Guava Cache 快 30%
2. **功能强大**: 支持过期策略、淘汰策略、统计信息
3. **Spring 官方推荐**: Spring Boot 默认集成

**缓存架构**:
```
请求 → Caffeine (L1) → Redis (L2) → MySQL (L3)
         ↓ 命中              ↓ 命中       ↓ 查询
      <1ms 返回         <5ms 返回     <50ms 返回
```

### 后果

**正面影响**:
- ✅ 缓存命中率 > 95%
- ✅ L1 缓存命中延迟 < 1ms
- ✅ L2 缓存命中延迟 < 5ms
- ✅ 数据库 QPS 降低 90%

**负面影响**:
- ⚠️ 缓存一致性问题（L1 和 L2 可能不一致）
- ⚠️ 内存占用增加

**缓存一致性方案**:
1. 使用 Redis Pub/Sub 广播缓存失效消息
2. Caffeine 监听失效消息，清理本地缓存
3. 设置合理的过期时间（Caffeine 5 分钟，Redis 10 分钟）

---

## ADR-007: 单网关 + 两个前端入口 + 一套统一身份

**状态**: ✅ 已采纳
**决策日期**: 2026-11-06
**决策人**: 架构组
**影响范围**: `platform-gateway`、`platform-auth`、`platform/common/security`、全部业务服务、管理台前端 `cognora-aura-web`、商城前台

### 背景 (Context)

平台现有 27 个后端服务，全部经由一个 `platform-gateway` 暴露；`platform-auth` 用 HS512 共享密钥签发 JWT，令牌里已带 `tenantId`、`tenantCode` 与 `ROLE_*` 角色矩阵。前端目前只有管理台 `cognora-aura-web`，商城前台（会员、商品浏览、下单、支付）正在规划。

如果不先把入口与身份固定下来，每新增一条业务线（SCM、ecom、LMS）都会自然长出“自己的网关 + 自己的登录 + 自己的角色体系”，最终无法回答三个问题：一个用户到底是谁、这个令牌能调哪些接口、出问题时该在哪一层拦住。

### 决策 (Decision)

**1. 单网关是唯一外部入口。**
所有前端、App、第三方回调都只访问 `platform-gateway`。内部服务之间走 Dubbo / 内部 HTTP，不对外暴露端口。网关负责 JWT 校验、路径级 audience 校验、HMAC 调用签名、CORS、限流、IP 策略，并把已验证的身份以签名头向下游传播。

**2. 两个前端入口按“受众”划分，而不是按业务域。**
- **管理台（Admin Console）**：公司内部员工使用，覆盖 SCM、财务、供应商、仓库、平台管理、审计等后台能力。
- **商城前台（Storefront）**：外部客户/会员使用，覆盖商品浏览、购物车、下单、支付、会员中心。

只有这两个入口。SCM、ecom、LMS 是后端的**服务分组**，不各自拥有独立入口。新增页面默认归入现有两个入口之一；只有当受众、部署边界、合规要求都不同时，才允许新增入口（判断标准见下）。

**3. 身份统一到 `platform-auth`，但两个入口使用不同 OAuth2 client 与不同 audience。**
- `platform-auth` 是唯一签发令牌的地方，继续在令牌中写入 `tenantId`、`tenantCode`、`roles`、`permissions`。
- 两个入口注册为不同 client：`cognora-aura-web`（管理台）与 `cognora-aura-mall`（商城前台）。
- 令牌新增标准声明 `aud`：
  - 管理台令牌 `aud = cognora-admin`
  - 商城令牌 `aud = cognora-mall`
- 网关按 `aud` + 请求路径判定：商城令牌不能访问 `/platform-system/**`、`/platform-tenant/**`、`/platform-audit/**`、`/scm-*/**`、`/lms-*/**` 等后台接口；管理台令牌也不能访问商城专属接口（`/ecom-mall/**`）。业务服务可再按 `security.audience.allowed` 做一层本地校验，避免绕过网关直连。

### 理由 (Rationale)

**为什么不给 SCM / ecom / lms 各开一个入口？**

| 方案 | 优点 | 缺点 | 是否采纳 |
|-----|------|------|---------|
| 单网关 | 一套鉴权、一套审计、一套限流；CORS 与证书只维护一份；故障域清晰 | 网关成为关键路径，需要容量与高可用投入 | ✅ |
| 每业务域一个网关 | 域内自治，故障隔离 | N 套鉴权/审计/限流重复实现；令牌与身份口径容易分叉；跨域调用要穿多个网关，排障成本高 | ❌ |
| 前后端 BFF 网关 | 前端可裁剪接口 | 只是多一层聚合，仍需统一身份；当前阶段收益不足以抵消维护成本 | ❌（后续可作网关后的聚合层） |

判断依据是**受众**与**信任边界**，不是团队组织或业务域。业务域再多，只要使用者仍是同一批内部员工，就应该共享同一个入口与同一套后台令牌。多网关只在“受众不同、合规隔离要求不同、需要独立发布节奏”时才值得付出重复建设的代价。

**为什么统一身份但不共用同一受众？**

- 统一身份解决“一个人是谁、属于哪个租户、有什么角色”，这是所有业务域的共同前提；分叉的登录体系会直接破坏租户隔离与审计完整性。
- 但管理台与商城前台的**信任等级不同**：管理台令牌能改价、退款、授权、看全量数据；商城令牌只应触碰自己的会员数据与前台接口。若两者 audience 相同，一次 XSS 或令牌泄漏就能把商城会话升级成后台操作。
- 受众分离还能让“误配路由”变成显式的 403，而不是静默放行：商城令牌打到 `/platform-system/**` 会在网关被拒绝并留下审计/指标。

**为什么用 `aud` 而不是新增自定义 claim？**
`aud` 是 JWT 标准声明，Spring Security 的 `Jwt`、网关的 `JwtAuthenticationToken`、第三方排查工具都能直接识别，不需要额外的解析约定；同时保留扩展为多受众（数组）的能力。

### 后果 (Consequences)

**正面影响**:
- ✅ 入口、鉴权、审计只有一个实现点，新增业务线不需要再建网关或登录体系。
- ✅ 商城令牌无法触达后台接口，最小权限从“角色配置”前移到“令牌受众”。
- ✅ 令牌受众可在网关与业务服务两层校验，绕过网关的直连也会被拦。

**负面影响 / 成本**:
- ⚠️ `platform-auth` 需要同时维护两个 client 与受众映射；新增入口必须同步登记，否则登录会被拒绝。
- ⚠️ 网关多了一条路径策略，需要随服务分组变化维护 allow/deny 列表。
- ⚠️ 已签发的旧令牌没有 `aud`，需要一个有截止时间的兼容窗口。

### 兼容与过渡 (Migration)

1. **签发端先行**：`platform-auth` 立即支持 `clientId` 入参，缺省为管理台 client（保证现有前端不改代码也拿到 `cognora-admin` 受众）；刷新令牌轮换时继承原受众。
2. **校验端宽容旧令牌**：网关与业务服务对**缺少 `aud` 的旧令牌**默认放行并打点（`legacy-token-policy: ALLOW` / `allow-legacy-token: true`）。带 `aud` 的新令牌立即按受众校验，不等待窗口结束。
3. **收紧时间点**：所有客户端都换成新登录流程后，至少等待一个刷新令牌周期（默认 7 天）再切换为 `DENY`；切换动作单独发版，不与本次改动合并。
4. **回滚**：两项均为配置开关（网关 `security.audience.enabled`、服务端 `security.audience.enabled`），回滚只需关闭开关，无需回退代码。

### 新增入口的判断标准

新增第三个前端入口，必须同时满足以下条件，缺一不可：

1. **受众不同**：使用者不是同一批内部员工，也不是同一批外部会员（例如面向供应商或承运商的自助门户）。
2. **信任边界不同**：需要独立的令牌受众、独立的最小权限集合，且不能复用现有两个受众中的任何一个。
3. **部署/发布边界不同**：需要独立域名、独立发布节奏或独立合规要求（如对外网开放、需单独等保备案）。
4. **身份仍复用 `platform-auth`**：新增入口只允许新增 client 与 audience，禁止新建登录服务或新的令牌格式。
5. **网关侧有明确的路径 allow/deny 规则**，并在本 ADR 追加一条记录说明范围。

只满足“页面风格不同”“团队不同”“业务域不同”的，一律归入现有两个入口。

### 参考资料

- RFC 7519 §4.1.3 `aud`（Audience）声明
- `docs/design/ARCHITECTURE.md`
- `platform/gateway/src/main/java/com/cognora/platform/gateway/config/GatewaySecurityConfig.java`
- `platform/common/web/src/main/java/com/cognora/platform/common/security/util/JwtUtils.java`

---

## ADR 变更流程

1. **提出变更**: 任何团队成员可以提出 ADR 变更请求
2. **架构评审**: 架构组评审（每周三下午）
3. **决策**: 架构师最终决策
4. **文档更新**: 更新 ADR 文档，标注状态（已采纳/已拒绝/已废弃）
5. **团队同步**: 在周会上同步 ADR 变更

---

## 参考资料

- ThoughtWorks ADR 模板: https://github.com/joelparkerhenderson/architecture-decision-record
- 阿里技术架构评审规范（内部文档）
- Google SRE Book: https://sre.google/books/
- 微软架构指南: https://docs.microsoft.com/en-us/azure/architecture/

---

**文档维护**: 架构组
**最后更新**: 2025-12-24
**版本**: v1.0

# 敏捷开发排期计划 (Sprint Planning)
# SCM Platform - Agile Development Schedule

> **文档类型**: 敏捷开发排期计划
> **项目经理**: PM负责人
> **开发周期**: 12周 (6个Sprint)
> **团队规模**: 7.5人
> **版本**: v1.0
> **最后更新**: 2025-12-24

---

## 一、Sprint 概览

### 1.1 Sprint 规划

| Sprint | 周期 | 主题 | 目标 | Story Points |
|--------|------|------|------|--------------|
| **Sprint 0** | Week 1-2 | 基础设施搭建 | 环境就绪 + Seata/XXL-Job集成 | 40 |
| **Sprint 1** | Week 3-4 | 商品+库存服务 | 商品管理 + Redis库存扣减 | 55 |
| **Sprint 2** | Week 5-6 | 订单服务 | 订单状态机 + Seata事务 | 60 |
| **Sprint 3** | Week 7-8 | 仓储+物流服务 | WMS + TMS基础功能 | 50 |
| **Sprint 4** | Week 9-10 | 高级功能 | 拣货路径优化 + 智能补货 | 55 |
| **Sprint 5** | Week 11-12 | 性能优化+上线 | 压测 + 优化 + 灰度发布 | 45 |
| **总计** | **12周** | | | **305** |

### 1.2 团队配置

| 角色 | 人数 | 职责 |
|------|------|------|
| 产品经理 (PM) | 1 | 需求梳理、验收 |
| 架构师 | 1 | 技术架构、Code Review |
| Tech Lead | 1 | 技术攻坚、代码质量 |
| 后端开发 | 3 | 功能开发 |
| 测试工程师 | 1 | 测试用例、自动化测试 |
| DevOps | 0.5 | CI/CD、监控告警 |
| **总计** | **7.5人** | |

---

## 二、Sprint 0: 基础设施搭建（Week 1-2）

### 2.1 Sprint 目标

**OKR**:
- **O**: 完成基础设施搭建，技术栈验证
- **KR1**: Docker Compose 环境一键启动
- **KR2**: Seata 分布式事务 Demo 验证通过
- **KR3**: XXL-Job 分布式任务 Demo 验证通过
- **KR4**: Elasticsearch 全文搜索 Demo 验证通过

### 2.2 User Stories

| ID | Story | 估点 | 优先级 | 负责人 |
|----|-------|------|--------|--------|
| US-001 | 作为**开发人员**，我希望**一键启动开发环境**，以便**快速开始开发** | 8 | P0 | DevOps |
| US-002 | 作为**架构师**，我希望**验证Seata AT模式**，以便**确保分布式事务可行** | 13 | P0 | 架构师 |
| US-003 | 作为**架构师**，我希望**验证XXL-Job调度**，以便**确保定时任务可用** | 8 | P0 | 架构师 |
| US-004 | 作为**开发人员**，我希望**验证Elasticsearch搜索**，以便**确保搜索性能** | 8 | P0 | Backend Dev1 |
| US-005 | 作为**团队**，我希望**完成CI/CD Pipeline**，以便**自动化测试部署** | 3 | P1 | DevOps |

**Total Story Points**: 40

### 2.3 详细任务

#### Week 1: 环境搭建

**Day 1-2: Docker Compose 环境**
```yaml
# docker-compose.yml
services:
  nacos:
    image: nacos/nacos-server:v2.2.0
    ports: ["8848:8848"]

  postgres:
    image: postgres:14
    environment:
      POSTGRES_PASSWORD: scm123456

  redis:
    image: redis:7.0

  elasticsearch:
    image: elasticsearch:8.11.4
    environment:
      discovery.type: single-node

  seata-server:
    image: seataio/seata-server:2.0.0
    ports: ["8091:8091"]

  xxl-job-admin:
    image: xuxueli/xxl-job-admin:2.4.0
    ports: ["8080:8080"]
```

**验收标准**:
- [ ] `docker-compose up -d` 一键启动所有服务
- [ ] Nacos 控制台可访问（http://localhost:8848/nacos）
- [ ] XXL-Job 控制台可访问（http://localhost:8080/xxl-job-admin）

**Day 3-5: 数据库初始化**
- [ ] 创建数据库（scm_product, scm_inventory, scm_order等）
- [ ] 初始化表结构（参考 DATABASE_DESIGN.md）
- [ ] 导入测试数据（10,000商品 + 100,000用户）
- [ ] Flyway/Liquibase 数据库版本管理

#### Week 2: 技术验证

**Day 1-3: Seata 分布式事务验证**
```java
// Demo: 订单-库存-支付 分布式事务
@Service
public class OrderService {

    @GlobalTransactional(name = "create-order-tx", rollbackFor = Exception.class)
    public Order createOrder(OrderDTO dto) {
        // 1. 创建订单
        Order order = orderMapper.insert(...);

        // 2. Dubbo RPC 扣减库存
        inventoryService.deductStock(dto.getSkuId(), dto.getQuantity());

        // 3. Dubbo RPC 创建支付单
        paymentService.createPayment(order.getId(), dto.getAmount());

        // 模拟异常，验证回滚
        if (dto.getAmount().compareTo(new BigDecimal(10000)) > 0) {
            throw new BusinessException("金额超限");
        }

        return order;
    }
}
```

**验收标准**:
- [ ] 正常流程: 订单、库存、支付 全部成功
- [ ] 异常回滚: 抛出异常后，订单、库存、支付 全部回滚

**Day 4-5: XXL-Job + Elasticsearch 验证**
- [ ] XXL-Job 定时任务: 每5分钟扫描超时订单
- [ ] Canal 监听 MySQL binlog 同步到 Elasticsearch
- [ ] Elasticsearch IK分词器测试

### 2.4 交付物

- [x] Docker Compose 环境配置文件
- [x] 数据库初始化脚本（Flyway）
- [x] Seata 分布式事务 Demo
- [x] XXL-Job 定时任务 Demo
- [x] Elasticsearch 搜索 Demo
- [x] CI/CD Pipeline（GitHub Actions）
- [x] 技术验证报告（PPT）

---

## 三、Sprint 1: 商品+库存服务（Week 3-4）

### 3.1 Sprint 目标

**OKR**:
- **O**: 完成商品管理和库存管理核心功能
- **KR1**: 商品 CRUD + 搜索 QPS > 10,000
- **KR2**: 库存扣减 TPS > 10,000
- **KR3**: 单元测试覆盖率 > 80%

### 3.2 User Stories

| ID | Story | 估点 | 优先级 | 负责人 |
|----|-------|------|--------|--------|
| US-101 | 作为**运营**，我希望**创建商品（SPU/SKU）**，以便**上架销售** | 8 | P0 | Backend Dev1 |
| US-102 | 作为**用户**，我希望**搜索商品（关键词+筛选）**，以便**快速找到商品** | 13 | P0 | Backend Dev1 |
| US-103 | 作为**系统**，我希望**实时同步商品到ES**，以便**保证搜索数据准确** | 8 | P0 | Backend Dev2 |
| US-104 | 作为**运营**，我希望**扣减库存（Redis Lua）**，以便**支持高并发下单** | 13 | P0 | Backend Dev2 |
| US-105 | 作为**运营**，我希望**预占库存**，以便**防止超卖** | 8 | P0 | Backend Dev2 |
| US-106 | 作为**测试**，我希望**并发测试库存扣减**，以便**验证原子性** | 5 | P0 | QA |

**Total Story Points**: 55

### 3.3 核心代码

#### 库存扣减（Redis Lua 脚本）

**deduct_stock.lua**:
```lua
-- KEYS[1]: 库存key (stock:sku:123)
-- ARGV[1]: 扣减数量
local stock = tonumber(redis.call('GET', KEYS[1]) or '0')

if stock >= tonumber(ARGV[1]) then
    redis.call('DECRBY', KEYS[1], ARGV[1])
    return stock - tonumber(ARGV[1])  -- 返回剩余库存
else
    return -1  -- 库存不足
end
```

**Java 调用**:
```java
@Service
public class InventoryService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RedisScript<Long> deductScript;

    public boolean deductStock(Long skuId, Integer quantity) {
        String key = "stock:sku:" + skuId;

        Long result = redisTemplate.execute(
            deductScript,
            Collections.singletonList(key),
            quantity.toString()
        );

        if (result != null && result >= 0) {
            // 异步同步到数据库
            asyncUpdateDB(skuId, quantity);
            return true;
        }

        return false;
    }
}
```

### 3.4 交付物

- [x] 商品服务 API（CRUD + 搜索）
- [x] 库存服务 API（扣减 + 预占 + 查询）
- [x] Elasticsearch 索引设计 + Mapping
- [x] Canal 实时同步 MySQL → ES
- [x] 单元测试（覆盖率 > 80%）
- [x] API 文档（Swagger）

---

## 四、Sprint 2: 订单服务（Week 5-6）

### 4.1 Sprint 目标

**OKR**:
- **O**: 完成订单管理核心流程
- **KR1**: 订单创建 TPS > 5,000
- **KR2**: Seata 分布式事务成功率 > 99.9%
- **KR3**: 订单状态机覆盖 11 种状态

### 4.2 User Stories

| ID | Story | 估点 | 优先级 | 负责人 |
|----|-------|------|--------|--------|
| US-201 | 作为**用户**，我希望**创建订单**，以便**购买商品** | 13 | P0 | Backend Dev3 |
| US-202 | 作为**系统**，我希望**订单状态自动流转**，以便**减少人工干预** | 13 | P0 | Backend Dev3 |
| US-203 | 作为**系统**，我希望**Seata保证订单-库存-支付一致**，以便**避免数据不一致** | 13 | P0 | Tech Lead |
| US-204 | 作为**系统**，我希望**定时取消超时订单**，以便**释放库存** | 8 | P0 | Backend Dev3 |
| US-205 | 作为**用户**，我希望**查询订单详情+物流轨迹**，以便**了解订单进度** | 8 | P1 | Backend Dev1 |
| US-206 | 作为**测试**，我希望**压测订单创建**，以便**验证性能** | 5 | P0 | QA |

**Total Story Points**: 60

### 4.3 订单状态机

```java
@Configuration
public class OrderStateMachineConfig extends StateMachineConfigurerAdapter<OrderStatus, OrderEvent> {

    @Override
    public void configure(StateMachineStateConfigurer<OrderStatus, OrderEvent> states) throws Exception {
        states
            .withStates()
            .initial(OrderStatus.PENDING_PAYMENT)
            .states(EnumSet.allOf(OrderStatus.class));
    }

    @Override
    public void configure(StateMachineTransitionConfigurer<OrderStatus, OrderEvent> transitions) throws Exception {
        transitions
            // 待支付 → 已支付
            .withExternal()
                .source(OrderStatus.PENDING_PAYMENT)
                .target(OrderStatus.PAID)
                .event(OrderEvent.PAYMENT_SUCCESS)
                .action(afterPaymentAction())
            .and()
            // 已支付 → 待发货
            .withExternal()
                .source(OrderStatus.PAID)
                .target(OrderStatus.PENDING_SHIP)
                .event(OrderEvent.WAREHOUSE_ALLOCATED)
            .and()
            // ... 其他状态流转
            ;
    }
}
```

### 4.4 交付物

- [x] 订单服务 API（创建 + 查询 + 取消）
- [x] Spring State Machine 状态机
- [x] Seata 分布式事务集成
- [x] XXL-Job 超时订单定时取消
- [x] 单元测试 + 集成测试
- [x] JMeter 压测脚本

---

## 五、Sprint 3-5（Week 7-12）

### Sprint 3: 仓储+物流（Week 7-8）
- [x] WMS 入库/出库管理
- [x] TMS 运单管理 + 轨迹追踪
- [x] 对接第三方物流（顺丰、菜鸟）

### Sprint 4: 高级功能（Week 9-10）
- [x] 拣货路径优化（TSP算法）
- [x] 智能补货（LSTM预测）
- [x] 波次拣货

### Sprint 5: 性能优化+上线（Week 11-12）
- [x] JMeter 压测（详见《性能基准测试计划》）
- [x] 性能调优（缓存 + 限流 + 降级）
- [x] 灰度发布（详见《上线发布计划》）

---

## 六、每日站会（Daily Standup）

**时间**: 每天 09:30 AM，15分钟

**议程**:
1. 昨天完成了什么？
2. 今天计划做什么？
3. 有哪些阻碍？

**工具**: 飞书/钉钉视频会议

---

## 七、Sprint评审（Sprint Review）

**时间**: 每个 Sprint 最后一天下午 14:00

**参与人**: PM, 架构师, 开发团队, QA

**议程**:
1. Demo 功能演示（30分钟）
2. 验收标准检查（30分钟）
3. 遗留问题讨论（30分钟）

---

## 八、Sprint回顾（Sprint Retrospective）

**时间**: 每个 Sprint 最后一天下午 16:00

**参与人**: 全员

**议程**（开心、惊喜、困惑）:
1. 这个 Sprint 哪些做得好？
2. 哪些需要改进？
3. 下个 Sprint 的行动计划？

---

## 九、风险管理

| 风险 | 概率 | 影响 | 缓解措施 |
|-----|------|------|---------|
| Seata 性能不满足要求 | 中 | 高 | 提前压测，准备 TCC 方案 |
| XXL-Job 单点故障 | 中 | 中 | 集群部署 |
| 人员请假影响进度 | 中 | 中 | 交叉 Review，知识共享 |
| 第三方API不稳定 | 高 | 中 | 降级方案，Mock数据 |

---

**文档维护**: PM + Scrum Master
**审批**: 技术总监
**版本**: v1.0
**最后更新**: 2025-12-24

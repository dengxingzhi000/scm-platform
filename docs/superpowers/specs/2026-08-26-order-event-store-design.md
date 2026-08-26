# 订单本地事件溯源存储启用设计

日期:2026-08-26
状态:已确认(用户批准)
范围:`scm-order/service` 的 `com.scmcloud.order.event` 包 + DDL + 主流程接线

## 背景与问题

`com.scmcloud.order.event` 包(`OrderEvent`、`OrderCreatedEvent`、`OrderStatusChangedEvent`、`OrderEventStore`、`OrderAggregate`)是未接线的死代码:包外零引用,且存在多项缺陷:

1. **模型错配**:事件用 `UUID orderId`,而订单主键是 `Long`;无 `orderNo`、`tenantId`
2. **SQL 拼接**:`OrderEventStore.getEvents(orderId, offset, limit)` 用 `.last("OFFSET " + offset + " LIMIT " + limit)` 拼字符串
3. **排序不稳**:仅按 `create_time` 升序,同毫秒事件顺序不保证
4. **表不存在**:`ord_order_event` 在 `scripts/db/` 无任何 DDL(retention 脚本引用了它);实体缺 `tenant_id`,过不了 CI 校验
5. **裸异常**:序列化/反序列化失败抛 `RuntimeException`
6. **聚合不完整**:无事件重放(rehydrate)API,`changeStatus` 不校验状态机合法性

## 目标

将该包修复并启用为订单**本地事件溯源/审计存储**,接入订单主流程,与现有机制并存互补:

- 与 `OrdStatusHistory`(状态历史表)并存:互不替代
- 与 outbox 领域事件体系(`domain/event/*` → Kafka)并存:事件存储只做本地 append + 重放查询,**不发消息**

## 非目标(YAGNI)

- 不发 Kafka / 不改 outbox 体系
- 不替代 `OrdStatusHistory`
- 不做表分区(retention 已有 3 年清理策略)
- 不做事件快照机制

## 设计

### 1. 模型重构(`order/event` 包)

**`OrderEvent`**(抽象基类,public)字段:

| 字段 | 类型 | 说明 |
|------|------|------|
| eventId | UUID | 事件唯一标识 |
| tenantId | UUID | 租户(对齐 `DomainEvent` 的 UUID tenantId) |
| orderId | Long | 订单主键 |
| orderNo | String | 订单号 |
| timestamp | Instant | 事件时间 |
| eventType | String | Jackson `@JsonTypeInfo` 判别字段 |

补 `equals/hashCode`(基于 eventId)+ `toString`。

子类拆为独立 public 文件(Jackson 多态注解保留):

- `OrderCreatedEvent`:userId(String)、totalAmount(BigDecimal)、payableAmount(BigDecimal)
- `OrderStatusChangedEvent`:fromStatus / toStatus 为 `OrderStatus` 枚举(复用 `domain/entity/OrderStatus`,枚举按 name 序列化)

### 2. `OrderAggregate`

- 新增静态工厂 `rehydrate(List<OrderEvent>)`:按序 `apply()` 重放;首事件必须是 `OrderCreatedEvent`,否则抛 `IllegalStateException`
- `changeStatus(OrderStatus target)`:内部用 `canTransitionTo()` 校验,非法流转抛异常
- 保留 uncommittedEvents 单元工作模式 API

### 3. `OrderEventStore` 修复

| 问题 | 修复 |
|------|------|
| 字符串拼接分页 SQL | MyBatis-Plus `Page` + `selectPage` |
| 排序不稳 | `create_time ASC, id ASC` 兜底 |
| 裸 RuntimeException | `scm-common/core` 的 `ServiceException` |
| 无幂等 | DDL `event_id` 唯一索引;重复插入捕获 `DuplicateKeyException` 记 warn 跳过 |

反序列化依赖 Jackson 多态(`@JsonTypeInfo` + `@JsonSubTypes`)round-trip。

### 4. DDL:`scripts/db/microservices/028_ord_order_event.sql`(db_order 库)

```sql
CREATE TABLE IF NOT EXISTS ord_order_event (
    id          BIGSERIAL PRIMARY KEY,
    tenant_id   UUID         NOT NULL,
    event_id    UUID         NOT NULL,
    order_id    BIGINT       NOT NULL,
    order_no    VARCHAR(64),
    event_type  VARCHAR(64)  NOT NULL,
    event_data  TEXT         NOT NULL,
    create_time TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_ord_order_event_event_id UNIQUE (event_id)
);
CREATE INDEX IF NOT EXISTS idx_ord_order_event_order ON ord_order_event (order_id, create_time);
COMMENT ON TABLE ord_order_event IS '订单本地事件溯源存储(append-only)';
```

实体 `OrdOrderEvent` 同步加 `tenantId`、`orderNo` 字段。

### 5. 主流程接线(同一事务内)

- `OrdOrderServiceImpl.createOrder`:保存成功后 append `OrderCreatedEvent`(tenantId 取自订单)
- `OrdOrderServiceImpl.updateOrderStatus`:流转成功后 append `OrderStatusChangedEvent`
- 其余直接调用 `transitionTo` 的入口(如 `OrdOrderCommandService`)在实现计划阶段逐一排查并同样接入
- append 失败随事务回滚(审计一致性优先于可用性)

### 6. 错误处理

- 序列化失败 → `ServiceException("Failed to serialize order event", cause)`
- 反序列化失败 → `ServiceException` 带 eventId 上下文
- 重复 event_id → 幂等跳过(warn 日志),不视为错误

## 测试策略

- `OrderAggregateTest`:create / rehydrate(含首事件非法)/ 合法与非法状态流转
- 事件多态 JSON 序列化 round-trip 单测(ObjectMapper)
- Store 层测试沿用现有集成测试模式(参照 `OrderLifecycleIntegrationTest`)
- 覆盖率过 jacoco 门槛(70% 行 / 60% 分支)

## 验证命令

```bash
mvn test -pl scm-order/service -f com.scm.parent/pom.xml
mvn clean install -DskipTests -f com.scm.parent/pom.xml
mvn verify -Djacoco.skip=false -f com.scm.parent/pom.xml
```

## 影响面

- 修改:`order/event` 包 3 文件重构为 5 文件;`OrdOrderEvent` 实体;`OrdOrderServiceImpl`;新增 DDL
- 不动:`domain/event/*`、outbox、`OrdStatusHistory`、Dubbo API、其他服务

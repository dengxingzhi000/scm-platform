# 订单事件溯源存储启用实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 `com.scmcloud.order.event` 包从死代码修复并启用为订单本地事件溯源存储,接入订单创建与状态流转主流程。

**Architecture:** 重构现有 3 个类为类型安全的事件模型(Long orderId / OrderStatus 枚举 / tenantId),修复 Store 的 SQL 拼接与异常处理,新增 `ord_order_event` DDL,在两个命令入口(`OrdOrderCommandService`、`OrdOrderServiceImpl`)事务内 append 事件。与 `OrdStatusHistory`、outbox 并存互补。

**Tech Stack:** Java 21、Spring Boot 4、MyBatis-Plus(baomidou)、Jackson 多态序列化、JUnit 5 + Mockito。

**Spec:** `docs/superpowers/specs/2026-08-26-order-event-store-design.md`

**构建环境:** Maven/JDK 不在默认 PATH,使用根目录 `build.bat` 固定的 GraalVM JDK 21 + Maven 路径;所有 mvn 命令带 `-f com.scm.parent/pom.xml`。

---

### Task 1: 事件模型重构 + 实体字段对齐

**Files:**
- Modify: `scm-order/service/src/main/java/com/scmcloud/order/event/OrderEvent.java`(重写)
- Create: `scm-order/service/src/main/java/com/scmcloud/order/event/OrderCreatedEvent.java`
- Create: `scm-order/service/src/main/java/com/scmcloud/order/event/OrderStatusChangedEvent.java`
- Modify: `scm-order/service/src/main/java/com/scmcloud/order/event/OrderAggregate.java`(编译适配,Task 2 增强)
- Modify: `scm-order/service/src/main/java/com/scmcloud/order/event/OrderEventStore.java`(仅 append 补字段)
- Modify: `scm-order/service/src/main/java/com/scmcloud/order/domain/entity/OrdOrderEvent.java`
- Test: `scm-order/service/src/test/java/com/scmcloud/order/event/OrderEventJsonTest.java`

- [ ] **Step 1: 写失败测试(JSON 多态 round-trip)**

```java
package com.scmcloud.order.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.scmcloud.order.domain.entity.OrderStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class OrderEventJsonTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .findAndRegisterModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Test
    void shouldRoundTripOrderCreatedEventPolymorphically() throws Exception {
        OrderCreatedEvent original = new OrderCreatedEvent(
                UUID.fromString("550e8400-e29b-41d4-a716-446655440000"),
                1L, "NO1001", "u-1",
                new BigDecimal("99.90"), new BigDecimal("89.90"));

        String json = objectMapper.writeValueAsString(original);
        OrderEvent deserialized = objectMapper.readValue(json, OrderEvent.class);

        OrderCreatedEvent restored = assertInstanceOf(OrderCreatedEvent.class, deserialized);
        assertEquals(original.getEventType(), restored.getEventType());
        assertEquals(original.getTenantId(), restored.getTenantId());
        assertEquals(original.getOrderId(), restored.getOrderId());
        assertEquals(original.getOrderNo(), restored.getOrderNo());
        assertEquals(original.getUserId(), restored.getUserId());
        assertEquals(original.getTotalAmount(), restored.getTotalAmount());
        assertEquals(original.getPayableAmount(), restored.getPayableAmount());
    }

    @Test
    void shouldRoundTripOrderStatusChangedEventPolymorphically() throws Exception {
        OrderStatusChangedEvent original = new OrderStatusChangedEvent(
                UUID.fromString("550e8400-e29b-41d4-a716-446655440000"),
                2L, "NO1002", OrderStatus.PAID, OrderStatus.PENDING_SHIP);

        String json = objectMapper.writeValueAsString(original);
        OrderEvent deserialized = objectMapper.readValue(json, OrderEvent.class);

        OrderStatusChangedEvent restored = assertInstanceOf(OrderStatusChangedEvent.class, deserialized);
        assertEquals(OrderStatus.PAID, restored.getFromStatus());
        assertEquals(OrderStatus.PENDING_SHIP, restored.getToStatus());
        assertEquals(2L, restored.getOrderId());
    }

    @Test
    void equalsShouldBeBasedOnEventId() {
        UUID shared = UUID.randomUUID();
        OrderCreatedEvent a = new OrderCreatedEvent(shared, 1L, "NO1001", "u-1", BigDecimal.ONE, BigDecimal.ONE);
        OrderCreatedEvent b = new OrderCreatedEvent(shared, 1L, "NO1001", "u-1", BigDecimal.ONE, BigDecimal.ONE);
        OrderCreatedEvent c = new OrderCreatedEvent(UUID.randomUUID(), 1L, "NO1001", "u-1", BigDecimal.ONE, BigDecimal.ONE);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
    }
}
```

- [ ] **Step 2: 运行确认编译失败**

Run: `mvn test -Dtest=OrderEventJsonTest -pl :scm-order-service -f com.scm.parent/pom.xml`
Expected: COMPILATION ERROR(`OrderCreatedEvent` 无对应构造器)

- [ ] **Step 3: 重写 `OrderEvent.java`(删除文件内嵌的包私有子类)**

```java
package com.scmcloud.order.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.time.Instant;
import java.util.UUID;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "eventType", visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = OrderCreatedEvent.class, name = "ORDER_CREATED"),
    @JsonSubTypes.Type(value = OrderStatusChangedEvent.class, name = "ORDER_STATUS_CHANGED")
})
public abstract class OrderEvent {

    private final UUID eventId;
    private final UUID tenantId;
    private final Long orderId;
    private final String orderNo;
    private final Instant timestamp;
    private final String eventType;

    protected OrderEvent(UUID tenantId, Long orderId, String orderNo, String eventType) {
        this(UUID.randomUUID(), tenantId, orderId, orderNo, Instant.now(), eventType);
    }

    @JsonCreator
    protected OrderEvent(@JsonProperty("eventId") UUID eventId,
                          @JsonProperty("tenantId") UUID tenantId,
                          @JsonProperty("orderId") Long orderId,
                          @JsonProperty("orderNo") String orderNo,
                          @JsonProperty("timestamp") Instant timestamp,
                          @JsonProperty("eventType") String eventType) {
        this.eventId = eventId;
        this.tenantId = tenantId;
        this.orderId = orderId;
        this.orderNo = orderNo;
        this.timestamp = timestamp;
        this.eventType = eventType;
    }

    public UUID getEventId() { return eventId; }
    public UUID getTenantId() { return tenantId; }
    public Long getOrderId() { return orderId; }
    public String getOrderNo() { return orderNo; }
    public Instant getTimestamp() { return timestamp; }
    public String getEventType() { return eventType; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OrderEvent other)) return false;
        return eventId.equals(other.eventId);
    }

    @Override
    public int hashCode() {
        return eventId.hashCode();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{eventId=" + eventId + ", orderId=" + orderId
                + ", orderNo='" + orderNo + "', eventType='" + eventType + "'}";
    }
}
```

- [ ] **Step 4: 创建 `OrderCreatedEvent.java`**

```java
package com.scmcloud.order.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * 订单创建事件(本地事件溯源用;跨服务发布走 domain/event 的 outbox 体系)。
 */
public class OrderCreatedEvent extends OrderEvent {

    private final String userId;
    private final BigDecimal totalAmount;
    private final BigDecimal payableAmount;

    public OrderCreatedEvent(UUID tenantId, Long orderId, String orderNo,
                             String userId, BigDecimal totalAmount, BigDecimal payableAmount) {
        super(tenantId, orderId, orderNo, "ORDER_CREATED");
        this.userId = userId;
        this.totalAmount = totalAmount;
        this.payableAmount = payableAmount;
    }

    @JsonCreator
    public OrderCreatedEvent(@JsonProperty("eventId") UUID eventId,
                              @JsonProperty("tenantId") UUID tenantId,
                              @JsonProperty("orderId") Long orderId,
                              @JsonProperty("orderNo") String orderNo,
                              @JsonProperty("timestamp") Instant timestamp,
                              @JsonProperty("eventType") String eventType,
                              @JsonProperty("userId") String userId,
                              @JsonProperty("totalAmount") BigDecimal totalAmount,
                              @JsonProperty("payableAmount") BigDecimal payableAmount) {
        super(eventId, tenantId, orderId, orderNo, timestamp, eventType);
        this.userId = userId;
        this.totalAmount = totalAmount;
        this.payableAmount = payableAmount;
    }

    public String getUserId() { return userId; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public BigDecimal getPayableAmount() { return payableAmount; }
}
```

- [ ] **Step 5: 创建 `OrderStatusChangedEvent.java`**

```java
package com.scmcloud.order.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.scmcloud.order.domain.entity.OrderStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * 订单状态流转事件(本地事件溯源用)。
 */
public class OrderStatusChangedEvent extends OrderEvent {

    private final OrderStatus fromStatus;
    private final OrderStatus toStatus;

    public OrderStatusChangedEvent(UUID tenantId, Long orderId, String orderNo,
                                   OrderStatus fromStatus, OrderStatus toStatus) {
        super(tenantId, orderId, orderNo, "ORDER_STATUS_CHANGED");
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
    }

    @JsonCreator
    public OrderStatusChangedEvent(@JsonProperty("eventId") UUID eventId,
                                    @JsonProperty("tenantId") UUID tenantId,
                                    @JsonProperty("orderId") Long orderId,
                                    @JsonProperty("orderNo") String orderNo,
                                    @JsonProperty("timestamp") Instant timestamp,
                                    @JsonProperty("eventType") String eventType,
                                    @JsonProperty("fromStatus") OrderStatus fromStatus,
                                    @JsonProperty("toStatus") OrderStatus toStatus) {
        super(eventId, tenantId, orderId, orderNo, timestamp, eventType);
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
    }

    public OrderStatus getFromStatus() { return fromStatus; }
    public OrderStatus getToStatus() { return toStatus; }
}
```

- [ ] **Step 6: 实体 `OrdOrderEvent.java` 加 `tenantId`、`orderNo` 字段**

在 `private UUID orderId;` 之后插入:

```java
    @TableField("order_no")
    private String orderNo;
```

在 `private UUID orderId;` 之前(`eventId` 声明之后)插入:

```java
    @TableField("tenant_id")
    private UUID tenantId;
```

- [ ] **Step 7: 编译适配 `OrderAggregate.java`(整文件替换;rehydrate/校验在 Task 2)**

```java
package com.scmcloud.order.event;

import com.scmcloud.order.domain.entity.OrderStatus;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class OrderAggregate {

    private UUID tenantId;
    private Long orderId;
    private String orderNo;
    private BigDecimal totalAmount;
    private BigDecimal payableAmount;
    private OrderStatus status;
    private List<OrderEvent> uncommittedEvents = new ArrayList<>();

    public static OrderAggregate create(UUID tenantId, Long orderId, String orderNo,
                                        BigDecimal totalAmount, BigDecimal payableAmount) {
        OrderAggregate aggregate = new OrderAggregate();
        OrderCreatedEvent event = new OrderCreatedEvent(
                tenantId, orderId, orderNo, null, totalAmount, payableAmount);
        aggregate.apply(event);
        aggregate.uncommittedEvents.add(event);
        return aggregate;
    }

    public void changeStatus(OrderStatus newStatus) {
        OrderStatusChangedEvent event =
                new OrderStatusChangedEvent(tenantId, orderId, orderNo, this.status, newStatus);
        apply(event);
        uncommittedEvents.add(event);
    }

    public void apply(OrderEvent event) {
        if (event instanceof OrderCreatedEvent e) {
            this.tenantId = e.getTenantId();
            this.orderId = e.getOrderId();
            this.orderNo = e.getOrderNo();
            this.totalAmount = e.getTotalAmount();
            this.payableAmount = e.getPayableAmount();
            this.status = OrderStatus.PENDING_PAYMENT;
        } else if (event instanceof OrderStatusChangedEvent e) {
            this.status = e.getToStatus();
        }
    }

    public List<OrderEvent> getUncommittedEvents() {
        return uncommittedEvents;
    }

    public void clearUncommittedEvents() {
        uncommittedEvents.clear();
    }
}
```

- [ ] **Step 8: `OrderEventStore.append` 补写新字段**

将 `append` 方法中 `entity.setEventId(event.getEventId());` 前后改为:

```java
            entity.setTenantId(event.getTenantId());
            entity.setEventId(event.getEventId());
            entity.setOrderId(event.getOrderId());
            entity.setOrderNo(event.getOrderNo());
```

(`getEvents(UUID)` 签名暂不改,Task 4 统一重写;本步只保证编译与字段落库。)

- [ ] **Step 9: 运行测试确认通过**

Run: `mvn test -Dtest=OrderEventJsonTest -pl :scm-order-service -f com.scm.parent/pom.xml`
Expected: `Tests run: 3, Failures: 0, Errors: 0`

- [ ] **Step 10: 全模块编译保持绿**

Run: `mvn test -pl :scm-order-service -f com.scm.parent/pom.xml`
Expected: BUILD SUCCESS(现有测试不回归)

- [ ] **Step 11: Commit**

```bash
git add scm-order/service/src/main/java/com/scmcloud/order/event/ scm-order/service/src/main/java/com/scmcloud/order/domain/entity/OrdOrderEvent.java scm-order/service/src/test/java/com/scmcloud/order/event/OrderEventJsonTest.java
git commit -m "refactor(order): align event model with real domain (Long orderId, OrderStatus enum, tenantId)"
```

---

### Task 2: OrderAggregate 增强(rehydrate + 状态机校验)

**Files:**
- Modify: `scm-order/service/src/main/java/com/scmcloud/order/event/OrderAggregate.java`
- Test: `scm-order/service/src/test/java/com/scmcloud/order/event/OrderAggregateTest.java`

- [ ] **Step 1: 写失败测试**

```java
package com.scmcloud.order.event;

import com.scmcloud.order.domain.entity.OrderStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNull;

class OrderAggregateTest {

    private final UUID tenantId = UUID.randomUUID();

    @Test
    void createShouldApplyCreatedEventAndStartPendingPayment() {
        OrderAggregate aggregate = OrderAggregate.create(
                tenantId, 1L, "NO1001", new BigDecimal("99.90"), new BigDecimal("89.90"));

        assertEquals(1L, aggregate.getOrderId());
        assertEquals("NO1001", aggregate.getOrderNo());
        assertEquals(OrderStatus.PENDING_PAYMENT, aggregate.getStatus());
        assertEquals(tenantId, aggregate.getTenantId());
        assertEquals(1, aggregate.getUncommittedEvents().size());
    }

    @Test
    void changeStatusShouldValidateTransition() {
        OrderAggregate aggregate = OrderAggregate.create(
                tenantId, 1L, "NO1001", BigDecimal.TEN, BigDecimal.TEN);

        assertThrows(IllegalStateException.class,
                () -> aggregate.changeStatus(OrderStatus.COMPLETED));
    }

    @Test
    void changeStatusShouldAppendEventOnValidTransition() {
        OrderAggregate aggregate = OrderAggregate.create(
                tenantId, 1L, "NO1001", BigDecimal.TEN, BigDecimal.TEN);

        aggregate.changeStatus(OrderStatus.PAID);

        assertEquals(OrderStatus.PAID, aggregate.getStatus());
        assertEquals(2, aggregate.getUncommittedEvents().size());
        OrderStatusChangedEvent event =
                assertInstanceOf(OrderStatusChangedEvent.class,
                        aggregate.getUncommittedEvents().get(1));
        assertEquals(OrderStatus.PENDING_PAYMENT, event.getFromStatus());
        assertEquals(OrderStatus.PAID, event.getToStatus());
    }

    @Test
    void rehydrateShouldRebuildStateFromHistory() {
        List<OrderEvent> history = List.of(
                new OrderCreatedEvent(tenantId, 1L, "NO1001", "u-1",
                        new BigDecimal("99.90"), new BigDecimal("89.90")),
                new OrderStatusChangedEvent(tenantId, 1L, "NO1001",
                        OrderStatus.PENDING_PAYMENT, OrderStatus.PAID),
                new OrderStatusChangedEvent(tenantId, 1L, "NO1001",
                        OrderStatus.PAID, OrderStatus.PENDING_SHIP));

        OrderAggregate aggregate = OrderAggregate.rehydrate(history);

        assertEquals(OrderStatus.PENDING_SHIP, aggregate.getStatus());
        assertEquals("u-1", aggregate.getUserId());
        assertEquals(new BigDecimal("99.90"), aggregate.getTotalAmount());
        assertNull(aggregate.getUncommittedEvents()); // 重放不产生未提交事件
    }

    @Test
    void rehydrateShouldRejectHistoryWithoutCreationEvent() {
        List<OrderEvent> history = List.of(new OrderStatusChangedEvent(
                tenantId, 1L, "NO1001", OrderStatus.PAID, OrderStatus.CANCELLED));

        assertThrows(IllegalStateException.class, () -> OrderAggregate.rehydrate(history));
    }
}
```

- [ ] **Step 2: 运行确认失败**

Run: `mvn test -Dtest=OrderAggregateTest -pl :scm-order-service -f com.scm.parent/pom.xml`
Expected: COMPILATION ERROR(无 `rehydrate`、`getUserId`、`getStatus` 等方法)

- [ ] **Step 3: 增强 OrderAggregate**

在 Task 1 版本基础上做以下修改:

字段区追加两个 getter 所需字段与只读访问器:

```java
    private String userId;
```

在类末尾(getter 区)添加:

```java
    public UUID getTenantId() { return tenantId; }
    public Long getOrderId() { return orderId; }
    public String getOrderNo() { return orderNo; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public BigDecimal getPayableAmount() { return payableAmount; }
    public String getUserId() { return userId; }
    public OrderStatus getStatus() { return status; }
```

`apply` 的 `OrderCreatedEvent` 分支补一行:

```java
            this.userId = e.getUserId();
```

`create` 的便捷构造传 userId 参数(签名改为带 userId):

```java
    public static OrderAggregate create(UUID tenantId, Long orderId, String orderNo, String userId,
                                        BigDecimal totalAmount, BigDecimal payableAmount) {
        OrderAggregate aggregate = new OrderAggregate();
        OrderCreatedEvent event = new OrderCreatedEvent(
                tenantId, orderId, orderNo, userId, totalAmount, payableAmount);
        aggregate.apply(event);
        aggregate.uncommittedEvents.add(event);
        return aggregate;
    }
```

同步修正 Task 1 测试中 `create(...)` 调用处(加 `"u-1"` 实参):
`OrderAggregateTest.createShouldApplyCreatedEventAndStartPendingPayment` 与
`changeStatusShouldValidateTransition`、`changeStatusShouldAppendEventOnValidTransition` 中
改为 `OrderAggregate.create(tenantId, 1L, "NO1001", "u-1", ..., ...)`。

`changeStatus` 加状态机校验(替换整个方法):

```java
    public void changeStatus(OrderStatus newStatus) {
        if (status != null && !status.canTransitionTo(newStatus)) {
            throw new IllegalStateException(
                    "Invalid order status transition: " + status + " -> " + newStatus);
        }
        OrderStatusChangedEvent event =
                new OrderStatusChangedEvent(tenantId, orderId, orderNo, this.status, newStatus);
        apply(event);
        uncommittedEvents.add(event);
    }
```

新增静态工厂(放在 `create` 之后):

```java
    /**
     * 从历史事件重放重建聚合。首事件必须是 ORDER_CREATED。
     */
    public static OrderAggregate rehydrate(List<OrderEvent> history) {
        if (history == null || history.isEmpty()
                || !(history.get(0) instanceof OrderCreatedEvent)) {
            throw new IllegalStateException(
                    "Cannot rehydrate order aggregate: history must start with ORDER_CREATED");
        }
        OrderAggregate aggregate = new OrderAggregate();
        history.forEach(event -> {
            aggregate.apply(event);
            aggregate.version++;
        });
        return aggregate;
    }

    private long version;

    public long getVersion() { return version; }
```

注意:`rehydrate` 不触碰 `uncommittedEvents`,测试断言其为 `null` —— 因此把
`uncommittedEvents` 初始化移入构造器语义:`create`/`changeStatus` 路径首次使用时惰性初始化。
将字段声明改为:

```java
    private List<OrderEvent> uncommittedEvents;
```

`create` 中 `aggregate.uncommittedEvents.add(event)` 前加:

```java
        aggregate.ensureUncommitted();
```

`changeStatus` 同样在 add 前调用 `ensureUncommitted();`,并新增私有方法:

```java
    private void ensureUncommitted() {
        if (uncommittedEvents == null) {
            uncommittedEvents = new ArrayList<>();
        }
    }
```

`clearUncommittedEvents` 保持不变(null 安全的 clear 已由调用方保证仅在 create 流程使用)。

- [ ] **Step 4: 运行确认通过**

Run: `mvn test -Dtest=OrderAggregateTest -pl :scm-order-service -f com.scm.parent/pom.xml`
Expected: `Tests run: 5, Failures: 0, Errors: 0`

- [ ] **Step 5: Commit**

```bash
git add scm-order/service/src/main/java/com/scmcloud/order/event/OrderAggregate.java scm-order/service/src/test/java/com/scmcloud/order/event/OrderAggregateTest.java
git commit -m "feat(order): add event replay and state-machine validation to OrderAggregate"
```

---

### Task 3: 新增 DDL `028_ord_order_event.sql`

**Files:**
- Create: `scripts/db/microservices/028_ord_order_event.sql`

- [ ] **Step 1: 创建脚本**

```sql
-- ======================================================================
-- 订单本地事件溯源存储 (db_order)
-- append-only;与 ord_status_history 并存;跨服务消息由 outbox 负责
-- 保留策略:retention/apply-retention.sh 已含 3 年清理
-- ======================================================================

CREATE TABLE IF NOT EXISTS ord_order_event (
    id          BIGSERIAL PRIMARY KEY,
    tenant_id   UUID         NOT NULL,
    event_id    UUID         NOT NULL,
    order_id    BIGINT       NOT NULL,
    order_no    VARCHAR(128),
    event_type  VARCHAR(64)  NOT NULL,
    event_data  TEXT         NOT NULL,
    create_time TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_ord_order_event_event_id UNIQUE (event_id)
);

CREATE INDEX IF NOT EXISTS idx_ord_order_event_order
    ON ord_order_event (order_id, create_time);

COMMENT ON TABLE ord_order_event IS '订单本地事件溯源存储(append-only,本地审计/重放)';
```

- [ ] **Step 2: 本地验证建表(可选,需本地 PostgreSQL)**

```bash
$env:PGPASSWORD='admin123'; psql -U admin -h localhost -d db_order -f scripts/db/microservices/028_ord_order_event.sql
```

Expected: `CREATE TABLE` / `CREATE INDEX` / `COMMENT`(幂等重复执行不报错)

- [ ] **Step 3: Commit**

```bash
git add scripts/db/microservices/028_ord_order_event.sql
git commit -m "feat(db): add ord_order_event table DDL for local event sourcing"
```

---

### Task 4: OrderEventStore 加固

**Files:**
- Modify: `scm-order/service/src/main/java/com/scmcloud/order/event/OrderEventStore.java`
- Test: `scm-order/service/src/test/java/com/scmcloud/order/event/OrderEventStoreTest.java`

- [ ] **Step 1: 写失败测试**

```java
package com.scmcloud.order.event;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scmcloud.common.exception.ServiceException;
import com.scmcloud.order.domain.entity.OrderStatus;
import com.scmcloud.order.domain.entity.OrdOrderEvent;
import com.scmcloud.order.mapper.OrdOrderEventMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderEventStoreTest {

    @Mock
    private OrdOrderEventMapper eventMapper;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private OrderEventStore store;

    @BeforeEach
    void setUp() {
        store = new OrderEventStore(eventMapper, objectMapper);
    }

    private OrderCreatedEvent createdEvent() {
        return new OrderCreatedEvent(UUID.randomUUID(), 1L, "NO1001", "u-1",
                new BigDecimal("99.90"), new BigDecimal("89.90"));
    }

    private OrderStatusChangedEvent statusEvent() {
        return new OrderStatusChangedEvent(UUID.randomUUID(), 1L, "NO1001",
                OrderStatus.PENDING_PAYMENT, OrderStatus.PAID);
    }

    private OrdOrderEvent entity(OrderEvent event) throws JsonProcessingException {
        OrdOrderEvent entity = new OrdOrderEvent();
        entity.setTenantId(event.getTenantId());
        entity.setEventId(event.getEventId());
        entity.setOrderId(event.getOrderId());
        entity.setOrderNo(event.getOrderNo());
        entity.setEventType(event.getEventType());
        entity.setEventData(objectMapper.writeValueAsString(event));
        return entity;
    }

    @Test
    void appendShouldPersistSerializedEventWithMetadata() {
        OrderEvent event = createdEvent();

        store.append(event);

        ArgumentCaptor<OrdOrderEvent> captor = ArgumentCaptor.forClass(OrdOrderEvent.class);
        verify(eventMapper).insert(captor.capture());
        OrdOrderEvent saved = captor.getValue();
        assertEquals(event.getEventId(), saved.getEventId());
        assertEquals(event.getTenantId(), saved.getTenantId());
        assertEquals(1L, saved.getOrderId());
        assertEquals("NO1001", saved.getOrderNo());
        assertEquals("ORDER_CREATED", saved.getEventType());
        assertTrue(saved.getEventData().contains("\"eventType\":\"ORDER_CREATED\""));
    }

    @Test
    void appendShouldIgnoreDuplicateEventId() {
        when(eventMapper.insert(any())).thenThrow(new DuplicateKeyException("duplicate"));

        assertDoesNotThrow(() -> store.append(createdEvent()));
    }

    @Test
    void appendShouldWrapSerializationFailure() throws JsonProcessingException {
        ObjectMapper broken = org.mockito.Mockito.mock(ObjectMapper.class);
        when(broken.writeValueAsString(any())).thenThrow(new JsonProcessingException("boom") {});
        OrderEventStore brokenStore = new OrderEventStore(eventMapper, broken);

        assertThrows(ServiceException.class, () -> brokenStore.append(createdEvent()));
    }

    @Test
    void getEventsShouldDeserializeInInsertionOrder() throws JsonProcessingException {
        when(eventMapper.selectList(any()))
                .thenReturn(List.of(entity(createdEvent()), entity(statusEvent())));

        List<OrderEvent> events = store.getEvents(1L);

        assertEquals(2, events.size());
        assertInstanceOf(OrderCreatedEvent.class, events.get(0));
        assertInstanceOf(OrderStatusChangedEvent.class, events.get(1));
    }

    @Test
    void getEventsShouldWrapDeserializationFailure() {
        OrdOrderEvent bad = new OrdOrderEvent();
        bad.setEventId(UUID.randomUUID());
        bad.setEventData("not-json");
        when(eventMapper.selectList(any())).thenReturn(List.of(bad));

        assertThrows(ServiceException.class, () -> store.getEvents(1L));
    }

    @Test
    @SuppressWarnings("unchecked")
    void pagedGetEventsShouldUsePageQuery() throws JsonProcessingException {
        Page<OrdOrderEvent> page = new Page<>(2, 10);
        page.setRecords(List.of(entity(createdEvent())));
        when(eventMapper.selectPage(any(), any())).thenReturn(page);

        List<OrderEvent> events = store.getEvents(1L, 2, 10);

        assertEquals(1, events.size());
        verify(eventMapper).selectPage(
                argThat(p -> p instanceof Page<?> pg && pg.getCurrent() == 2 && pg.getSize() == 10),
                any());
    }
}
```

注:`argThat` 需要静态导入 `org.mockito.ArgumentMatchers.argThat`,补进 import 区。

- [ ] **Step 2: 运行确认失败**

Run: `mvn test -Dtest=OrderEventStoreTest -pl :scm-order-service -f com.scm.parent/pom.xml`
Expected: COMPILATION ERROR(`getEvents(Long)` 不存在、无 `getEvents(Long, int, int)` 分页重载)

- [ ] **Step 3: 重写 OrderEventStore(整文件替换)**

```java
package com.scmcloud.order.event;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scmcloud.common.exception.ServiceException;
import com.scmcloud.order.domain.entity.OrdOrderEvent;
import com.scmcloud.order.mapper.OrdOrderEventMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

import java.util.List;

@Slf4j
@Repository
public class OrderEventStore {

    private final OrdOrderEventMapper eventMapper;
    private final ObjectMapper objectMapper;

    public OrderEventStore(OrdOrderEventMapper eventMapper, ObjectMapper objectMapper) {
        this.eventMapper = eventMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * 追加事件。event_id 唯一约束保证幂等:重复事件记 warn 跳过。
     */
    public void append(OrderEvent event) {
        try {
            OrdOrderEvent entity = new OrdOrderEvent();
            entity.setTenantId(event.getTenantId());
            entity.setEventId(event.getEventId());
            entity.setOrderId(event.getOrderId());
            entity.setOrderNo(event.getOrderNo());
            entity.setEventType(event.getEventType());
            entity.setEventData(objectMapper.writeValueAsString(event));
            eventMapper.insert(entity);
        } catch (DuplicateKeyException e) {
            log.warn("Duplicate order event ignored: eventId={}, orderId={}",
                    event.getEventId(), event.getOrderId());
        } catch (JsonProcessingException e) {
            throw new ServiceException("Failed to serialize order event: " + event.getEventId(), e);
        }
    }

    /**
     * 按写入顺序取全部事件(create_time ASC, id ASC 兜底保证稳定排序)。
     */
    public List<OrderEvent> getEvents(Long orderId) {
        LambdaQueryWrapper<OrdOrderEvent> wrapper = Wrappers.lambdaQuery(OrdOrderEvent.class)
                .eq(OrdOrderEvent::getOrderId, orderId)
                .orderByAsc(OrdOrderEvent::getCreateTime)
                .orderByAsc(OrdOrderEvent::getId);
        return eventMapper.selectList(wrapper).stream()
                .map(this::deserialize)
                .toList();
    }

    /**
     * 分页取事件(pageNo 从 1 开始),替代原 .last() 字符串拼接。
     */
    public List<OrderEvent> getEvents(Long orderId, int pageNo, int pageSize) {
        Page<OrdOrderEvent> result = eventMapper.selectPage(
                new Page<>(pageNo, pageSize),
                Wrappers.<OrdOrderEvent>lambdaQuery()
                        .eq(OrdOrderEvent::getOrderId, orderId)
                        .orderByAsc(OrdOrderEvent::getCreateTime)
                        .orderByAsc(OrdOrderEvent::getId));
        return result.getRecords().stream()
                .map(this::deserialize)
                .toList();
    }

    private OrderEvent deserialize(OrdOrderEvent entity) {
        try {
            return objectMapper.readValue(entity.getEventData(), OrderEvent.class);
        } catch (JsonProcessingException e) {
            throw new ServiceException(
                    "Failed to deserialize order event: " + entity.getEventId(), e);
        }
    }
}
```

- [ ] **Step 4: 运行确认通过**

Run: `mvn test -Dtest=OrderEventStoreTest -pl :scm-order-service -f com.scm.parent/pom.xml`
Expected: `Tests run: 6, Failures: 0, Errors: 0`

- [ ] **Step 5: Commit**

```bash
git add scm-order/service/src/main/java/com/scmcloud/order/event/OrderEventStore.java scm-order/service/src/test/java/com/scmcloud/order/event/OrderEventStoreTest.java
git commit -m "fix(order): harden OrderEventStore (Page pagination, stable ordering, ServiceException, idempotent append)"
```

---

### Task 5: 接线 OrdOrderCommandService(3 处)

**Files:**
- Modify: `scm-order/service/src/main/java/com/scmcloud/order/service/command/OrdOrderCommandService.java`
- Test: `scm-order/service/src/test/java/com/scmcloud/order/service/command/OrdOrderCommandServiceEventTest.java`

- [ ] **Step 1: 写失败测试**

```java
package com.scmcloud.order.service.command;

import com.scmcloud.common.domain.Money;
import com.scmcloud.common.domain.TenantId;
import com.scmcloud.order.domain.entity.OrdOrder;
import com.scmcloud.order.domain.entity.OrderStatus;
import com.scmcloud.order.event.OrderCreatedEvent;
import com.scmcloud.order.event.OrderEvent;
import com.scmcloud.order.event.OrderEventStore;
import com.scmcloud.order.event.OrderStatusChangedEvent;
import com.scmcloud.order.mapper.OrdOrderMapper;
import com.scmcloud.system.api.StatusMachineDubboService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrdOrderCommandServiceEventTest {

    @Mock private OrdOrderMapper ordOrderMapper;
    @Mock private OrdOrderItemCommandService ordOrderItemCommandService;
    @Mock private OrdStatusHistoryCommandService ordStatusHistoryCommandService;
    @Mock private com.scmcloud.order.domain.repository.OrdOrderRepository ordOrderRepository;
    @Mock private OrderEventStore eventStore;
    @Mock private StatusMachineDubboService statusMachine;

    private OrdOrderCommandService service;

    @BeforeEach
    void setUp() {
        service = new OrdOrderCommandService(ordOrderMapper, ordOrderItemCommandService,
                ordStatusHistoryCommandService, ordOrderRepository, eventStore);
        ReflectionTestUtils.setField(service, "statusMachine", statusMachine);
    }

    private OrdOrder order(int statusCode) {
        OrdOrder order = new OrdOrder();
        order.setId(1L);
        order.setOrderNo("NO1001");
        order.setStatus(statusCode);
        order.setUserId("u-1");
        order.setTenantId(TenantId.generate());
        order.setTotalAmount(Money.of(new BigDecimal("99.90")));
        order.setPayableAmount(Money.of(new BigDecimal("89.90")));
        return order;
    }

    @Test
    void createOrderShouldAppendOrderCreatedEvent() {
        OrdOrder order = order(OrderStatus.PENDING_PAYMENT.getCode());
        when(ordOrderMapper.insert(any(OrdOrder.class))).thenReturn(1);

        service.createOrder(order, List.of());

        ArgumentCaptor<OrderEvent> captor = ArgumentCaptor.forClass(OrderEvent.class);
        verify(eventStore).append(captor.capture());
        OrderCreatedEvent event = assertInstanceOf(OrderCreatedEvent.class, captor.getValue());
        assertEquals(1L, event.getOrderId());
        assertEquals("NO1001", event.getOrderNo());
        assertEquals(order.getTenantId().toUUID(), event.getTenantId());
        assertEquals(new BigDecimal("99.90"), event.getTotalAmount());
        assertEquals(new BigDecimal("89.90"), event.getPayableAmount());
    }

    @Test
    void updateOrderStatusShouldAppendStatusChangedEvent() {
        OrdOrder existing = order(OrderStatus.PAID.getCode());
        when(ordOrderMapper.selectById(1L)).thenReturn(existing);
        when(statusMachine.canTransition("ORDER", "PAID", "PENDING_SHIP"))
                .thenReturn(new StatusMachineDubboService.TransitionCheckDTO(
                        true, "ORDER", "PAID", "PENDING_SHIP", null));
        when(ordOrderMapper.updateById(any(OrdOrder.class))).thenReturn(1);

        service.updateOrderStatus(1L, OrderStatus.PENDING_SHIP.getCode());

        ArgumentCaptor<OrderEvent> captor = ArgumentCaptor.forClass(OrderEvent.class);
        verify(eventStore).append(captor.capture());
        OrderStatusChangedEvent event =
                assertInstanceOf(OrderStatusChangedEvent.class, captor.getValue());
        assertEquals(OrderStatus.PAID, event.getFromStatus());
        assertEquals(OrderStatus.PENDING_SHIP, event.getToStatus());
        assertEquals(existing.getTenantId().toUUID(), event.getTenantId());
    }

    @Test
    void cancelTimeoutOrderShouldAppendCancelledEvent() {
        OrdOrder existing = order(OrderStatus.PAID.getCode());
        when(ordOrderMapper.selectById(1L)).thenReturn(existing);

        service.cancelTimeoutOrder(existing);

        ArgumentCaptor<OrderEvent> captor = ArgumentCaptor.forClass(OrderEvent.class);
        verify(eventStore).append(captor.capture());
        OrderStatusChangedEvent event =
                assertInstanceOf(OrderStatusChangedEvent.class, captor.getValue());
        assertEquals(OrderStatus.CANCELLED, event.getToStatus());
        verify(ordOrderRepository).save(existing);
    }
}
```

注:`createOrder` 内 `items` 为空会抛 IllegalArgumentException,故传 `List.of()` 前需
构造至少一个明细 —— 将该测试中调用改为:

```java
        com.scmcloud.order.domain.entity.OrdOrderItem item = new com.scmcloud.order.domain.entity.OrdOrderItem();
        item.setSubtotal(Money.of(new BigDecimal("99.90")));
        service.createOrder(order, List.of(item));
```

- [ ] **Step 2: 运行确认失败**

Run: `mvn test -Dtest=OrdOrderCommandServiceEventTest -pl :scm-order-service -f com.scm.parent/pom.xml`
Expected: COMPILATION ERROR(构造器缺 `eventStore` 参数)

- [ ] **Step 3: 接线实现**

`OrdOrderCommandService.java` 三处修改:

(a) import 区追加:

```java
import com.scmcloud.order.event.OrderCreatedEvent;
import com.scmcloud.order.event.OrderEventStore;
import com.scmcloud.order.event.OrderStatusChangedEvent;
```

(b) 字段区追加(`@RequiredArgsConstructor` 自动进构造器):

```java
    private final OrderEventStore eventStore;
```

(c) `createOrder`:在 `ordStatusHistoryCommandService.save(history);`(约 L83)之后追加:

```java
        eventStore.append(new OrderCreatedEvent(
                order.getTenantId() != null ? order.getTenantId().toUUID() : null,
                order.getId(),
                order.getOrderNo(),
                order.getUserId(),
                order.getTotalAmount() != null ? order.getTotalAmount().getAmount() : null,
                order.getPayableAmount() != null ? order.getPayableAmount().getAmount() : null));
```

(d) `updateOrderStatus`:在 `if (updated > 0)` 块内 history save 之后追加:

```java
            eventStore.append(new OrderStatusChangedEvent(
                    order.getTenantId() != null ? order.getTenantId().toUUID() : null,
                    order.getId(),
                    order.getOrderNo(),
                    OrderStatus.fromCode(fromStatus),
                    targetStatus));
```

(e) `cancelTimeoutOrder`:在 `existing.cancel(...)` 之前捕获原状态,`ordOrderRepository.save(existing);` 之后追加:

```java
        OrderStatus previousStatus = existing.getStatusEnum();
        existing.cancel("订单超时未支付，系统自动取消");

        ordOrderRepository.save(existing);

        eventStore.append(new OrderStatusChangedEvent(
                existing.getTenantId() != null ? existing.getTenantId().toUUID() : null,
                existing.getId(),
                existing.getOrderNo(),
                previousStatus,
                OrderStatus.CANCELLED));
```

- [ ] **Step 4: 运行确认通过**

Run: `mvn test -Dtest=OrdOrderCommandServiceEventTest -pl :scm-order-service -f com.scm.parent/pom.xml`
Expected: `Tests run: 3, Failures: 0, Errors: 0`

- [ ] **Step 5: Commit**

```bash
git add scm-order/service/src/main/java/com/scmcloud/order/service/command/OrdOrderCommandService.java scm-order/service/src/test/java/com/scmcloud/order/service/command/OrdOrderCommandServiceEventTest.java
git commit -m "feat(order): append local sourcing events in command service (create/status/cancel)"
```

---

### Task 6: 接线 OrdOrderServiceImpl(2 处)

**Files:**
- Modify: `scm-order/service/src/main/java/com/scmcloud/order/service/impl/OrdOrderServiceImpl.java`
- Test: `scm-order/service/src/test/java/com/scmcloud/order/service/impl/OrdOrderServiceImplEventTest.java`

- [ ] **Step 1: 写失败测试**

```java
package com.scmcloud.order.service.impl;

import com.scmcloud.common.domain.Money;
import com.scmcloud.common.domain.TenantId;
import com.scmcloud.order.domain.entity.OrdOrder;
import com.scmcloud.order.domain.entity.OrderStatus;
import com.scmcloud.order.event.OrderCreatedEvent;
import com.scmcloud.order.event.OrderEvent;
import com.scmcloud.order.event.OrderEventStore;
import com.scmcloud.order.event.OrderStatusChangedEvent;
import com.scmcloud.order.mapper.OrdOrderMapper;
import com.scmcloud.order.service.IOrdOrderItemService;
import com.scmcloud.order.service.IOrdStatusHistoryService;
import com.scmcloud.system.api.StatusMachineDubboService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrdOrderServiceImplEventTest {

    @Mock private IOrdOrderItemService orderItemService;
    @Mock private IOrdStatusHistoryService statusHistoryService;
    @Mock private OrderEventStore eventStore;
    @Mock private StatusMachineDubboService statusMachine;
    @Mock private OrdOrderMapper ordOrderMapper;

    private OrdOrderServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new OrdOrderServiceImpl(orderItemService, statusHistoryService, eventStore);
        ReflectionTestUtils.setField(service, "baseMapper", ordOrderMapper);
        ReflectionTestUtils.setField(service, "statusMachine", statusMachine);
    }

    private OrdOrder order(int statusCode) {
        OrdOrder order = new OrdOrder();
        order.setId(1L);
        order.setOrderNo("NO1001");
        order.setStatus(statusCode);
        order.setUserId("u-1");
        order.setTenantId(TenantId.generate());
        order.setTotalAmount(Money.of(new BigDecimal("99.90")));
        order.setPayableAmount(Money.of(new BigDecimal("89.90")));
        return order;
    }

    @Test
    void createOrderShouldAppendOrderCreatedEvent() {
        OrdOrder order = order(OrderStatus.PENDING_PAYMENT.getCode());
        when(ordOrderMapper.insert(any(OrdOrder.class))).thenReturn(1);

        service.createOrder(order, List.of());

        ArgumentCaptor<OrderEvent> captor = ArgumentCaptor.forClass(OrderEvent.class);
        verify(eventStore).append(captor.capture());
        OrderCreatedEvent event = assertInstanceOf(OrderCreatedEvent.class, captor.getValue());
        assertEquals("NO1001", event.getOrderNo());
        assertEquals(order.getTenantId().toUUID(), event.getTenantId());
    }

    @Test
    void updateOrderStatusShouldAppendStatusChangedEvent() {
        OrdOrder existing = order(OrderStatus.PAID.getCode());
        when(ordOrderMapper.selectById(1L)).thenReturn(existing);
        when(statusMachine.canTransition("ORDER", "PAID", "PENDING_SHIP"))
                .thenReturn(new StatusMachineDubboService.TransitionCheckDTO(
                        true, "ORDER", "PAID", "PENDING_SHIP", null));
        when(ordOrderMapper.updateById(any(OrdOrder.class))).thenReturn(1);

        service.updateOrderStatus(1L, OrderStatus.PENDING_SHIP.getCode());

        ArgumentCaptor<OrderEvent> captor = ArgumentCaptor.forClass(OrderEvent.class);
        verify(eventStore).append(captor.capture());
        OrderStatusChangedEvent event =
                assertInstanceOf(OrderStatusChangedEvent.class, captor.getValue());
        assertEquals(OrderStatus.PAID, event.getFromStatus());
        assertEquals(OrderStatus.PENDING_SHIP, event.getToStatus());
    }
}
```

- [ ] **Step 2: 运行确认失败**

Run: `mvn test -Dtest=OrdOrderServiceImplEventTest -pl :scm-order-service -f com.scm.parent/pom.xml`
Expected: COMPILATION ERROR(构造器缺第三参)

- [ ] **Step 3: 接线实现**

(a) import 区追加:

```java
import com.scmcloud.order.event.OrderCreatedEvent;
import com.scmcloud.order.event.OrderEventStore;
import com.scmcloud.order.event.OrderStatusChangedEvent;
```

(b) 字段区追加:

```java
    private final OrderEventStore eventStore;
```

(c) `createOrder`:在末尾 `log.info("订单创建成功...")` 之前追加:

```java
        eventStore.append(new OrderCreatedEvent(
                order.getTenantId() != null ? order.getTenantId().toUUID() : null,
                order.getId(),
                order.getOrderNo(),
                order.getUserId(),
                order.getTotalAmount() != null ? order.getTotalAmount().getAmount() : null,
                order.getPayableAmount() != null ? order.getPayableAmount().getAmount() : null));
```

(d) `updateOrderStatus`:在 `if (updated)` 块内 history save 之后追加:

```java
            eventStore.append(new OrderStatusChangedEvent(
                    order.getTenantId() != null ? order.getTenantId().toUUID() : null,
                    order.getId(),
                    order.getOrderNo(),
                    OrderStatus.fromCode(fromStatus),
                    targetStatus));
```

- [ ] **Step 4: 运行确认通过**

Run: `mvn test -Dtest=OrdOrderServiceImplEventTest -pl :scm-order-service -f com.scm.parent/pom.xml`
Expected: `Tests run: 2, Failures: 0, Errors: 0`

- [ ] **Step 5: Commit**

```bash
git add scm-order/service/src/main/java/com/scmcloud/order/service/impl/OrdOrderServiceImpl.java scm-order/service/src/test/java/com/scmcloud/order/service/impl/OrdOrderServiceImplEventTest.java
git commit -m "feat(order): append local sourcing events in OrdOrderServiceImpl (create/status)"
```

---

### Task 7: 全量验证

**Files:** 无新改动(验证 + 修复回归)

- [ ] **Step 1: 模块全量测试**

Run: `mvn test -pl :scm-order-service -f com.scm.parent/pom.xml`
Expected: BUILD SUCCESS,无测试失败(现有集成/单元测试不回归)

- [ ] **Step 2: 全仓快速构建**

Run: `mvn clean install -DskipTests -f com.scm.parent/pom.xml`
Expected: BUILD SUCCESS

- [ ] **Step 3: 覆盖率门槛(可选但推荐)**

Run: `mvn verify -Djacoco.skip=false -f com.scm.parent/pom.xml`
Expected: jacoco check 通过(70% 行 / 60% 分支);若 order 模块未达标,优先补 OrderAggregate/OrderEventStore 边界分支测试

- [ ] **Step 4: 遗留文件提交(如有)**

```bash
git status --porcelain
git add <遗留的修改文件>
git commit -m "chore(order): event sourcing verification fixes"
```

---

## 自查记录(writing-plans Self-Review)

1. **Spec coverage**:模型重构(Task 1)、聚合 rehydrate+校验(Task 2)、DDL 含 tenant_id/event_id 唯一(Task 3)、Store 四项修复(Task 4)、主流程接线含 command service 其余 transitionTo 入口排查结果——`cancelTimeoutOrder`(Task 5)与 impl 两处(Task 6)、错误处理(Task 4 ServiceException/幂等)、测试策略(各任务 TDD)——全覆盖。
2. **Placeholder scan**:无 TBD/TODO;所有代码步骤给出完整代码;Task 5 Step 1 的 items 构造补充已内联说明。
3. **Type consistency**:`OrderCreatedEvent(UUID tenantId, Long orderId, String orderNo, String userId, BigDecimal totalAmount, BigDecimal payableAmount)` 在 Task 1 定义、Task 5/6 使用一致;`getEvents(Long)` / `getEvents(Long, int, int)` Task 4 定义、无其他调用点;`TenantId.toUUID()`、`Money.getAmount()` 已核实存在;`TransitionCheckDTO(boolean, String, String, String, String)` 与 api 定义一致。


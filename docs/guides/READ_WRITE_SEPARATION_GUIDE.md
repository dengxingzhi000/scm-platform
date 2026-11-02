# 读写分离（主从）使用指南

适用模块：`scm-common/data-rw`、`scm-common/data-rw-stub`，以及使用
`@Master` / `@Slave` 的业务服务（目前 `scm-order` 标注最多）。

## 1. 实现构成

| 组件 | 作用 |
|------|------|
| `@Master` / `@Slave` | 方法级路由声明，`@Slave(value="slave1")` 可指定从库 |
| `ReadWriteRoutingAspect` | AOP 拦截，向 `ReadWriteRoutingContext` 压栈 |
| `SqlRoutingInterceptor` | MyBatis 拦截器，AUTO 模式下按 SQL 类型兜底：SELECT → 从库，INSERT/UPDATE/DELETE → 主库并标记写 |
| `ReadWriteRoutingDataSource` | `AbstractRoutingDataSource`，按上下文选择 master / slave，带从库重试与回退 |
| `SlaveHealthChecker` | 定时探活 + PostgreSQL/MySQL 复制延迟检查 |
| `SlaveCircuitBreaker` | 从库不可用时短路到主库（需 Sentinel on classpath） |
| `ReadWriteEndpoint` | `/actuator/readwrite`，查看各组主从状态与延迟 |
| `ConnectionPoolMetrics` | `datasource.rw.pool.*` 连接池指标 |

`ReadWriteAutoConfiguration` 的注册顺序在 `DynamicDataSourceAutoConfiguration`
之前，并向 dynamic-datasource 暴露每个 group 作为一个数据源。

## 2. 当前状态：默认关闭

自动配置整体挂在：

```yaml
spring:
  datasource:
    rw:
      enabled: true
```

**仓库里没有任何服务设置这个属性**，因此：

- `ReadWriteRoutingAspect` 不会注册，`@Master` / `@Slave` 只是注释性注解；
- 即使显式打开，`spring.datasource.rw.groups` 为空时构造期会抛
  `No datasource group configured`；
- `docker-compose.yml` 只有单机 PostgreSQL，没有流复制备库。

关闭状态下所有读写都落在 `spring.datasource.url` 指向的单库，行为与普通
Spring Boot 应用一致。

## 3. 启用（以 scm-order 为例）

`scm-order/service/src/main/resources/application-readwrite.yml` 是配置模板，
默认不加载。启用：

```bash
SPRING_PROFILES_ACTIVE=readwrite \
DB_SLAVE_HOST=10.0.0.21 \
DB_SLAVE_PORT=5432 \
java -jar scm-order-service.jar
```

要点：

1. `spring.datasource.dynamic.primary=order` 把路由数据源设为默认库，
   否则注解栈压得再对也没有消费者。
2. `DB_SLAVE_HOST` 未设置时默认等于 `DB_HOST`。这会在没有备库时也走完整路由
   链路（两套连接池、健康检查、指标），便于验证接线；接入真实备库只需补上
   这两个环境变量。
3. 从库账号需要只读权限。写操作若误路由到从库会直接报错，属于预期保护。

## 4. 语义规则

- **命令方法必须同时标 `@Master` 和 `@Transactional`**。`@Master` 保证整段
  业务逻辑（含其中的读）都在主库；事务保证失败回滚。
- **查询方法标 `@Slave`**。方法体内所有 SQL 都会走从库，包括意外的写操作——
  所以查询服务不得包含写逻辑。
- 未标注的方法处于 AUTO：由 `SqlRoutingInterceptor` 按 SQL 类型路由；无法解析
  的 SQL（如部分动态拼接）会退回默认数据源（主库）。
- `@Transactional(readOnly = true)` 在 AUTO 状态下会被识别为从库路由。
- **写后读**：`@Master` 方法成功返回后会调用
  `ReadWriteRoutingContext.markWrite()`，同一线程在
  `read-master-after-write`（默认 2s）窗口内的后续读仍然走主库。
  该标记是 **ThreadLocal 且无请求级清理**（`ReadWriteRoutingContext.clear()`
  在生产代码中没有调用点），窗口按时间自然过期，不会永久粘住主库。

## 5. 写代码时的硬性约束

**不要"写完立刻用查询服务回查"再返回给调用方。** 例如：

```java
// ✗ 主从生效后，这里可能读到复制延迟的旧状态
commandService.updateOrderStatus(id, status);
OrdOrder order = queryService.getById(id);   // @Slave
```

正确做法是让命令方法返回写后实体：

```java
// ✓ scm-order 现在的写法
OrdOrder updated = commandService.updateOrderStatus(id, status);
return ApiResponse.success(OrderConverter.toResponse(updated));
```

`OrdOrderController` 的 `PUT /{id}/status` 与 `PUT /{id}/cancel` 已按此调整。

其他约束：

- 跨库/跨服务的一致性依赖 Seata，不依赖主从延迟；
- 库存扣减等热路径走 Redis + Lua，不经过该路由层；
- 不要求强一致的后台报表、列表页才适合 `@Slave`。

## 6. 观测

```bash
curl -H "X-Tenant-Id: <uuid>" http://localhost:8203/actuator/readwrite
```

关键指标：

| 指标 | 含义 |
|------|------|
| `datasource.rw.route{target="master"}` | 路由到主库次数 |
| `datasource.rw.route{target="slave"}` | 路由到从库次数 |
| `datasource.rw.fallback` | 从库不可用回退次数 |
| `datasource.rw.retry` | 从库重试次数 |
| `datasource.rw.replication.lag` | 复制延迟（ms） |
| `datasource.rw.pool.*` | 各连接池状态 |

`datasource.rw.route{target="slave"}` 长期为 0 说明注解没有生效或流量全是写。

## 7. 上线检查清单

- [ ] 备库流复制已建立，`pg_stat_replication` 有记录
- [ ] 从库账号具备只读权限，且能连上 `db_order`
- [ ] `SPRING_PROFILES_ACTIVE` 含 `readwrite`
- [ ] `/actuator/readwrite` 中 group `order` 的 `slave1` 为 available
- [ ] `datasource.rw.replication.lag` 稳定低于 `replication-lag-tolerance`
- [ ] 订单状态变更接口返回的是写后状态（不是回查结果）
- [ ] `datasource.rw.route{target="slave"}` 有流量

## 8. 相关文档

- `docs/design/CQRS_ARCHITECTURE.md` §4.1 —— 设计与路由链路
- `docs/development/CQRS_PERFORMANCE_MONITORING.md` —— 监控与排障
- `docs/development/prometheus-cqrs-alerts.yml` —— 读写分离告警规则

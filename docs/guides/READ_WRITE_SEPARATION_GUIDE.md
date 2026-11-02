# 读写分离（主从）使用指南

适用模块：`scm-common/data-rw`（实现）、`scm-common/data`（请求作用域过滤器）、
使用 `@Master` / `@Slave` 的业务服务（`scm-product` 已启用；`scm-order` 保留了
未激活的 `application-readwrite.yml` 模板）。

## 1. 实现构成

| 组件 | 位置 | 作用 |
|------|------|------|
| `@Master` / `@Slave` | `data-rw` `annotation` | 方法级/类级路由声明；`@Slave(value="slave1", fallbackToMaster=false)` |
| `ReadWriteRoutingAspect` | `data-rw` `aop` | `HIGHEST_PRECEDENCE`，在 `@Transactional` 之前压栈；识别 `readOnly` 事务 |
| `ReadWriteRoutingContext` | `data-rw` `routing` | 栈式 ThreadLocal（每层带从库名与回退开关）+ 快照/传播 |
| `ReadWriteRoutingDataSource` | `data-rw` `routing` | `AbstractRoutingDataSource`，首次取连接时决定主/从 |
| `SqlRoutingInterceptor` | `data-rw` `sql` | MyBatis 兜底：SELECT→从库（仅请求线程）、写→主库、`FOR UPDATE`/`/* MASTER */` 强制主库 |
| `ReadWriteContextFilter` | `data` `rw/web` | 标记请求线程，并在请求结束时 `clear()`，避免线程池复用泄漏上下文 |
| `ReadWriteTaskDecorator` | `data-rw` `async` | 把路由上下文传播到 `applicationTaskExecutor` / 默认 `@Async` 线程 |
| `SlaveHealthChecker` | `data-rw` `health` | 定时探活 + 复制延迟检查，失败达阈值标记从库不可用；首次调度同时承担启动预热 |
| `SlaveCircuitBreaker` | `data-rw` `circuitbreaker` | Sentinel 熔断，包住从库连接的**整个持有期**（取连接→关闭），打开后直接走主库 |
| `ReadWriteTracingInterceptor` | `data-rw` `tracing` | 在 `@Master/@Slave/@Transactional` 方法的 span 与 DEBUG 日志里标注实际目标与是否降级 |
| `SlaveWarmupManager` | `data-rw` `loadbalance` | 应用就绪后异步预热从库连接并记录耗时；同时负责加权负载均衡的权重爬升 |
| `CountingDataSource` | `data-rw` `dynamic` | 统计每个从库当前被调用方持有的连接数，供 `LEAST_CONNECTIONS` |
| `ReadWriteDatasourceConflictChecker` | `data-rw` `config` | 启动期检测 `@DS` / 额外 dynamic datasource 与 rw 的冲突，命中直接启动失败 |
| `ReadWriteHealthIndicator` / `ReadWriteEndpoint` | `data-rw` `health` / `endpoint` | `/actuator/health` 降级详情、`/actuator/readwrite` 状态、计数、熔断与预热结果 |
| `ConnectionPoolMetrics` | `data-rw` `metrics` | `datasource.rw.pool.*` 各连接池指标 |

主从各自是独立的 Hikari 连接池（`<group>-master`、`<group>-<slaveName>`），池名也是
指标与日志的区分依据。

## 2. 启用现状

- `scm-product` 已在 `scm-product/service/src/main/resources/application.yml` 启用。
- 其它服务保持 `spring.datasource.rw.enabled=false`（默认值），行为与单库完全一致。
- `spring.datasource.rw.enabled=true` 但 `groups` 为空时，自动配置只打一条 WARN，
  不注册任何路由 Bean，服务继续使用原来的 `spring.datasource` 单库。

## 3. 配置示例（scm-product）

```yaml
spring:
  datasource:
    dynamic:
      enabled: false        # 必须：让 rw 直接提供 DataSource，避免 baomidou 的 primary 冲突

    rw:
      enabled: true
      default-target: MASTER          # AUTO 的兜底目标，必须是主库
      read-master-after-write: 2s     # 同线程写后读窗口
      replication-lag-tolerance: 1s
      load-balance: ROUND_ROBIN
      health-check-enabled: true
      health-check-interval: 30s
      slave-retry-count: 3
      fail-fast-on-slave-connection-failure: true
      slave-down-affects-health: false
      # Sentinel 熔断：包住从库连接的整个持有期（连接关闭时结束统计）
      circuit-breaker:
        enabled: true
        slow-rt-ms: 500              # 单次从库连接持有超过 500ms 记为慢请求
        slow-ratio-threshold: 0.5    # 慢请求占比 > 50% 触发熔断
        error-ratio: 0.5             # SQL 异常占比 > 50% 触发熔断
        time-window-seconds: 30      # 熔断打开后的观察窗口
        min-request-amount: 10       # 统计窗口内最少请求数
        stat-interval-ms: 10000      # 统计滚动窗口
      groups:
        product:
          master:
            url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/db_product?stringtype=unspecified
            username: ${DB_USERNAME:admin}
            password: ${DB_PASSWORD:changeme}
            driver-class-name: org.postgresql.Driver
          slaves:
            - name: slave1
              url: jdbc:postgresql://${DB_SLAVE_HOST:${DB_HOST:localhost}}:${DB_SLAVE_PORT:${DB_PORT:5432}}/db_product?stringtype=unspecified
              username: ${DB_SLAVE_USERNAME:${DB_USERNAME:admin}}
              password: ${DB_SLAVE_PASSWORD:${DB_PASSWORD:changeme}}
              weight: 1
```

要点：

1. **`spring.datasource.dynamic.enabled=false`**。不关掉时，baomidou 会再包一层
   `DynamicRoutingDataSource`，并要求 `spring.datasource.dynamic.primary` 指向 rw 的
   group 名；两套路由叠加后 `@DS` 与读写路由都会变得不可预测。
2. `spring.datasource.url` 在 rw 开启时不参与路由，只是 `rw.enabled=false` 时的回退配置。
3. 从库 URL 默认指向 `DB_HOST`，仅为在没有真实备库时验证链路；**生产必须显式设置
   `DB_SLAVE_*`，并给从库账号只读权限**。写误路由到从库会直接报错，这是预期保护。
4. 从库 `connection-timeout` 建议 5s 左右：路由层会用 `Connection.isValid()` 探测，
   池的连接超时会直接挂在请求线程上。

## 4. 路由语义

决策顺序（先命中先返回）：

| 顺序 | 条件 | 目标 |
|------|------|------|
| 1 | `forceMaster()` / 写后读窗口（`read-master-after-write`）内 | 主库 |
| 2 | 当前作用域 `@Master` / 非 readOnly `@Transactional` | 主库 |
| 3 | 当前作用域 `@Slave` / `@Transactional(readOnly=true)` | 从库（不可用则按 `fallbackToMaster` 处理） |
| 4 | `AUTO` + 请求线程 + MyBatis SELECT | 从库（由 `SqlRoutingInterceptor` 压栈） |
| 5 | `AUTO` + 非请求线程（异步、定时任务、启动期 Flyway） | 主库（`default-target`） |

补充规则：

- **类级注解会被方法级覆盖**：方法上直接命中注解；方法没有注解时才回退到类注解
  （JDK 代理再回退到接口声明）。
- **`this` 自调用不生效**：AOP 代理只在跨 Bean 调用时拦截，同类内部 `this.foo()`
  不经过代理，注解不会压栈。
- **事务内不能切换数据源**：事务开始时从路由数据源取一次连接并绑定到
  `TransactionSynchronizationManager`，之后所有语句复用该连接。事务里嵌套 `@Slave`
  或 `@Master` 都不会换库，只会打一条 WARN。路由决策必须放在**最外层事务方法**上。
- 非 readOnly 事务若嵌套在 `@Slave` 作用域内，切面会强制压入 MASTER 并告警——写事务
  绝不能落在只读副本上（若外层事务已绑定连接，则只能告警）。
- **异步线程**：`ReadWriteTaskDecorator` 会传播显式注解与写标记；手工创建的线程池
  不在此列，需用 `ReadWriteRoutingContext.wrap(Runnable)`，否则线程以 `AUTO` 起步，
  未注解的 SELECT 走主库、`@Slave` 仍生效、写标记丢失。`tenantAwareTaskExecutor`
  自带的是租户装饰器，不会传播路由上下文，用它时同样需要显式 wrap。
- `@Slave(fallbackToMaster=false)` 在从库不可用时抛 `ReadWriteRoutingException`，
  不会静默回退主库。
- **`@DS` 与读写路由互斥（启动期强制校验）**：rw 开启时，如果应用里存在任意
  `@DS`（类或方法，限 `com.scmcloud` 包），或 `spring.datasource.dynamic.enabled`
  处于开启状态且配置了不是 rw group 的 `spring.datasource.dynamic.datasource.*`，
  `ReadWriteDatasourceConflictChecker` 会在 `afterSingletonsInstantiated` 抛
  `IllegalStateException` 并**启动失败**，错误信息会列出冲突的 Bean/方法/数据源名。
  原因：`@DS("org")` 会绕过 `ReadWriteRoutingDataSource`，写可能绕过主库、
  `@Slave` 读可能落到非副本库。解决方式：为每个 `@DS` 名建一个 rw group，
  或移除 `@DS`，或关闭 rw。`dynamic.enabled=false` 时额外 datasource 属性是惰性的，
  不会触发失败，但 `@DS` 注解仍会（它会让读者误以为注解生效）。

### 4.1 熔断（Sentinel）

- 每个 `<group>.<slave>` 注册两条规则：慢请求比例（连接持有时间 > `slow-rt-ms`
  的占比超过 `slow-ratio-threshold`）和异常比例（SQL 异常占比超过 `error-ratio`）。
- 入口在 `ReadWriteRoutingDataSource.getConnection()`：取从库连接前 `SphU.entry`，
  返回的连接是一个代理，`close()` 时 `entry.exit()`；SQL 异常在代理里
  `Tracer.trace()`。因此统计窗口覆盖**取连接 + 查询/事务持有时间**，不是只统计路由决策。
- 触发熔断后：该从库被本地 `open-until` 标记跳过（连连接探测都不做），
  本次请求直接回退主库并 `datasource.rw.fallback++`。
- `time-window-seconds` 过后本地标记解除，允许一次试探；试探成功则恢复 CLOSED，
  再次被 Sentinel 阻塞则重新打开。
- 熔断状态**优先于健康检查**：健康检查刚把从库标回可用，只要熔断窗口未结束，
  路由仍不会碰它。
- `circuit-breaker.enabled=false` 时不走 Sentinel，只剩“探测失败即熔断 + 健康检查恢复”。

### 4.2 链路追踪

`readWriteTracingAdvisor` 这个 Advisor 把 `ReadWriteTracingInterceptor` 挂在
`@Master / @Slave / @Transactional`（含类级）方法上，order 为
`HIGHEST_PRECEDENCE + 10`：在路由切面之内、事务拦截器之外。

- Span 名：`db.rw.<Class>.<method>`。
- 开始时：`db.rw.routing_type`（MASTER/SLAVE/AUTO）、`db.rw.force_master`、
  `db.rw.specified_slave`。
- 结束时：`db.rw.target`（master 或具体从库名）、`db.rw.fallback`（是否降级）、
  `db.rw.slave`。
- 同时打 DEBUG：`[RW-Trace] Xxx.method routing=SLAVE target=master slave=slave1 fallback=true`。
  没有配置 OTel exporter 时 span 是 no-op，但这条日志仍能回答“这次读为什么到了主库”。
- 记录的是**方法作用域内最后一次实际路由**；一个方法内多次切库时以最后一次为准，
  `fallback` 也取最后一次。

### 4.3 启动预热

- `ApplicationReadyEvent` 触发 `SlaveWarmupManager.warmupAll(...)`，为每个从库提交
  一个异步任务；任务在 `rw-slave-warmup` 守护线程池里执行，**不阻塞应用启动**。
- 每个从库打开一条连接并 `isValid(3)`，记录耗时与成功/失败；成功打 INFO
  `[Warmup] Slave [product.slave1] prewarmed in 12ms`，失败打 WARN 并 `markOffline`
  （失败不会影响启动，也不改变路由可用性——可用性仍由探测/健康检查决定）。
- 结果保留在 `/actuator/readwrite` 的 `warmup` 字段。
- 同一个 manager 也负责加权策略的权重爬升：`markOnline` 后 `warmupSeconds` 内
  权重线性增长；`ROUND_ROBIN`/`LEAST_CONNECTIONS` 不使用权重。

### 4.4 负载均衡

| 策略 | 行为 |
|------|------|
| `ROUND_ROBIN` / `RANDOM` | 在可用从库中轮询/随机，不看连接数 |
| `WEIGHTED_ROUND_ROBIN` / `WEIGHTED_RANDOM` | 使用 `weight`，并叠加预热权重 |
| `LEAST_CONNECTIONS` | 使用 `CountingDataSource` 的**实时持有连接数**，取最小；并列时取配置顺序第一个 |

`CountingDataSource` 在 `getConnection()` 成功后 +1，在返回连接的 `close()` 时 -1
（重复 close 只减一次）；`getConnection()` 抛异常时立即回滚。事务中连接被持有到
事务结束，计数也持续到那时。

## 5. 回退与降级

| 场景 | 行为 |
|------|------|
| 没有配置 `slaves` / `slaves-enabled=false` | 启动时打一条 INFO，所有读走主库 |
| 从库不可达（启动时） | `initialization-fail-timeout=-1`，连接池后台启动，**应用不会启动失败** |
| 从库首次连接探测失败 | 立即标记不可用（`fail-fast-on-slave-connection-failure=true`），后续请求直接走主库；健康检查恢复后自动重新可用 |
| 复制延迟超过 `replication-lag-tolerance` | 连续失败达到 `failure-threshold` 后标记不可用并回退主库 |
| 从库慢请求/异常比例超阈值 | Sentinel 熔断打开，窗口内直接回退主库；窗口结束后试探，成功恢复 |
| 熔断打开但健康检查刚把从库标回可用 | 熔断窗口未结束前仍不尝试该从库（熔断优先） |
| `groups` 为空但 `enabled=true` | WARN + 不注册路由 Bean，继续用单库 |
| 主库 url/username 非法 | 启动期 `ConfigurationProperties` 校验失败，错误信息含 `groups[...].master.username` |
| 同时存在 `@DS` / 额外 dynamic datasource | 启动失败，错误信息列出冲突的 Bean/方法/数据源 |

从库故障默认**不会**把 `/actuator/health` 变成 DOWN（`slave-down-affects-health=false`），
避免 K8s 因备库抖动重启正常 Pod；需要严格健康语义时可改为 `true`。

## 6. 观测

```bash
curl -H "X-Tenant-Id: <uuid>" http://localhost:8201/actuator/readwrite
```

返回包含：

- 每个 group 的 `slaveAvailability`、`healthStatus`（可用性/复制延迟/连续失败数）、
  `loadBalancer`、`routeCounts`；
- `circuitBreaker`：`slave:<group>.<slave>` 的 `state`（CLOSED/OPEN/HALF_OPEN）与 `openUntil`；
- `warmup`：每个从库预热的 `success`、`elapsedMs`、`error`；
- `currentThread`：调用线程的 `routingType`、`specifiedSlave`、`fallbackToMaster`、
  `forceMaster`、`scopeDepth`、`requestScope`、`lastTarget`/`lastSlave`/`lastFallback`、
  距上次写操作的毫秒数。

指标（Micrometer）：

| 指标 | 含义 |
|------|------|
| `datasource.rw.route{group,target=master}` | 路由到主库次数（含回退） |
| `datasource.rw.route{group,target=slave}` | 路由到从库次数 |
| `datasource.rw.fallback{group}` | 从库不可用/熔断回退次数 |
| `datasource.rw.retry{group}` | 从库重试次数 |
| `datasource.rw.replication.lag{group,slave}` | 复制延迟（ms） |
| `datasource.rw.pool.*{group,name}` | 各连接池 active/idle/total/pending 等 |

`target=slave` 长期为 0 时，优先检查：是否 `dynamic.enabled=false`、是否被非请求线程
调用（异步/定时任务默认主库）、是否所有查询都在事务内。

## 7. 语义边界与必须强制主库的场景

复制延迟是**物理事实**，路由层只能提供线程内的写后读窗口：

- 写后立刻读：只有**同一线程**且在同一请求内、`read-master-after-write` 窗口内的读
  会被强制主库。跨线程、跨请求、跨服务都不保证。
- 任何需要“读到刚写入结果”的场景必须显式 `@Master`（或放在主库事务内），不要依赖
  窗口，更不要写完后调用 `@Slave` 查询再返回。
- 金额、余额、库存扣减、状态机流转、幂等判定等强一致读必须走主库；库存热路径本身
  走 Redis + Lua，不经过该路由层。
- 报表、列表、导出等可容忍延迟的查询才适合 `@Slave`。
- 事务内所有语句都在事务开始时选定的库上执行，因此**命令方法必须自己带事务并标
  `@Master`**，不要在事务内部再期望切换。

## 8. 已验证 / 未验证

已执行（`mvn -o test`）：

- `scm-common/data-rw`：**106 个测试，0 失败，1 跳过**。新增覆盖：
  - 熔断：失败后打开、打开期间 `connect` 返回 null、窗口结束后 HALF_OPEN 试探成功恢复
    CLOSED、`enabled=false` 时惰性；
  - 数据源级熔断：从库查询异常 → 下一次 `getConnection` 回退主库并计 `fallback`；
  - 追踪：span 的 `db.rw.target`/`db.rw.fallback`/`specified_slave` 与异常状态；
  - 预热：异步预热成功/失败、耗时记录、`warmupAll` 覆盖所有从库；
  - 负载均衡：`LEAST_CONNECTIONS` 在有一个从库繁忙时选择另一个；计数在获取/关闭/
    失败获取三条路径上正确；
  - 启动校验：`@DS` 与额外 dynamic datasource 都会让 Context 启动失败，错误信息可读。
- `scm-common/data`：29 个测试，0 失败，3 跳过。
- `scm-product/service`：18 个测试，0 失败，含 `application.yml` 全量绑定（含
  `circuit-breaker` 段、从库 5s 超时、默认目标主库）。
- 交叉 `test-compile`：`scm-finance`、`scm-logistics`、`scm-audit`、`scm-order` 通过。

**真实双 PostgreSQL 测试（默认跳过，可一键启用）**：

```bash
# 前提：Docker daemon 可用（Docker Desktop / WSL2 docker / 远程 DOCKER_HOST 均可）
mvn test -pl ../scm-common/data-rw -Drw.it=true -Dtest=ReadWriteTwoDatabaseTest
```

`ReadWriteTwoDatabaseTest` 用两个 `postgres:16-alpine` 容器验证：写落主库、
`@Slave` 读落从库（从库有主库没有的行）、写后读窗口回主库、从库停机后降级主库。
它同时带 `@EnabledIfSystemProperty(rw.it=true)` 和
`@Testcontainers(disabledWithoutDocker = true)`，所以在没有 Docker 的机器上也不会
让 `mvn test` 变红。

**未验证**：

- 本机只安装了 Docker CLI，没有 Docker Desktop、没有 docker 服务，`docker version`
  连接 `//./pipe/docker_engine` 失败，无法启动守护进程；因此上面的真实双库测试
  **没有执行**，只完成了逻辑级与装配级验证。
- 没有启动完整 `scm-product`（需要 PostgreSQL/Nacos/Redis/XXL-Job）；YAML 与自动装配
  只在 `ApplicationContextRunner` 级别验证（不可达的 JDBC URL 也能正常启动）。
- Sentinel 在真实流量下的熔断触发时机（慢请求比例、异常比例、半开恢复）只做了
  缩小参数（`min-request-amount=1`、`stat-interval=1s`）的定时验证。
- 复制延迟、备库只读权限、真实故障切换、OTel exporter 的实际 span 上报需要在
  预发环境按第 9 节清单演练。

## 9. 上线检查清单

- [ ] 备库流复制已建立，`pg_stat_replication` 有记录且 `state=streaming`
- [ ] 从库账号只有 `SELECT` 权限，能连上目标库
- [ ] `spring.datasource.dynamic.enabled=false`，`rw.groups.<name>` 与库名一致
- [ ] 从库 `connection-timeout` ≤ 5s，`initialization-fail-timeout` 为负
- [ ] `/actuator/readwrite` 中 `slave1` 为 available，`replication.lag` 低于阈值
- [ ] `datasource.rw.route{target="slave"}` 有流量
- [ ] 所有写接口返回写后实体，而不是“写完再 `@Slave` 回查”
- [ ] 异步/定时任务中的强一致读已显式 `@Master` 或用 `ReadWriteRoutingContext.wrap`

## 10. 相关文档

- `docs/design/CQRS_ARCHITECTURE.md` §4.1 —— 设计与路由链路
- `docs/development/CQRS_PERFORMANCE_MONITORING.md` —— 监控与排障
- `docs/development/prometheus-cqrs-alerts.yml` —— 读写分离告警规则

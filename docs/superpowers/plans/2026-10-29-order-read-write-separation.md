# scm-order 主从读写分离落地

## 背景

`scm-common/data-rw` 实现完整（5 种负载均衡、从库健康检查、复制延迟检测、熔断、
连接池指标、actuator 端点），但 `ReadWriteAutoConfiguration` 挂在
`spring.datasource.rw.enabled=true` 上，全仓库无人设置该属性：

- 自动配置不加载 → `ReadWriteRoutingAspect` 未注册 → `@Master`/`@Slave` 全部是装饰注解；
- 即使打开开关，`spring.datasource.rw.groups` 也为空，构造期抛 `No datasource group configured`；
- `docker-compose.yml` 只有单机 PostgreSQL，没有流复制备库。

因此设计文档描述的 CQRS 主从路由在运行时并不存在。

## 本次范围

只做「让主从设计在代码层面真正成立」所需的最小改动，不引入新的运行时依赖。

### 1. `@Master` 补上写标记

`ReadWriteRoutingAspect.aroundMaster` 结束后调用 `ReadWriteRoutingContext.markWrite()`。

原因：`aroundTransactional` 只在 `current() == AUTO` 时才执行，因此同时带
`@Master` + `@Transactional` 的方法（scm-order 全部命令服务都是这样）一旦
`@Master` 的 advice 先执行，`markWrite()` 永远不会被调用，「写后读主库」窗口失效。

风险：当前 `lastWriteTime` 是 ThreadLocal 且无请求级清理，窗口按
`read-master-after-write`（默认 2s）自然过期，不会永久粘住主库。

### 2. 无从库时不再每次读都打 WARN

`ReadWriteRoutingDataSource.determineCurrentLookupKey()`：当 group 未配置任何
从库时，`selectSlaveWithRetry()` 恒返回 null，原实现对每一次查询打
`WARN no available slave, fallback to MASTER`。单机部署下这是日志洪泛，
改为 DEBUG；真·从库全部不可用时仍保留 WARN。

### 3. 命令服务返回写后实体，消除「写完再查」

`OrdOrderController.updateStatus` / `cancel` 在命令成功后立刻调用
`queryService.getById()`，而该方法标注 `@Slave`。今天因为注解失效所以无害，
一旦主从生效就会因复制延迟返回旧状态。

改为 `OrdOrderCommandService.updateOrderStatus` / `cancelOrder` 返回更新后的
`OrdOrder`（`null` 表示订单不存在），controller 直接使用返回值，不再二次查询。

注：`updateOrderStatus` / `cancelOrder` 的 outbox 写入在改动前已经存在，
原计划中的 TODO 已不成立。

### 4. 主从配置模板

新增 `scm-order/service/src/main/resources/application-readwrite.yml`，
默认不激活（`application.yml` 未开启 `readwrite` profile 时完全不加载）。
激活方式与从库接入步骤写入 `docs/guides/READ_WRITE_SEPARATION_GUIDE.md`
（该文件被 `docs/development/CQRS_PERFORMANCE_MONITORING.md` 引用但此前不存在）。

### 5. 测试

- `ReadWriteRoutingAspectTest`：`@Master` 成功返回后 `lastWriteTime` 非空；
  异常返回时不标记且正确弹栈；
- `ReadWriteRoutingDataSourceTest`：无从库 / 从库不可用时回退主库、
  从库可用时路由从库、写后读窗口强制主库；
- `ReadWriteProfileConfigTest`：解析 `application-readwrite.yml`，校验
  `dynamic.primary=order`、group 形状与一致性窗口；
- 更新 `OrdOrderCommandServiceEventTest` 适配返回值变化，补充 4 个用例。

## 不在本次范围

- `OrderDubboServiceImpl.createOrder` 绕过 outbox、未写明细与状态历史；
- `OrdOrderServiceImpl` 与 `OrdOrderCommandService` 的双写路径合并；
- 真实备库的部署与流复制配置。

## 验证

```bash
mvn test -pl ../scm-common/data-rw -f com.scm.parent/pom.xml
mvn test -pl ../scm-order/service -Dtest='OrdOrderCommandServiceEventTest' -f com.scm.parent/pom.xml
```

# InvInventoryController 优化设计

**日期**: 2026-08-12
**作者**: opencode (brainstorming session)
**范围**: `scm-inventory/service` 模块的 `InvInventoryController`
**状态**: 设计草案，等待用户审阅

## 背景

`InvInventoryController` 现有实现存在以下问题：

1. **重复异常处理**: `adjustInventory`、`transferInventory`、`initInventory` 三个方法各自包含相同的 `try/catch` 块，仅做日志+重新包装 `RuntimeException`，而项目已存在 `GlobalExceptionHandler` 处理所有常见异常
2. **缺少幂等保护**: AGENTS.md 明确要求"关键操作（库存扣减、订单创建）使用请求 ID 存储在 Redis 中 24h 过期"，但当前所有写接口均未实现
3. **缺少读缓存**: 库存查询是热点路径，但 `getInventory`、`checkStockAvailable` 等接口每次都直接走 service，未利用现有 Caffeine + Redis 两级缓存
4. **返回类型不一致**: 直接返回实体（项目惯例），未利用 `ApiResponse<T>` 统一包装，与 `GlobalExceptionHandler` 响应风格不对齐
5. **日志样式混乱**: emoji 拼字符串 + 混杂 `INFO/DEBUG/WARN` 级别，缺少结构化字段，未使用项目 `LogUtils`
6. **校验不充分**: 批量接口未限制 skuIds 上限，分页 size 未限制最大值
7. **缺少限流**: 高频接口无防护，可能被滥用拖垮下游

## 目标

按四个方向全面优化：

| 方向 | 措施 |
|------|------|
| 代码质量 | 删除 controller 内 try/catch，统一 `ApiResponse<T>` 包装 |
| 可观测性 | 用 `LogUtils.business()` 替代手写日志，限流熔断由 Sentinel 上报 |
| 性能 | 热点查询接口加两级缓存（30~60s TTL） |
| 安全/健壮性 | 写接口加 `@Idempotent`，批量接口限制 size，分页限制 max size |

## 非目标（Out of Scope）

- 不修改 `IInvInventoryService` 及其实现类
- 不修改 DTO 定义
- 不修改前端 `scm-web/src/features/inventory/services/inventory.service.ts`（破坏性变更留待后续 PR）
- 不引入 Swagger/OpenAPI（项目所有 controller 均未启用，保持一致）
- 不动 `GlobalExceptionHandler` / `IdempotentAspect` / `TwoLevelCache` 自身

## 设计

### 1. 接口总览

| 方法 | HTTP | 路径 | 返回类型 | 缓存 | 限流 (qps) | 幂等 |
|------|------|------|----------|------|------------|------|
| `getInventory` | GET | `/api/v1/inventory` | `ApiResponse<InventoryResponse>` | `inventory` 60s | 200 | — |
| `batchGetInventory` | POST | `/api/v1/inventory/batch` | `ApiResponse<List<InventoryResponse>>` | `inventory` 60s | 50 | — |
| `queryInventory` | POST | `/api/v1/inventory/query` | `ApiResponse<Page<InventoryResponse>>` | — | 100 | — |
| `adjustInventory` | POST | `/api/v1/inventory/adjust` | `ApiResponse<InventoryResponse>` | — | 50 | `@Idempotent(key=#request.adjustNo, ttl=24h)` |
| `transferInventory` | POST | `/api/v1/inventory/transfer` | `ApiResponse<Boolean>` | — | 30 | `@Idempotent(key=#request.transferNo, ttl=24h)` |
| `checkStockAvailable` | GET | `/api/v1/inventory/check` | `ApiResponse<Boolean>` | `inventory` 30s | 200 | — |
| `getInventoryStats` | GET | `/api/v1/inventory/stats` | `ApiResponse<InventoryStatsResponse>` | `inventoryStats` 60s | 20 | — |
| `initInventory` | POST | `/api/v1/inventory/init` | `ApiResponse<InventoryResponse>` | — | 20 | `@Idempotent(key=#skuId+':'+#warehouseId, ttl=24h)` |

### 2. 缓存策略

复用现有 `TwoLevelCache` (Caffeine L1 5min + Redis L2)，无需新建缓存组件。

需要在 `RedisConfig.CACHE_TTLS` 中新增两个 cache name：

```java
Map.entry("inventory", Duration.ofSeconds(60)),
Map.entry("inventoryStats", Duration.ofSeconds(60)),
```

> 注意：`checkStockAvailable` 也走 `inventory` cache，但因入参含 `quantity`，缓存键会动态生成（quantity 变化时失效快），TTL 30s 偏短保证快速失效。

缓存 key 生成（由 `@Cacheable` SpEL 自动生成）：
- `getInventory`: `#skuId + ':' + #warehouseId`
- `batchGetInventory`: `#warehouseId + ':' + #skuIds?.toString()`（warehouseId 可空）
- `checkStockAvailable`: `#skuId + ':' + #warehouseId + ':' + #quantity`
- `getInventoryStats`: 常量键 `"global"`（stats 是全量）

### 3. 幂等策略

复用现有 `@Idempotent` 注解 + `IdempotentAspect`：

- 三个写接口均加 `@Idempotent`
- SpEL key 从请求体字段生成，重复请求 24h 窗口内直接抛 `BusinessException(IDEMPOTENT_REPLAY)`
- 读接口**不**加 `@Idempotent`：重复请求由缓存自然去重，避免向客户端返回错误
- `initInventory` 的 key 用 `skuId:warehouseId`：业务上同一 SKU+仓库不应重复初始化

### 4. 限流策略

复用现有 Sentinel + `BlockExceptionHandlerStrategy`，通过 `@SentinelResource` 注解声明资源名和阈值：

```java
@SentinelResource(value = "inventory.get", blockHandler = "handleBlock")
public ApiResponse<InventoryResponse> getInventory(...) { ... }
```

Sentinel 规则配置后续单独处理（通过 Nacos 动态下发或本地静态），本任务仅声明注解。

### 5. 校验增强

| 参数 | 新增校验 |
|------|----------|
| `batchGetInventory` 的 `skuIds` | `@NotEmpty` + `@Size(max = 100, message = "批量查询 SKU 数量不能超过 100")` |
| `adjustInventory` 的 `request.quantity` | `@Max(value = 10000, message = "单次调整数量不能超过 10000")` |
| `transferInventory` 的 `request.quantity` | `@Max(value = 10000)` |
| `queryInventory` 的 `request.page` | `@Min(1)`（默认已有） |
| `queryInventory` 的 `request.size` | `@Min(1) @Max(500)` |

### 6. 日志改造

写操作前打 `LogUtils.business("inventory.adjust", "start", request)`，结果后打 `LogUtils.business("inventory.adjust", "success", result)`。

错误日志交给 `GlobalExceptionHandler` 统一记录（含 traceId），controller 不再重复 log.error。

### 7. 测试影响

`scm-inventory/service/src/test` 下可能存在 controller 测试：

- 若有，所有 mock 期望需从返回实体改为返回 `ApiResponse`
- service 层单元测试不受影响

## 风险与权衡

| 风险 | 缓解 |
|------|------|
| `ApiResponse` 包装是破坏性变更，前端所有 inventory 调用需同步解包 | 留作独立 PR，本 PR 不动前端；前端可在同一迭代内跟进 |
| `inventory` 缓存 key 含 SKU+仓库组合，SKU 维度爆炸风险 | TTL 60s + Caffeine L1 10000 条上限控制占用 |
| `@Idempotent` 抛错对客户端不友好（写操作中途网络重试会失败） | 这是项目既定行为，AGENTS.md 已要求 24h 幂等；客户端需传唯一业务号 |
| Sentinel 规则阈值硬编码 vs Nacos 动态下发 | 本任务仅声明 `@SentinelResource`，阈值后续单独 PR 通过 Nacos 配置 |

## 实施步骤概要

1. 新增 `inventory`、`inventoryStats` 到 `RedisConfig.CACHE_TTLS`
2. 重写 `InvInventoryController`：
   - 删除所有 `try/catch`
   - 所有方法返回 `ApiResponse<T>`
   - 加 `@Cacheable` / `@SentinelResource` / `@Idempotent`
   - 校验注解补充
   - 日志替换为 `LogUtils.business`
3. 更新/新增 controller 单元测试（如有）
4. `mvn verify -pl scm-inventory/service -f com.scm.parent/pom.xml` 通过

## 待用户确认事项

无（已通过 brainstorming 会话全部确认）。
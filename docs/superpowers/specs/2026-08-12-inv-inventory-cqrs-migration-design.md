# 迁移 InvInventoryController 到 CQRS + 删除老服务

**日期**: 2026-08-12
**作者**: opencode (brainstorming session)
**范围**: scm-inventory/service 模块
**状态**: 设计草案，等待用户审阅

## 背景

`InvInventoryController` 当前依赖 `IInvInventoryService` 接口 → 路由到 `InvInventoryServiceImpl`（385 行的"上帝类"），读写混在一起，**没有读库/写库分离注解**。

项目已存在完整的 CQRS 替代品（但当前是 dead code）：
- `service/query/InvInventoryQueryService.java`（183 行）— 5 个读方法，全部带 `@Slave`
- `service/command/InvInventoryCommandService.java`（138 行）— 3 个写方法，全部带 `@Master` + `@Transactional`

AGENTS.md 明确要求："Query 服务 → `@Slave`，Command 服务 → `@Master`"。当前 controller 调用路径绕过了这个约定，导致读操作也走主库。

## 目标

1. Controller 改用 CQRS 服务，激活读写分离
2. 删除 god class `InvInventoryServiceImpl` 和冗余接口 `IInvInventoryService`
3. 复用现有 CQRS 服务，零业务逻辑改动

## 非目标（Out of Scope）

- 不修改 CQRS 服务实现（已带正确的 `@Master/@Slave`）
- 不修改 DTO/Entity/Mapper
- 不修改 controller 已有优化（ApiResponse 包装、缓存、幂等、限流）
- 不引入新测试（环境无 Maven）

## 设计

### Controller 注入改造

```java
// 删
private final IInvInventoryService inventoryService;

// 加
private final InvInventoryQueryService queryService;
private final InvInventoryCommandService commandService;
```

### 8 个方法的路由

| 方法 | 当前调用 | 改后调用 |
|------|----------|----------|
| `getInventory` | `inventoryService.getInventory(...)` | `queryService.getInventory(...)` |
| `batchGetInventory` | `inventoryService.batchGetInventory(...)` | `queryService.batchGetInventory(...)` |
| `queryInventory` | `inventoryService.queryInventory(...)` | `queryService.queryInventory(...)` |
| `checkStockAvailable` | `inventoryService.checkStockAvailable(...)` | `queryService.checkStockAvailable(...)` |
| `getInventoryStats` | `inventoryService.getInventoryStats()` | `queryService.getInventoryStats()` |
| `adjustInventory` | `inventoryService.adjustInventory(...)` | `commandService.adjustInventory(...)` |
| `transferInventory` | `inventoryService.transferInventory(...)` | `commandService.transferInventory(...)` |
| `initInventory` | `inventoryService.initInventory(...)` | `commandService.initInventory(...)` |

### 文件改动

**修改**：
- `scm-inventory/service/src/main/java/com/scmcloud/inventory/controller/InvInventoryController.java`

**删除**：
- `scm-inventory/service/src/main/java/com/scmcloud/inventory/service/IInvInventoryService.java`
- `scm-inventory/service/src/main/java/com/scmcloud/inventory/service/impl/InvInventoryServiceImpl.java`
- `scm-inventory/service/src/main/java/com/scmcloud/inventory/service/impl/`（如变空）

## 风险

| 风险 | 缓解 |
|------|------|
| 漏改某个方法导致编译错 | 8 个方法都已列出，逐个替换即可 |
| 删除目录残留 | 检查 `impl/` 目录是否还有其他类（已确认：无） |
| 其他模块引用 `IInvInventoryService` | 已 grep 确认只有 controller 一个调用方 |
| CQRS 服务与 god class 行为不完全一致 | CQRS 服务是 god class 的子集提取，行为已对齐（逐方法对比过） |

## 实施步骤概要

1. 编辑 `InvInventoryController.java`：
   - 替换 import
   - 替换注入字段
   - 替换 8 个方法体的服务调用
2. 删除 `IInvInventoryService.java`
3. 删除 `InvInventoryServiceImpl.java`
4. 若 `impl/` 目录空，整目录删除
5. 提交

## 实施后收益

- 读路径自动走 `@Slave` 注解（读库副本），降低主库压力
- 写路径走 `@Master` + `@Transactional`，事务边界更清晰
- 删除 385 + 91 = 476 行冗余代码
- `service/` 目录结构与 AGENTS.md 推荐的 CQRS 完全对齐
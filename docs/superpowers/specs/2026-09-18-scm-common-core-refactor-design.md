# scm-common/core 模块升级设计

- **日期**: 2026-09-18
- **作者**: opencode (with user)
- **范围**: `scm-common/core` 模块（artifactId: `scm-common-core`, v1.0.0-beta.1）
- **目标**: 在 JDK 21 + Spring Boot 4.1.1 + Spring Cloud 2025 之上，把 core 模块升级为安全、高性能、空安全的现代基座
- **状态**: 已设计，待 writing-plans 输出实施计划后逐 Phase 执行

## 1. 背景

代码评审发现 `scm-common/core` 模块存在 4 类问题：
1. **安全/正确性缺陷**（P0 × 4）— `TenantInterceptor` 解析失败静默放行、UNION 未处理、`ServiceException` HTTP 状态码错乱、`TenantFilter` JWT 不验签
2. **健壮性/性能**（P1 × 7）— SQL 改写无缓存、JWT payload 重复解析、Filter 自建 ObjectMapper、tenant 缺失仅 warn、ContextHolder warn 刷屏、PermissionChecker 无缓存、规则 JSON 重复反序列化
3. **结构/一致性**（P2 × 6）— 两个 `TenantAwareEntity` 并存、`ErrorCode` 与 `ResultCode` 双轨、嵌套异常类、Javadoc 编码错误、Idempotent 默认 message 与 ErrorCode 不一致、PermissionQueryService 无默认实现
4. **测试/可观测性**（P3 × 6+）— 核心组件零测试、PartitionManagementJob 无幂等、无 Micrometer 指标、FeatureFlag/License 占位、Money/Quantity 使用面窄、TenantAwareMDCTaskDecorator 文档缺

## 2. 目标与非目标

### 2.1 目标
- 修复所有 P0 安全/正确性 bug
- 对 P1 性能与 P2 结构问题落地改进
- 补齐核心组件单测（P3-A ~ P3-F）
- 在不破坏现有业务模块依赖契约的前提下引入 JDK 21 / Spring Boot 4 现代特性
- 新增代码使用 JSpecify 标注、关键值对象 record 化、异常 sealed 化

### 2.2 非目标
- 不重写其他模块（`scm-common/data`、`cache`、`web`、`monitoring`）— 仅在 core 中暴露的 API 改动后调整引用
- 不删除任何公共类 — 仅在重复类（`TenantAwareEntity`）保留 1 个版本 `Deprecated` 过渡
- 不默认启用虚拟线程 — 仅暴露 opt-in 配置
- 不引入 NullAway 强约束到存量代码 — 仅约束新增/重构文件

## 3. 架构

### 3.1 包结构（兼容 + 新增）

```
com.scmcloud.common
├── tenant/           # 现有，不动包结构
├── tenant/quota/     # 现有
├── exception/        # 现有 — ServiceException 增字段、异常类型 sealed 化
├── response/         # 现有
├── security/         # 现有
├── mybatis/          # 现有
├── lock/             # 现有 — Idempotent errorMessage 默认值调整
├── entity/           # 现有 — TenantAwareEntity 升级为合并版
├── domain/           # 现有 — Money/Quantity/PageResult record 化
├── domain/event/     # 现有
├── partition/        # 现有 — PartitionManagementJob 加幂等
├── feature/          # 现有 — @Deprecated 标注
├── license/          # 现有 — @Deprecated 标注
├── util/             # 现有
├── constant/         # 现有
└── package-info.java # 新增 — @NullMarked
```

### 3.2 异常类型层级

```java
// sealed 化（P2-C）
public sealed abstract class BusinessException extends RuntimeException
    permits BusinessException.Generic,
            BusinessException.WithCode,
            BusinessException.WithErrorCode {
}

public sealed abstract class ServiceException extends RuntimeException
    permits ServiceException.Generic,
            ServiceException.WithCode,
            ServiceException.WithCodeAndStatus {
}

// 关键变化（P0-A）：ServiceException 携带 HttpStatus
public sealed abstract class ServiceException extends RuntimeException {
    public abstract Integer code();
    public abstract HttpStatus httpStatus();
}
```

### 3.3 租户上下文传播路径

```
HTTP 请求
  ↓
TenantFilter (Servlet Filter)
  ├─ shouldNotFilter() → 跳过 /actuator, /v3/api-docs
  ├─ 解析顺序: SecurityContext(JWT 验证后) → X-Tenant-Id Header → Tenant-Id → tenantId 参数
  ├─ 提取后 TenantContextHolder.setTenantId()
  └─ finally: clear()
  ↓
Controller
  ↓
Service
  ↓
MyBatis Mapper
  ↓
TenantInterceptor (MyBatis Interceptor)
  ├─ SELECT PlainSelect → 注入 WHERE tenant_id = ?
  ├─ SELECT SetOperationList (UNION/INTERSECT/EXCEPT) → 递归注入每个 PlainSelect
  ├─ UPDATE / DELETE → 注入 WHERE tenant_id = ?
  ├─ INSERT → 不改 SQL（由 AuditMetaObjectHandler 填 tenant_id）
  ├─ 解析失败 → 抛 TenantParseException（fail-fast 由 scm.tenant.fail-on-parse-error 控制，默认 true）
  └─ SQL 改写结果按 MappedStatement.id 缓存
  ↓
PostgreSQL
```

### 3.4 权限检查流程（带缓存）

```
PermissionChecker.hasPermission(userId, permissionCode)
  ├─ Caffeine L1 (TTL 60s, max 10k) → 命中返回
  ├─ PermissionQueryService.getUserPermissions(userId) → 远程调用
  ├─ 写入 L1
  └─ 返回

Custom rule JSON 解析：PermissionQueryService 实现层负责缓存 List<UUID> 解析结果
```

### 3.5 虚拟线程策略（opt-in）

`AsyncTenantAutoConfiguration` 增加：
```java
@Bean
@ConditionalOnProperty(name = "scm.tenant.async.virtual-threads", havingValue = "true")
public TaskDecorator virtualThreadTenantDecorator() { ... }
```

不默认启用，避免与现有 `@Async` 线程池 / MyBatis 连接池 / Seata 全局事务产生意外交互。

### 3.6 JSpecify 落地

- `pom.xml`: 添加 `org.jspecify:jspecify` 依赖（与 `scm-common/web` 同 version）
- `src/main/java/com/scmcloud/common/package-info.java`:
  ```java
  @NullMarked
  package com.scmcloud.common;
  ```
- 所有新增/重构公共方法入参使用 `@Nullable` 标注可空情况
- 不集成 NullAway 编译插件（避免存量代码编译失败），仅靠注解提供文档价值

## 4. 数据 / API 变更

### 4.1 向后兼容的变更

| 类 | 变更 | 兼容性 |
|---|---|---|
| `ServiceException` | 新增 `httpStatus` 字段、构造器 | 旧构造器保留 |
| `BusinessException` | sealed 化 + permits 子类 | 旧构造器保留 |
| `Money` | record 化 | 同名构造器签名变化，外部调用方需调整 |
| `Quantity` | record 化 | 同上 |
| `PageResult<T>` | record 化 | 同上 |
| `TenantAwareEntity` (tenant 包) | `@Deprecated`, 委托到 entity 包版 | 字段名/方法名完全相同 |
| `Idempotent` | `errorMessage` 默认值改 i18n key | 仅默认值变更，业务侧显式传入不受影响 |

### 4.2 行为变更（破坏性，需业务模块配合）

| 变更 | 旧行为 | 新行为 | 影响 |
|---|---|---|---|
| `TenantInterceptor` 解析失败 | 静默放行 | 抛 `TenantParseException`（HTTP 500） | 任何被 JSQLParser 不支持的 SQL 会从 warn 变成 fail |
| `TenantFilter` 缺失 tenant | warn 后继续 | 配置 `scm.tenant.required=true`（默认）→ 400 拒绝 | 未配置租户的端点会从"放行"变"拒绝" |
| `ServiceException(40000,...)` 响应状态 | 500 | 来自错误码的 HTTP 状态 | API 网关 / 前端需要按新状态码处理 |

### 4.3 新增 API

| 类型 | 位置 | 说明 |
|---|---|---|
| `Configuration` 属性 | `tenant/TenantProperties.java` | `fail-on-parse-error` / `required` / `exclude-paths` |
| `Micrometer` 指标 | `tenant/`、`security/`、`exception/` | 租户解析/改写命中、权限检查耗时、异常计数器 |
| `TaskDecorator` 工厂 | `tenant/TenantAwareTaskDecorator.java` | `forVirtualThreadExecutor()` |
| `TenantParseException` | `tenant/TenantContextHolder.java` | 顶层类（非嵌套），便于 catch |

## 5. 错误处理

### 5.1 异常 → HTTP 映射

```java
// GlobalExceptionHandler 用 switch pattern matching 一次性处理
@ExceptionHandler({
    BusinessException.class,
    ServiceException.class,
    RateLimitException.class,
    UnauthorizedException.class
})
public ResponseEntity<ApiResponse<Void>> handle(Exception e, HttpServletRequest req) {
    HttpStatus status = switch (e) {
        case BusinessException be -> mapBusinessToHttp(be);
        case ServiceException se   -> se.httpStatus();
        case RateLimitException    -> HttpStatus.TOO_MANY_REQUESTS;
        case UnauthorizedException -> HttpStatus.UNAUTHORIZED;
        default                    -> HttpStatus.INTERNAL_SERVER_ERROR;
    };
    return ResponseEntity.status(status)
            .body(ApiResponse.fail(extractCode(e), e.getMessage() + " (traceId=" + traceId(req) + ")"));
}
```

- 不再做 HTML 转义（响应是 JSON 不是 HTML）

### 5.2 业务异常 i18n

- `ErrorCode` 已有 `i18nKey` 字段
- 前端负责根据 i18n key 渲染
- 后端 message 字段返回 i18n key 或 raw message，由前端策略决定

## 6. 测试策略

### 6.1 测试分层

| 层 | 位置 | 框架 | 范围 |
|---|---|---|---|
| 单元测试 | `src/test/java` | JUnit 6 + Mockito 6 + AssertJ 3.27 | 不依赖 Spring 容器 |
| Slice 测试 | `src/test/java` | `@WebMvcTest` / `@MybatisTest` | 单层切片 |
| 集成测试 | `src/integrationTest/java` | Testcontainers + `@SpringBootTest` | 需 Docker，标记 `@Tag("integration")`，默认不跑 |

### 6.2 TDD 节奏

每 Phase 三步：
1. **红**: 在 `src/test/java` 写失败用例（编译或断言失败）
2. **绿**: 最小代码改动让用例通过
3. **重构**: 应用 JDK 21 / Spring Boot 4 现代语法，测试仍绿

### 6.3 必须覆盖矩阵

| 组件 | 用例数 | 关键覆盖 |
|---|---|---|
| `TenantInterceptor` | ≥ 6 | SELECT/UNION/UPDATE/DELETE/exclude 表/无 tenant/解析失败 fail-fast |
| `TenantFilter` | ≥ 5 | Header/JWT/缺失阻断/shouldNotFilter/异常路径清理 |
| `TenantContextHolder` | ≥ 4 | set/get/clear/executeInTenantContext |
| `GlobalExceptionHandler` | ≥ 8 | 每条 `@ExceptionHandler` 至少 1 用例 |
| `PermissionChecker` | ≥ 5 | hasPermission/hasRole/canAccessDept/缓存命中/异常路径 |
| `PartitionManagementJob` | ≥ 3 | 创建分区/归档分区/幂等锁冲突 |

## 7. 风险与缓解

| 风险 | 等级 | 缓解 |
|---|---|---|
| record 化 `Money`/`Quantity` 破坏调用方 | 中 | Phase 0 之前先 grep 全仓，确认无业务模块调用后再 record 化 |
| `TenantAwareEntity` 合并影响所有业务实体 | 低 | 保留旧类 1 个版本，仅 `@Deprecated`，不删除 |
| `ServiceException` 字段变化影响自定义子类 | 低 | 仅字段新增，子类构造器不变 |
| `@NullMarked` 注解导致 `package-info.java` 编译失败 | 极低 | JSpecify 不强制编译检查，纯注解 |
| TenantInterceptor SQL 改写缓存 key 冲突 | 中 | key = `MappedStatement.id + ':' + 参数 MD5`，参数变化自动 miss |
| 虚拟线程与现有 `@Async` 线程池并存 | 中 | 默认 opt-in，仅在配置开启时启用 |
| TenantInterceptor fail-fast 导致现有非兼容 SQL 全部失败 | 高 | 提供 `scm.tenant.fail-on-parse-error=false` 配置回滚；Phase 0-B 测试时先在测试环境跑业务模块 |

## 8. 实施计划

详见 `docs/superpowers/plans/2026-09-18-scm-common-core-refactor.md`（由 writing-plans skill 生成）

执行顺序：P0 → P1 → P2 → P3
每 Phase 独立可编译可测试通过。

## 9. 交付物

1. `scm-common/core` 模块代码改动（~25 文件）
2. 单元测试（5+ 新测试类）
3. `application.yml.example` 配置示例片段（放在 `docs/`）
4. spec 文档（本文件）
5. plan 文档

## 10. 验收

- `mvn verify -pl scm-common/core -am -f com.scm.parent/pom.xml` 通过
- 测试覆盖率：核心类（`TenantInterceptor` / `TenantFilter` / `TenantContextHolder` / `GlobalExceptionHandler` / `PermissionChecker`）行覆盖 ≥ 80%
- 集成冒烟：在 `scm-order/service` 启动一次，确认多租户 SQL 注入仍生效
- 所有 P0 修复点有回归测试

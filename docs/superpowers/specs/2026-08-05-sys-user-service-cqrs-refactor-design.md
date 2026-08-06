# SysUserService CQRS 重构设计文档

**日期**: 2026-08-05
**状态**: 已批准
**作者**: opencode

## 1. 背景与目标

### 1.1 背景
`scm-system/service/.../ISysUserService.java` 与 `Impl/SysUserServiceImpl.java` 当前合计约 950 行，单一接口暴露 19 个方法，覆盖：

1. 用户 CRUD（增/删/改/查/分页）
2. 鉴权查询（Spring Security 登录、`SecurityUser` 装配）
3. 密码生命周期（重置/修改）
4. 角色授予 + 临时角色生命周期
5. 锁/解锁 + 数据权限统计
6. 登录信息更新

实现层 (`SysUserServiceImpl`) 同时存在以下问题：

| 类别 | 具体问题 |
|---|---|
| CQRS 偏离 | 22 个方法集中在单一 `ISysUserService`，未拆分查询侧/命令侧 |
| 职责过载 | 临时角色方法（`grantTemporaryRoles` 等）只访问 `db_permission`，与用户 CRUD 无直接关系 |
| 缓存粒度 | 全部 `@CacheEvict` 使用 `allEntries=true` 或不带租户前缀的 key，跨租户误删 |
| 读路由 | `listUsers` 仅有 `@Transactional(readOnly=true)`，未加 `@Slave` |
| 代码重复 | 命令方法 8 步样板（租户校验 → 操作员解析 → 权限校验 → 加载用户 → 404 → 归属校验 → 数据范围 → 审计日志）逐方法复制 |
| 错误码 | `resetPassword` / `changePassword` 仍使用原始字符串 `BusinessException`，未走 `ResultCode` 枚举 |
| 注解缺失 | `listUsers` / `getUserById` / `getUserInfo` / `addUser` / `updateUser` / `deleteUser` / `updateLastLogin` 等接口方法未标注 `@Override` |
| 硬编码常量 | `deleteUser` 中内置 UUID `019a0aee-3b74-7bfc-b34f-48b5428d4875` 标识"超管账号"，不可配置 |
| 辅助方法 | `userStatusName` 将 `0/1/2` 映射为 `INACTIVE/ACTIVE/LOCKED`，无集中枚举 |
| 小性能点 | `generateRandomPassword()` 每次调用 `new SecureRandom()` |

### 1.2 目标
- 将单一服务拆为查询/命令两侧，沿用项目已有的 `service/query/` + `service/command/` CQRS 模式
- 把临时角色生命周期抽离到独立的角色命令服务
- 抽出跨切面样板（租户/权限/归属/审计）至统一模板
- 缓存键改为租户感知；为每个读方法显式 `@Slave`
- 用 `ResultCode` 替换字符串异常
- 配置化超管 UUID，统一状态枚举
- **保持现有业务行为不变**（不补权限漏洞、不改接口语义、不动权限相关 out-of-scope 的缺陷）

### 1.3 非目标
- 不新增测试覆盖（用户明确不要求）
- 不修改 `UserCrossDatabaseQueryService` / `UserRoleCrossDatabaseCommandService` / `PermissionChecker` 等基础设施
- 不修复 `grantTemporaryRoles` 缺权限校验等历史遗留问题
- 不重构 `controller` 层（除非引用了即将被删除的接口）

## 2. 设计方案

### 2.1 服务拆分（硬切分，无兼容 façade）

| 新接口 | 方法 | 所在包 | 备注 |
|---|---|---|---|
| `ISysUserQueryService` | `listUsers`, `getUserById`, `getUserByUsername`, `getUserInfo`, `getUserTemporaryRoles`, `canAccessDept`, `getUserDataScope`, `getUserStatistics` | `service/query/` | 只读，全部 `@Slave` |
| `ISysUserCommandService` | `addUser`, `updateUser`, `deleteUser`, `resetPassword`, `changePassword`, `grantRoles`, `lockUser`, `updateLastLogin` | `service/command/` | 写，全部 `@Transactional` |
| `IUserRoleCommandService` | `grantTemporaryRoles`, `extendTemporaryRole`, `terminateTemporaryRole` | `service/command/` | 仅与 `db_permission` 交互 |

旧接口 `ISysUserService` / `SysUserServiceImpl` 在所有调用方迁移完成后**直接删除**，不留兼容 façade（项目已有的 CQRS 类如 `UserCrossDatabaseQueryService` 也未保留旧 façade，保持一致）。

### 2.2 缓存键策略

| 缓存名 | 写入 | 失效 |
|---|---|---|
| `user` (DTO) | `key = T(TenantAwareCacheKeyGenerator).generate("user", #id)` | 命令侧按 `tenantId + id` 失效 |
| `userDetails` (Spring Security) | `key = T(...).generate("userDetails", #username)` | 命令侧按 `tenantId + username` 失效 |
| `userInfo` (含菜单) | 同上 | 同上 |
| `userTemporaryRoles` | 同上 | 同上 |
| `userDataScope` | 同上 | 同上 |

- 删除所有 `@CacheEvict(value=..., allEntries=true)`；改为按租户+主键的精确失效。
- 依赖 `scm-common/cache/.../TenantAwareCacheKeyGenerator`（已存在，无需新写）。
- 注：`@Cacheable` 侧的 key 在注解中用 SpEL 调用静态生成器：`key = "T(com.scmcloud.common.cache.TenantAwareCacheKeyGenerator).generate('user', #id, #root.target.tenantContextHolder.tenantId)"`（具体 SpEL 视 API 而定，落地时按 TenantAwareCacheKeyGenerator 真实签名调整）。

### 2.3 跨切面模板 `UserCommandTemplate`

新增 `scm-system/service/.../command/UserCommandTemplate.java`，对外暴露：

```java
public class UserCommandTemplate {
    /**
     * 执行一条用户命令的标准前置流程：
     * 1) 校验租户上下文
     * 2) 取当前操作员 UUID（可空）
     * 3) 权限校验 operatorPermission
     * 4) 通过 loader 加载 SysUser，不存在抛 ResultCode.USER_NOT_FOUND
     * 5) 校验数据归属
     * 6) 数据范围校验（operatorId, user.createBy, user.deptId）
     * 7) 调用 action(user, operatorId)
     * 8) 记录审计日志 TenantValidationUtil.logTenantOperation(...)
     */
    public void execute(UUID operatorPermission, String action, String resourceType,
                        Function<UUID, SysUser> loader, BiConsumer<SysUser, UUID> action) { ... }

    public void execute(UUID userId, String operatorPermission, String action,
                        BiConsumer<SysUser, UUID> body) { ... }   // 简化版：直接给 userId
}
```

约束：
- `addUser`（创建）不进入 `execute`，因为没有现成 `SysUser` 可加载，保留 `addUser` 自己的样板（但仍调用 `permissionChecker.requirePermission` + 审计日志）。
- `changePassword` / `resetPassword` / `lockUser` / `updateLastLogin` 属于"轻命令"，可走简化版或直接内联（不强求走模板）。
- `grantRoles` 走 `execute()` 完整版（标准 8 步 + 自带的角色等级校验，作为 `execute` 的扩展参数）。
- `grantTemporaryRoles` / `extendTemporaryRole` / `terminateTemporaryRole` 迁到 `UserRoleCommandServiceImpl`，**不**走模板（它们操作的是关联表，无 SysUser 直接加载）。

### 2.4 错误码补全

`scm-common/.../response/ResultCode` 新增：
- `USER_NOT_FOUND` 已存在 → 复用
- 新增 `PASSWORD_INCORRECT_OLD`（替换 `new BusinessException("原密码不正确")`）
- 新增 `PASSWORD_SAME_AS_OLD`（替换 `new BusinessException("新密码不能与原密码相同")`）

### 2.5 配置化超管 UUID

`application*.yml` 新增：
```yaml
app:
  security:
    admin-user-id: 019a0aee-3b74-7bfc-b34f-48b5428d4875
```

`SysUserCommandServiceImpl`：
```java
@Value("${app.security.admin-user-id}")
private String adminUserId;
```

`deleteUser` 中的硬编码 UUID 改为 `UUID.fromString(adminUserId)`。

### 2.6 状态枚举

新增 `scm-system/.../domain/enums/UserStatus.java`：
```java
public enum UserStatus {
    INACTIVE(0), ACTIVE(1), LOCKED(2);
    private final int code;
    public static UserStatus fromCode(Integer c) { ... }
}
```

`userStatusName` 私有方法删除；调用处改为 `UserStatus.fromCode(user.getStatus())`。

### 2.7 `SecureRandom` 单例

`UserRoleCommandServiceImpl` 中如有随机需求，复用同一 `SecureRandom`。当前只有 `generateRandomPassword`（在 `SysUserCommandServiceImpl`）使用：
- 改为 `private static final SecureRandom RANDOM = new SecureRandom();`

### 2.8 注解补齐

所有实现类的接口方法补 `@Override`：`listUsers`, `getUserById`, `getUserInfo`, `addUser`, `updateUser`, `deleteUser`, `updateLastLogin`, `grantTemporaryRoles`, `extendTemporaryRole`, `terminateTemporaryRole`, `getUserTemporaryRoles`, `canAccessDept`, `getUserDataScope`, `getUserStatistics`。

## 3. 依赖关系

```
SysUserQueryServiceImpl
  ├── SysUserMapper
  ├── UserCrossDatabaseQueryService
  ├── RoleCrossDatabaseQueryService
  ├── DeptCrossDatabaseQueryService
  ├── PermissionCrossDatabaseQueryService

SysUserCommandServiceImpl
  ├── SysUserMapper
  ├── UserCrossDatabaseQueryService     (grantRoles 时取角色等级)
  ├── RoleCrossDatabaseQueryService
  ├── UserRoleCrossDatabaseCommandService (角色授予)
  ├── PasswordEncoder
  ├── DataSyncEventPublisher
  ├── PermissionChecker
  ├── StatusValidator
  └── UserCommandTemplate

UserRoleCommandServiceImpl
  ├── UserCrossDatabaseQueryService      (hasTemporaryRole 等存在性校验)
  └── UserRoleCrossDatabaseCommandService
```

## 4. 文件清单

### 新增
- `scm-system/service/src/main/java/com/scmcloud/system/service/query/ISysUserQueryService.java`
- `scm-system/service/src/main/java/com/scmcloud/system/service/query/SysUserQueryServiceImpl.java`
- `scm-system/service/src/main/java/com/scmcloud/system/service/command/ISysUserCommandService.java`
- `scm-system/service/src/main/java/com/scmcloud/system/service/command/SysUserCommandServiceImpl.java`
- `scm-system/service/src/main/java/com/scmcloud/system/service/command/IUserRoleCommandService.java`
- `scm-system/service/src/main/java/com/scmcloud/system/service/command/UserRoleCommandServiceImpl.java`
- `scm-system/service/src/main/java/com/scmcloud/system/service/command/UserCommandTemplate.java`
- `scm-system/service/src/main/java/com/scmcloud/system/domain/enums/UserStatus.java`

### 修改
- `scm-common/.../response/ResultCode.java`：新增 `PASSWORD_INCORRECT_OLD`, `PASSWORD_SAME_AS_OLD`
- 所有 `ISysUserService` 调用方（controller / Dubbo service / 其它 service）：按方法分类改为注入 `ISysUserQueryService` 或 `ISysUserCommandService`
- `application.yml` / `application-*.yml`：新增 `app.security.admin-user-id`

### 删除
- `scm-system/service/src/main/java/com/scmcloud/system/service/ISysUserService.java`
- `scm-system/service/src/main/java/com/scmcloud/system/service/Impl/SysUserServiceImpl.java`

## 5. 验证步骤

1. `grep -r "ISysUserService\|SysUserServiceImpl" scm-system/ scm-auth/ scm-gateway/ scm-common/` 返回 0（确认无残留引用）
2. `mvn clean install -pl scm-system/service -am -f com.scm.parent/pom.xml` 编译通过
3. `mvn test -pl scm-system/service -f com.scm.parent/pom.xml` 既有测试全过（`UserCrossDatabaseQueryServiceTest` 等）
4. 手动核对每个原方法 1:1 落到新接口上，参数/返回值/异常类型不变

## 6. 风险与回滚

- **风险**：调用方遗漏导致编译失败 → 通过 grep + 编译一次性捕获
- **回滚**：`git revert` 整个 commit 即可，外部调用方零运行时依赖旧类

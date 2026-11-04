# SCM Platform 多租户完整指南

> **文档类型**: 多租户架构设计与实施指南
> **版本**: v2.0
> **最后更新**: 2025-12-26
> **维护者**: SCM Platform Team

本文档提供SCM平台多租户SaaS架构的完整设计思想、实施方案和最佳实践。

---

## 📋 目录

1. [概述](#1-概述)
2. [设计原则](#2-设计原则)
3. [核心表结构与实体改造](#3-核心表结构与实体改造)
4. [多租户实现](#4-多租户实现)
5. [数据权限设计](#5-数据权限设计)
6. [应用层集成](#6-应用层集成)
7. [迁移实施](#7-迁移实施)
8. [最佳实践](#8-最佳实践)
9. [常见问题](#9-常见问题)

---

## 1. 概述

### 1.1 改造目标

将scm-system模块从单租户架构升级为支持多租户SaaS平台的架构：

- **租户隔离**：不同租户的用户、角色、权限完全隔离
- **平台权限共享**：平台级基础权限可被所有租户复用
- **灵活的数据权限**：支持部门、用户、自定义SQL等多种数据权限策略
- **高性能查询**：优化权限查询，避免多表JOIN性能问题
- **易于扩展**：支持租户自定义角色和权限

### 1.2 改造范围

**改造时间**：2025-01-24
**改造模块**：scm-system
**涉及表数量**：8个核心表

| 实体类 | 新增字段 | 说明 |
|-------|---------|------|
| SysUser | tenant_id, user_type, data_scope | 用户表，支持平台/租户用户区分 |
| SysRole | tenant_id, role_type, role_category, custom_dept_ids | 角色表，支持平台/租户角色区分 |
| SysPermission | tenant_id, permission_scope | 权限表，支持平台/租户权限区分 |
| SysDept | tenant_id, dept_level, dept_path | 部门表，租户内部组织架构 |
| SysUserRole | tenant_id | 用户角色关联表 |
| SysRolePermission | tenant_id | 角色权限关联表 |
| SysDataPermissionRule | tenant_id | 数据权限规则表 |
| SysRoleDataRule | tenant_id | 角色数据权限关联表 |

---

## 2. 设计原则

### 2.1 多租户隔离策略

**共享数据库，共享表，行级隔离**（当前方案）

```sql
-- 所有业务表都包含 tenant_id 字段
CREATE TABLE sys_user (
    id UUID PRIMARY KEY,
    tenant_id UUID,  -- 租户ID（NULL=平台管理员）
    username VARCHAR(50) NOT NULL,
    -- ...
);

-- 租户隔离索引
CREATE INDEX idx_user_tenant ON sys_user(tenant_id) WHERE NOT deleted;
```

### 2.2 平台资源 vs 租户资源

**平台资源**（tenant_id = NULL）：
- 所有租户共享的基础功能
- 由平台统一维护，租户无法修改
- 示例：订单管理菜单、商品创建按钮、基础API接口

**租户资源**（tenant_id ≠ NULL）：
- 租户根据业务需要自定义
- 仅在租户内可见和使用
- 示例：租户特有的审批流程权限、自定义报表权限

```sql
-- 平台角色（所有租户共享）
INSERT INTO sys_role (id, tenant_id, role_code, role_type)
VALUES (uuid_generate_v4(), NULL, 'SUPER_ADMIN', 'PLATFORM_ROLE');

-- 租户角色（仅租户自己可见）
INSERT INTO sys_role (id, tenant_id, role_code, role_type)
VALUES (uuid_generate_v4(), '租户UUID', 'DEPT_MANAGER', 'TENANT_ROLE');
```

---

## 3. 核心表结构与实体改造

### 3.1 SysUser（用户表）

#### 新增字段

```java
@Schema(description = "租户ID（NULL=平台管理员）")
@TableField("tenant_id")
private UUID tenantId;

@Schema(description = "用户类型:PLATFORM_ADMIN-平台管理员,TENANT_ADMIN-租户管理员,TENANT_USER-租户用户")
@TableField("user_type")
private String userType;

@Schema(description = "数据权限范围:ALL-全部,DEPT-本部门,DEPT_AND_SUB-本部门及下级,SELF-仅本人,CUSTOM-自定义")
@TableField("data_scope")
private String dataScope;
```

#### 用户类型（user_type）

| 类型 | 说明 | tenant_id | 权限范围 |
|------|------|-----------|----------|
| `PLATFORM_ADMIN` | 平台管理员 | NULL | 可管理所有租户，访问平台后台 |
| `TENANT_ADMIN` | 租户管理员 | 非NULL | 可管理租户内所有资源和用户 |
| `TENANT_USER` | 租户普通用户 | 非NULL | 根据角色权限访问租户资源 |

#### 数据权限范围（data_scope）

| 范围 | 说明 | 应用场景 |
|------|------|----------|
| `ALL` | 全部数据 | 租户管理员，查看租户内所有数据 |
| `DEPT` | 本部门数据 | 部门经理，只查看本部门的订单/库存 |
| `DEPT_AND_SUB` | 本部门及下级部门 | 大区经理，查看本区域所有部门数据 |
| `SELF` | 仅本人数据 | 普通员工，只查看自己创建的订单 |
| `CUSTOM` | 自定义规则 | 特殊角色，通过 `sys_data_permission_rule` 定义 |

#### 使用示例

```java
// 创建租户管理员
SysUser tenantAdmin = new SysUser();
tenantAdmin.setTenantId(tenantId);
tenantAdmin.setUsername("tenant_admin");
tenantAdmin.setUserType("TENANT_ADMIN");
tenantAdmin.setDataScope("ALL");

// 创建平台管理员
SysUser platformAdmin = new SysUser();
platformAdmin.setTenantId(null); // 平台管理员 tenant_id 为 NULL
platformAdmin.setUsername("platform_admin");
platformAdmin.setUserType("PLATFORM_ADMIN");
platformAdmin.setDataScope("ALL");
```

### 3.2 SysRole（角色表）

#### 新增字段

```java
@Schema(description = "租户ID（NULL=平台角色）")
@TableField("tenant_id")
private UUID tenantId;

@Schema(description = "角色类型:PLATFORM_ROLE-平台角色,TENANT_ROLE-租户角色")
@TableField("role_type")
private String roleType;

@Schema(description = "角色分类:BUSINESS-业务角色,FUNCTIONAL-职能角色,CUSTOM-自定义角色")
@TableField("role_category")
private String roleCategory;

@Schema(description = "自定义部门ID列表(当data_scope=CUSTOM时使用)")
@TableField(value = "custom_dept_ids", typeHandler = JacksonTypeHandler.class)
private List<UUID> customDeptIds;
```

#### 角色类型（role_type）

| 类型 | 说明 | tenant_id | 使用场景 |
|------|------|-----------|----------|
| `PLATFORM_ROLE` | 平台角色 | NULL | 平台预定义角色，跨租户使用（如：超级管理员） |
| `TENANT_ROLE` | 租户角色 | 非NULL | 租户自定义角色，仅在租户内使用 |

#### 角色分类（role_category）

- `BUSINESS`：业务角色（如：采购员、仓管员、销售员）
- `FUNCTIONAL`：职能角色（如：财务、人事、IT）
- `CUSTOM`：自定义角色（租户根据业务自定义）

#### 预定义角色示例

**平台角色**（tenant_id = NULL）：

| 角色编码 | 角色名称 | 数据权限 | 说明 |
|---------|---------|----------|------|
| `SUPER_ADMIN` | 超级管理员 | ALL | 平台最高权限，可管理所有租户 |
| `PLATFORM_OPS` | 平台运维 | ALL | 负责平台运维、监控、配置 |

**租户角色**（tenant_id = 租户UUID）：

| 角色编码 | 角色名称 | 数据权限 | 说明 |
|---------|---------|----------|------|
| `TENANT_ADMIN` | 租户管理员 | ALL | 租户内最高权限 |
| `WAREHOUSE_MANAGER` | 仓库经理 | DEPT | 管理本仓库的出入库、库存 |
| `PURCHASER` | 采购员 | SELF | 创建和管理自己的采购订单 |
| `SALES` | 销售员 | DEPT_AND_SUB | 查看本部门及下级销售数据 |
| `FINANCE` | 财务 | ALL | 查看租户内所有财务数据 |

### 3.3 SysPermission（权限表）

#### 新增字段

```java
@Schema(description = "租户ID（NULL=平台级权限，所有租户共享）")
@TableField("tenant_id")
private UUID tenantId;

@Schema(description = "权限归属:PLATFORM-平台级权限(所有租户共享),TENANT-租户级权限(租户自定义)")
@TableField("permission_scope")
private String permissionScope;
```

#### 权限类型（permission_type）

| 类型 | 说明 | 示例 |
|------|------|------|
| `MENU` | 菜单权限 | 订单管理菜单、商品管理菜单 |
| `BUTTON` | 按钮权限 | 创建订单按钮、删除商品按钮 |
| `API` | 接口权限 | `/api/orders/*` 接口访问权限 |
| `DATA` | 数据权限 | 只能查看本部门订单 |

#### 平台级权限示例

```sql
-- 菜单权限
INSERT INTO sys_permission (tenant_id, permission_code, permission_name, permission_type, permission_scope) VALUES
(NULL, 'MENU_DASHBOARD', '仪表盘', 'MENU', 'PLATFORM'),
(NULL, 'MENU_ORDER', '订单管理', 'MENU', 'PLATFORM'),
(NULL, 'MENU_PRODUCT', '商品管理', 'MENU', 'PLATFORM'),
(NULL, 'MENU_INVENTORY', '库存管理', 'MENU', 'PLATFORM');

-- 按钮权限
INSERT INTO sys_permission (tenant_id, permission_code, permission_name, permission_type, permission_scope) VALUES
(NULL, 'BTN_ORDER_CREATE', '创建订单', 'BUTTON', 'PLATFORM'),
(NULL, 'BTN_ORDER_UPDATE', '编辑订单', 'BUTTON', 'PLATFORM'),
(NULL, 'BTN_ORDER_DELETE', '删除订单', 'BUTTON', 'PLATFORM');

-- API权限
INSERT INTO sys_permission (tenant_id, permission_code, permission_name, permission_type, permission_scope, api_path, http_method) VALUES
(NULL, 'API_ORDER_QUERY', '查询订单接口', 'API', 'PLATFORM', '/api/orders/**', 'GET'),
(NULL, 'API_ORDER_CREATE', '创建订单接口', 'API', 'PLATFORM', '/api/orders', 'POST');
```

### 3.4 SysDept（部门表）

#### 新增字段

```java
@Schema(description = "租户ID")
@TableField("tenant_id")
private UUID tenantId;

@Schema(description = "部门层级（1=一级部门）")
@TableField("dept_level")
private Integer deptLevel;

@Schema(description = "部门路径（用于快速查询上下级部门，如 /uuid1/uuid2/uuid3）")
@TableField("dept_path")
private String deptPath;
```

#### 部门路径（dept_path）

`dept_path` 用于快速查询部门的上下级关系：

**示例：**

| dept_id | dept_name | parent_id | dept_path | dept_level |
|---------|-----------|-----------|-----------|------------|
| uuid-1 | 总部 | NULL | /uuid-1 | 1 |
| uuid-2 | 华东大区 | uuid-1 | /uuid-1/uuid-2 | 2 |
| uuid-3 | 上海分公司 | uuid-2 | /uuid-1/uuid-2/uuid-3 | 3 |
| uuid-4 | 上海仓库 | uuid-3 | /uuid-1/uuid-2/uuid-3/uuid-4 | 4 |

**查询示例：**

```sql
-- 查询华东大区及其所有下级部门
SELECT * FROM sys_department
WHERE tenant_id = '<tenant-id>'
  AND dept_path LIKE '/uuid-1/uuid-2%';

-- 查询上海分公司的直接下级部门
SELECT * FROM sys_department
WHERE tenant_id = '<tenant-id>'
  AND parent_id = 'uuid-3';
```

---

## 4. 多租户实现

### 4.1 TenantInterceptor 自动注入

`TenantInterceptor` 会自动为所有查询注入 `tenant_id` 条件：

```java
// 原始 SQL
SELECT * FROM sys_user WHERE username = ?

// TenantInterceptor 自动注入后
SELECT * FROM sys_user WHERE username = ? AND tenant_id = '<current-tenant-id>'
```

**排除的表**（不会被注入 tenant_id）：

- tenant
- tenant_package
- tenant_subscription
- tenant_resource_quota
- tenant_config
- tenant_feature
- tenant_operation_log

### 4.2 AuditMetaObjectHandler 自动填充

自动填充审计字段：

```java
// INSERT 时自动填充
- id (UUIDv7)
- tenant_id (从 TenantContextHolder 获取)
- created_at
- created_by
- updated_at
- deleted (默认 false)

// UPDATE 时自动填充
- updated_at
- updated_by
```

### 4.3 TenantContextHolder 租户上下文

使用 ThreadLocal 存储当前请求的租户ID：

```java
// 获取当前租户ID
UUID tenantId = TenantContextHolder.getTenantId();

// 在指定租户上下文中执行
TenantContextHolder.executeInTenantContext(tenantId, () -> {
    // 这里的代码会在指定租户上下文中执行
    return userService.createUser(user);
});
```

### 4.4 JWT 租户绑定（P0-1）

Access Token / Refresh Token 现在都写入 `tenantId`（必填）与 `tenantCode`（可空，用于展示与排障）：

- 签发：`JwtUtils#generateAccessToken(..., UUID tenantId, String tenantCode)`；`tenantId` 为空时直接抛
  `UnauthorizedException("invalid_token_tenant_missing")`，不存在“无租户 token”。
- 校验：`JwtAuthenticationFilter` 在验签后调用 `JwtUtils#validateTenantBinding(token, 请求头租户)`：
  - Token 无 `tenantId` 声明 → 401，响应体 `{"code":401,"error":"invalid_token_tenant_missing",...}`
    （升级前的旧 token 一律拒绝，不静默降级）；
  - 请求头缺租户或与 Token 的 `tenantId` 不一致 → 401，`error = invalid_token_tenant_mismatch`。
- 刷新：`refreshToken` / `refreshTokenWithRotation` 从旧 refresh token 读取租户声明后再签发，旧 refresh token
  无租户声明时同样 401。
- 前端收到上述 code 后应清理本地会话并跳转登录页。

> ⚠️ **上线影响**：本变更上线后，所有在部署前签发的 token 都会被判定为
> `invalid_token_tenant_missing` 并强制重新登录（全站重登）。必须在维护窗口部署，
> 并在发布说明中注明；无需清理 Redis 中的旧 token（它们已不可用，按 TTL 自然过期）。

---

## 5. 数据权限设计

### 5.1 数据权限规则表

```sql
CREATE TABLE sys_data_permission_rule (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    rule_name VARCHAR(100) NOT NULL,
    rule_code VARCHAR(50) NOT NULL,
    resource_type VARCHAR(50) NOT NULL,      -- 资源类型（如 ORDER, PRODUCT, INVENTORY）
    rule_type VARCHAR(20) NOT NULL CHECK (rule_type IN ('DEPT', 'DEPT_AND_SUB', 'USER', 'CUSTOM_SQL')),
    rule_config JSONB DEFAULT '{}',          -- 规则配置
    custom_sql TEXT,                         -- 自定义SQL条件
    status VARCHAR(20) DEFAULT 'ACTIVE',
    priority INT DEFAULT 0
);
```

### 5.2 规则类型

| 规则类型 | 说明 | 示例 |
|---------|------|------|
| `DEPT` | 本部门数据 | 只能查看本部门的订单 |
| `DEPT_AND_SUB` | 本部门及下级数据 | 查看本部门及所有下级部门的订单 |
| `USER` | 指定用户数据 | 只能查看指定用户创建的订单 |
| `CUSTOM_SQL` | 自定义SQL条件 | 复杂业务规则，通过SQL表达式定义 |

### 5.3 规则配置示例

**部门规则**：
```json
{
  "deptIds": ["uuid-1", "uuid-2"],
  "includeSubDepts": true
}
```

**用户规则**：
```json
{
  "userIds": ["uuid-3", "uuid-4"],
  "fieldName": "created_by"
}
```

**自定义SQL规则**：
```sql
custom_sql = "created_by = #{currentUserId} OR assigned_to = #{currentUserId}"
```

### 5.4 数据权限应用流程

```
┌─────────────┐
│ 用户登录     │
└──────┬──────┘
       │
       v
┌─────────────────────────────────┐
│ 查询用户角色和数据权限           │
│ (v_user_permissions 视图)       │
└──────────┬──────────────────────┘
           │
           v
┌─────────────────────────────────┐
│ 解析数据权限规则                 │
│ - data_scope (ALL, DEPT, SELF)  │
│ - sys_data_permission_rule 表   │
└──────────┬──────────────────────┘
           │
           v
┌─────────────────────────────────┐
│ 构建SQL条件                      │
│ - MyBatis 拦截器自动注入         │
│ - WHERE tenant_id = ?           │
│   AND (数据权限条件)             │
└──────────┬──────────────────────┘
           │
           v
┌─────────────────────────────────┐
│ 执行查询，返回权限范围内的数据    │
└─────────────────────────────────┘
```

### 5.5 权限查询优化视图

创建视图用于快速查询用户权限：

```sql
CREATE OR REPLACE VIEW v_user_permissions AS
SELECT
    u.id AS user_id,
    u.tenant_id,
    u.username,
    u.user_type,
    u.department_id,
    u.data_scope AS user_data_scope,
    r.id AS role_id,
    r.role_code,
    r.role_name,
    r.data_scope AS role_data_scope,
    r.custom_dept_ids,
    p.id AS permission_id,
    p.permission_code,
    p.permission_name,
    p.permission_type,
    p.api_path,
    p.http_method
FROM sys_user u
JOIN sys_user_role ur ON u.id = ur.user_id AND u.tenant_id = ur.tenant_id
JOIN sys_role r ON ur.role_id = r.id AND ur.tenant_id = r.tenant_id
JOIN sys_role_permission rp ON r.id = rp.role_id AND r.tenant_id = rp.tenant_id
JOIN sys_permission p ON rp.permission_id = p.id
WHERE NOT u.deleted AND NOT r.deleted AND NOT p.deleted
  AND u.status = 'ACTIVE' AND r.status = 'ACTIVE';
```

**查询示例**：

```sql
-- 查询用户所有菜单权限
SELECT DISTINCT permission_code, permission_name, parent_id, sort_order
FROM v_user_permissions
WHERE user_id = '<user-id>'
  AND tenant_id = '<tenant-id>'
  AND permission_type = 'MENU'
ORDER BY sort_order;

-- 查询用户是否有某个按钮权限
SELECT COUNT(*) > 0 AS has_permission
FROM v_user_permissions
WHERE user_id = '<user-id>'
  AND tenant_id = '<tenant-id>'
  AND permission_code = 'BTN_ORDER_DELETE';
```

---

## 6. 应用层集成

### 6.1 权限检查拦截器

```java
@Component
public class PermissionInterceptor implements HandlerInterceptor {

    @Autowired
    private PermissionService permissionService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 1. 获取当前用户和租户
        UUID userId = getCurrentUserId();
        UUID tenantId = TenantContextHolder.getTenantId();

        // 2. 获取请求的API路径和方法
        String apiPath = request.getRequestURI();
        String httpMethod = request.getMethod();

        // 3. 检查用户是否有该API的访问权限
        boolean hasPermission = permissionService.checkApiPermission(
            userId, tenantId, apiPath, httpMethod
        );

        if (!hasPermission) {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            return false;
        }

        return true;
    }
}
```

### 6.2 前端按钮权限指令

**Vue 3 示例：**

```vue
<template>
  <button v-permission="'BTN_ORDER_DELETE'" @click="deleteOrder">
    删除订单
  </button>
</template>

<script setup>
import { usePermission } from '@/hooks/usePermission';

const { hasPermission } = usePermission();

// 自定义指令
app.directive('permission', {
  mounted(el, binding) {
    const permissionCode = binding.value;
    if (!hasPermission(permissionCode)) {
      el.style.display = 'none';
    }
  }
});
</script>
```

### 6.3 数据权限注解

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DataPermission {
    String resourceType();              // 资源类型（如 ORDER, PRODUCT）
    String[] fields() default {};       // 需要过滤的字段（如 created_by, department_id）
}

// 使用示例
@Service
public class OrderService {

    @DataPermission(resourceType = "ORDER", fields = {"created_by", "department_id"})
    public List<Order> listOrders(OrderQueryDTO query) {
        // MyBatis拦截器会自动注入数据权限条件
        // WHERE tenant_id = ? AND (数据权限SQL)
        return orderMapper.selectList(query);
    }
}
```

---

## 7. 迁移实施

### 7.1 数据库迁移

```bash
# 1. 备份数据库
pg_dump -U postgres scm_platform > backup_before_migration_$(date +%Y%m%d).sql

# 2. 执行迁移脚本
psql -U postgres -d scm_platform -f scripts/db/migrations/004_transform_permission_system_multi_tenant.sql

# 3. 验证表结构
psql -U postgres -d scm_platform -c "\d+ sys_user"
psql -U postgres -d scm_platform -c "\d+ sys_role"
psql -U postgres -d scm_platform -c "\d+ sys_permission"
```

### 7.2 数据迁移

```sql
-- 1. 为现有用户分配默认租户
UPDATE sys_user
SET tenant_id = '<default-tenant-uuid>',
    user_type = 'TENANT_USER'
WHERE tenant_id IS NULL;

-- 2. 标记平台管理员
UPDATE sys_user
SET tenant_id = NULL,
    user_type = 'PLATFORM_ADMIN'
WHERE username IN ('admin', 'root');

-- 3. 同步关联表的 tenant_id（由触发器自动完成，验证即可）
SELECT ur.id, u.tenant_id, ur.tenant_id
FROM sys_user_role ur
JOIN sys_user u ON ur.user_id = u.id
WHERE ur.tenant_id IS DISTINCT FROM u.tenant_id;
```

### 7.3 应用层改造清单

**已完成**：
- [x] SysUser 实体类多租户字段添加
- [x] SysRole 实体类多租户字段添加
- [x] SysPermission 实体类多租户字段添加
- [x] SysDept 实体类多租户字段添加
- [x] 关联表多租户字段添加
- [x] 数据权限表多租户字段添加

**待完成**：
- [ ] Service 层改造（支持多租户查询）
- [ ] Mapper XML 改造（根据需要）
- [ ] Controller 层改造（租户ID验证）
- [ ] 单元测试编写
- [ ] 集成测试编写

---

## 8. 最佳实践

### 8.1 平台权限 vs 租户权限

**平台权限（tenant_id = NULL）**：
- 所有租户共享的基础功能权限
- 由平台统一维护，租户无法修改
- 示例：订单管理菜单、商品创建按钮、基础API接口

**租户权限（tenant_id ≠ NULL）**：
- 租户根据业务需要自定义的权限
- 仅在租户内可见和使用
- 示例：租户特有的审批流程权限、自定义报表权限

### 8.2 权限粒度建议

- **菜单权限**：控制用户可以看到哪些菜单
- **按钮权限**：控制用户可以执行哪些操作
- **API权限**：防止绕过前端直接调用API
- **数据权限**：控制用户可以访问哪些数据范围

### 8.3 性能优化

1. **缓存用户权限**：
   - 用户登录后，将权限列表缓存到 Redis
   - 过期时间：30分钟
   - 权限变更时主动失效缓存

2. **使用视图简化查询**：
   - 创建 `v_user_permissions` 视图
   - 避免每次都 JOIN 多张表

3. **索引优化**：
   - 所有关联查询的字段都建立索引
   - 使用 `(tenant_id, xxx)` 复合索引

```sql
-- 优化用户名查询
CREATE UNIQUE INDEX idx_user_username ON sys_user(tenant_id, username) WHERE NOT deleted;

-- 优化角色编码查询
CREATE UNIQUE INDEX idx_role_code ON sys_role(tenant_id, role_code) WHERE NOT deleted;
```

### 8.4 安全建议

1. **最小权限原则**：
   - 默认不授予任何权限
   - 根据角色逐步授予必要权限

2. **租户隔离**：
   - 所有查询必须带上 `tenant_id` 过滤
   - 使用 MyBatis 拦截器自动注入

3. **审计日志**：
   - 记录所有权限变更操作
   - 记录敏感操作（如删除、导出）

---

## 9. 常见问题

### 9.1 必须设置租户上下文

**问题**：所有业务操作前必须设置租户上下文，否则会抛出异常

**解决方案**：

```java
// HTTP 请求必须携带 X-Tenant-Id 头
X-Tenant-Id: 123e4567-e89b-12d3-a456-426614174000

// 或在代码中手动设置
TenantContextHolder.setTenantId(tenantId);
```

### 9.2 平台管理员切换租户

**问题**：平台管理员需要临时切换到特定租户上下文

**解决方案**：

```java
// 方法1：使用回调
UUID result = TenantContextHolder.executeInTenantContext(targetTenantId, () -> {
    return userService.createUser(user);
});

// 方法2：手动设置
UUID originalTenantId = TenantContextHolder.getTenantId();
try {
    TenantContextHolder.setTenantId(targetTenantId);
    userService.createUser(user);
} finally {
    TenantContextHolder.setTenantId(originalTenantId);
}
```

### 9.3 配置仓库经理的数据权限

**示例**：

```java
// 1. 创建角色
SysRole role = new SysRole();
role.setTenantId(tenantId);
role.setRoleCode("WAREHOUSE_MANAGER");
role.setRoleName("仓库经理");
role.setDataScope(3); // 3=本部门数据

// 2. 分配用户到角色
SysUserRole userRole = new SysUserRole();
userRole.setUserId(userId);
userRole.setRoleId(role.getId());
// tenant_id 由触发器自动填充

// 3. 查询时会自动过滤（由数据权限拦截器实现）
// SELECT * FROM ord_order
// WHERE tenant_id = ? AND created_by IN (本部门用户ID列表)
```

---

## 附录

### A. 完整 ER 图

```
┌─────────────┐       ┌─────────────┐       ┌─────────────┐
│  sys_user   │──────▶│sys_user_role│◀──────│  sys_role   │
└─────────────┘       └─────────────┘       └─────────────┘
       │                                             │
       │                                             │
       │                                             ▼
       │                                    ┌──────────────────┐
       │                                    │sys_role_permission│
       │                                    └──────────────────┘
       │                                             │
       ▼                                             ▼
┌─────────────┐                              ┌─────────────┐
│sys_department│                             │sys_permission│
└─────────────┘                              └─────────────┘
       │
       │
       ▼
┌──────────────────────┐       ┌──────────────────────────┐
│sys_data_permission   │──────▶│sys_role_data_permission  │
│      _rule           │       └──────────────────────────┘
└──────────────────────┘
```

### B. 相关文档

- 架构设计: [../design/ARCHITECTURE.md](../design/ARCHITECTURE.md)
- 数据库设计: [../technical/DATABASE_DESIGN.md](../technical/DATABASE_DESIGN.md)
- 开发规范: [../development/DEVELOPMENT_STANDARDS.md](../development/DEVELOPMENT_STANDARDS.md)
- 迁移脚本: [../../scripts/db/migrations/004_transform_permission_system_multi_tenant.sql](../../scripts/db/migrations/004_transform_permission_system_multi_tenant.sql)

---

**文档版本**：v2.0
**最后更新**：2025-12-26
**作者**：SCM Platform Team
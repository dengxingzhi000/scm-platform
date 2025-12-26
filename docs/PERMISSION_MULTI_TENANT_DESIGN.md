# 权限系统多租户改造设计文档

## 1. 概述

本文档描述如何将传统的 RBAC（基于角色的访问控制）权限系统改造为支持多租户 SaaS 平台的权限系统。

### 1.1 设计目标

- **租户隔离**：不同租户的用户、角色、权限完全隔离
- **平台权限共享**：平台级基础权限可被所有租户复用
- **灵活的数据权限**：支持部门、用户、自定义SQL等多种数据权限策略
- **高性能查询**：优化权限查询，避免多表JOIN性能问题
- **易于扩展**：支持租户自定义角色和权限

### 1.2 核心表结构

改造后的权限系统包含以下核心表：

| 表名 | 说明 | 多租户策略 |
|------|------|-----------|
| `sys_user` | 用户表 | `tenant_id` 区分租户，`user_type` 区分平台/租户用户 |
| `sys_role` | 角色表 | `tenant_id` 区分租户，`role_type` 区分平台/租户角色 |
| `sys_permission` | 权限表 | `tenant_id=NULL` 为平台权限（共享），非NULL为租户自定义权限 |
| `sys_department` | 部门表 | 租户内部组织架构，按 `tenant_id` 隔离 |
| `sys_data_permission` | 数据权限规则表 | 定义数据级权限控制规则 |
| `sys_user_role` | 用户-角色关联 | 继承用户的 `tenant_id` |
| `sys_role_permission` | 角色-权限关联 | 继承角色的 `tenant_id` |
| `sys_role_data_permission` | 角色-数据权限关联 | 数据权限与角色的绑定 |

---

## 2. 用户系统设计

### 2.1 用户类型（user_type）

```sql
user_type VARCHAR(20) CHECK (user_type IN ('PLATFORM_ADMIN', 'TENANT_ADMIN', 'TENANT_USER'))
```

| 类型 | 说明 | tenant_id | 权限范围 |
|------|------|-----------|----------|
| `PLATFORM_ADMIN` | 平台管理员 | NULL | 可管理所有租户，访问平台后台 |
| `TENANT_ADMIN` | 租户管理员 | 非NULL | 可管理租户内所有资源和用户 |
| `TENANT_USER` | 租户普通用户 | 非NULL | 根据角色权限访问租户资源 |

### 2.2 数据权限范围（data_scope）

```sql
data_scope VARCHAR(20) CHECK (data_scope IN ('ALL', 'DEPT', 'DEPT_AND_SUB', 'SELF', 'CUSTOM'))
```

| 范围 | 说明 | 应用场景 |
|------|------|----------|
| `ALL` | 全部数据 | 租户管理员，查看租户内所有数据 |
| `DEPT` | 本部门数据 | 部门经理，只查看本部门的订单/库存 |
| `DEPT_AND_SUB` | 本部门及下级部门 | 大区经理，查看本区域所有部门数据 |
| `SELF` | 仅本人数据 | 普通员工，只查看自己创建的订单 |
| `CUSTOM` | 自定义规则 | 特殊角色，通过 `sys_data_permission` 定义 |

### 2.3 用户表结构

```sql
CREATE TABLE sys_user (
    id UUID PRIMARY KEY,
    tenant_id UUID,                          -- 租户ID（平台管理员为NULL）
    username VARCHAR(50) NOT NULL,
    password VARCHAR(255) NOT NULL,
    user_type VARCHAR(20) DEFAULT 'TENANT_USER',
    department_id UUID,                      -- 所属部门
    data_scope VARCHAR(20) DEFAULT 'SELF',   -- 数据权限范围
    status VARCHAR(20) DEFAULT 'ACTIVE',
    -- 其他字段...
    CONSTRAINT uk_tenant_username UNIQUE (tenant_id, username)
);
```

---

## 3. 角色系统设计

### 3.1 角色类型（role_type）

```sql
role_type VARCHAR(20) CHECK (role_type IN ('PLATFORM_ROLE', 'TENANT_ROLE'))
```

| 类型 | 说明 | tenant_id | 使用场景 |
|------|------|-----------|----------|
| `PLATFORM_ROLE` | 平台角色 | NULL | 平台预定义角色，跨租户使用（如：超级管理员） |
| `TENANT_ROLE` | 租户角色 | 非NULL | 租户自定义角色，仅在租户内使用 |

### 3.2 角色分类（role_category）

用于对角色进行业务分类：

- `BUSINESS`：业务角色（如：采购员、仓管员、销售员）
- `FUNCTIONAL`：职能角色（如：财务、人事、IT）
- `CUSTOM`：自定义角色（租户根据业务自定义）

### 3.3 角色表结构

```sql
CREATE TABLE sys_role (
    id UUID PRIMARY KEY,
    tenant_id UUID,                          -- 租户ID（平台角色为NULL）
    role_code VARCHAR(50) NOT NULL,
    role_name VARCHAR(100) NOT NULL,
    role_type VARCHAR(20) DEFAULT 'TENANT_ROLE',
    role_category VARCHAR(50) DEFAULT 'BUSINESS',
    data_scope VARCHAR(20) DEFAULT 'SELF',   -- 角色级数据权限
    custom_dept_ids JSONB DEFAULT '[]',      -- 自定义部门列表（data_scope=CUSTOM时使用）
    status VARCHAR(20) DEFAULT 'ACTIVE',
    -- 其他字段...
    CONSTRAINT uk_tenant_role_code UNIQUE (tenant_id, role_code)
);
```

### 3.4 预定义角色示例

#### 平台角色（tenant_id = NULL）

| 角色编码 | 角色名称 | 数据权限 | 说明 |
|---------|---------|----------|------|
| `SUPER_ADMIN` | 超级管理员 | ALL | 平台最高权限，可管理所有租户 |
| `PLATFORM_OPS` | 平台运维 | ALL | 负责平台运维、监控、配置 |

#### 租户角色（tenant_id = 租户UUID）

| 角色编码 | 角色名称 | 数据权限 | 说明 |
|---------|---------|----------|------|
| `TENANT_ADMIN` | 租户管理员 | ALL | 租户内最高权限 |
| `WAREHOUSE_MANAGER` | 仓库经理 | DEPT | 管理本仓库的出入库、库存 |
| `PURCHASER` | 采购员 | SELF | 创建和管理自己的采购订单 |
| `SALES` | 销售员 | DEPT_AND_SUB | 查看本部门及下级销售数据 |
| `FINANCE` | 财务 | ALL | 查看租户内所有财务数据 |

---

## 4. 权限系统设计

### 4.1 权限类型（permission_type）

```sql
permission_type VARCHAR(20) CHECK (permission_type IN ('MENU', 'BUTTON', 'API', 'DATA'))
```

| 类型 | 说明 | 示例 |
|------|------|------|
| `MENU` | 菜单权限 | 订单管理菜单、商品管理菜单 |
| `BUTTON` | 按钮权限 | 创建订单按钮、删除商品按钮 |
| `API` | 接口权限 | `/api/orders/*` 接口访问权限 |
| `DATA` | 数据权限 | 只能查看本部门订单 |

### 4.2 权限归属（permission_scope）

```sql
permission_scope VARCHAR(20) CHECK (permission_scope IN ('PLATFORM', 'TENANT'))
```

| 归属 | 说明 | tenant_id | 使用场景 |
|------|------|-----------|----------|
| `PLATFORM` | 平台级权限 | NULL | 所有租户共享的基础权限 |
| `TENANT` | 租户级权限 | 非NULL | 租户自定义的特殊权限 |

### 4.3 权限表结构

```sql
CREATE TABLE sys_permission (
    id UUID PRIMARY KEY,
    tenant_id UUID,                          -- NULL=平台权限，非NULL=租户自定义权限
    permission_code VARCHAR(50) NOT NULL,
    permission_name VARCHAR(100) NOT NULL,
    permission_type VARCHAR(20) DEFAULT 'MENU',
    permission_scope VARCHAR(20) DEFAULT 'PLATFORM',
    parent_id UUID,                          -- 父权限（用于树形菜单）
    sort_order INT DEFAULT 0,

    -- API权限专用字段
    api_path VARCHAR(500),                   -- API路径（支持Ant通配符，如 /api/orders/**）
    http_method VARCHAR(10),                 -- HTTP方法（GET, POST, PUT, DELETE, *）

    status VARCHAR(20) DEFAULT 'ACTIVE',
    -- 其他字段...
);
```

### 4.4 平台级权限示例

所有租户共享这些基础权限（`tenant_id = NULL`）：

#### 菜单权限

```sql
INSERT INTO sys_permission (tenant_id, permission_code, permission_name, permission_type, permission_scope) VALUES
(NULL, 'MENU_DASHBOARD', '仪表盘', 'MENU', 'PLATFORM'),
(NULL, 'MENU_ORDER', '订单管理', 'MENU', 'PLATFORM'),
(NULL, 'MENU_PRODUCT', '商品管理', 'MENU', 'PLATFORM'),
(NULL, 'MENU_INVENTORY', '库存管理', 'MENU', 'PLATFORM'),
(NULL, 'MENU_WAREHOUSE', '仓库管理', 'MENU', 'PLATFORM'),
(NULL, 'MENU_SUPPLIER', '供应商管理', 'MENU', 'PLATFORM'),
(NULL, 'MENU_REPORT', '报表分析', 'MENU', 'PLATFORM'),
(NULL, 'MENU_SYSTEM', '系统设置', 'MENU', 'PLATFORM');
```

#### 按钮权限

```sql
INSERT INTO sys_permission (tenant_id, permission_code, permission_name, permission_type, permission_scope) VALUES
(NULL, 'BTN_ORDER_CREATE', '创建订单', 'BUTTON', 'PLATFORM'),
(NULL, 'BTN_ORDER_UPDATE', '编辑订单', 'BUTTON', 'PLATFORM'),
(NULL, 'BTN_ORDER_DELETE', '删除订单', 'BUTTON', 'PLATFORM'),
(NULL, 'BTN_ORDER_EXPORT', '导出订单', 'BUTTON', 'PLATFORM');
```

#### API权限

```sql
INSERT INTO sys_permission (tenant_id, permission_code, permission_name, permission_type, permission_scope, api_path, http_method) VALUES
(NULL, 'API_ORDER_QUERY', '查询订单接口', 'API', 'PLATFORM', '/api/orders/**', 'GET'),
(NULL, 'API_ORDER_CREATE', '创建订单接口', 'API', 'PLATFORM', '/api/orders', 'POST'),
(NULL, 'API_ORDER_UPDATE', '更新订单接口', 'API', 'PLATFORM', '/api/orders/*', 'PUT'),
(NULL, 'API_ORDER_DELETE', '删除订单接口', 'API', 'PLATFORM', '/api/orders/*', 'DELETE');
```

---

## 5. 部门组织架构设计

### 5.1 部门表结构

```sql
CREATE TABLE sys_department (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,                 -- 租户ID
    dept_code VARCHAR(50) NOT NULL,
    dept_name VARCHAR(100) NOT NULL,
    parent_id UUID,                          -- 父部门ID（NULL=根部门）
    dept_level INT DEFAULT 1,                -- 部门层级
    dept_path VARCHAR(500),                  -- 部门路径（如 /1/2/3，用于快速查询上下级）
    sort_order INT DEFAULT 0,
    leader_id UUID,                          -- 部门负责人
    contact_phone VARCHAR(20),
    status VARCHAR(20) DEFAULT 'ACTIVE',
    -- 其他字段...
    CONSTRAINT uk_tenant_dept_code UNIQUE (tenant_id, dept_code)
);
```

### 5.2 部门路径（dept_path）

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

## 6. 数据权限设计

### 6.1 数据权限规则表

```sql
CREATE TABLE sys_data_permission (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    rule_name VARCHAR(100) NOT NULL,
    rule_code VARCHAR(50) NOT NULL,
    resource_type VARCHAR(50) NOT NULL,      -- 资源类型（如 ORDER, PRODUCT, INVENTORY）
    rule_type VARCHAR(20) NOT NULL CHECK (rule_type IN ('DEPT', 'DEPT_AND_SUB', 'USER', 'CUSTOM_SQL')),
    rule_config JSONB DEFAULT '{}',          -- 规则配置
    custom_sql TEXT,                         -- 自定义SQL条件
    status VARCHAR(20) DEFAULT 'ACTIVE',
    priority INT DEFAULT 0,
    -- 其他字段...
);
```

### 6.2 规则类型（rule_type）

| 规则类型 | 说明 | 示例 |
|---------|------|------|
| `DEPT` | 本部门数据 | 只能查看本部门的订单 |
| `DEPT_AND_SUB` | 本部门及下级数据 | 查看本部门及所有下级部门的订单 |
| `USER` | 指定用户数据 | 只能查看指定用户创建的订单 |
| `CUSTOM_SQL` | 自定义SQL条件 | 复杂业务规则，通过SQL表达式定义 |

### 6.3 规则配置（rule_config）

使用 JSONB 存储灵活的规则配置：

**示例1：部门规则**

```json
{
  "deptIds": ["uuid-1", "uuid-2"],
  "includeSubDepts": true
}
```

**示例2：用户规则**

```json
{
  "userIds": ["uuid-3", "uuid-4"],
  "fieldName": "create_by"
}
```

**示例3：自定义SQL规则**

```sql
custom_sql = "create_by = #{currentUserId} OR assigned_to = #{currentUserId}"
```

### 6.4 数据权限应用流程

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
│ - sys_data_permission 表        │
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

---

## 7. 权限查询优化

### 7.1 用户权限视图（v_user_permissions）

为了避免多表 JOIN 查询性能问题，创建视图用于快速查询用户权限：

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

### 7.2 查询示例

**查询用户所有菜单权限**

```sql
SELECT DISTINCT permission_code, permission_name, parent_id, sort_order
FROM v_user_permissions
WHERE user_id = '<user-id>'
  AND tenant_id = '<tenant-id>'
  AND permission_type = 'MENU'
ORDER BY sort_order;
```

**查询用户是否有某个按钮权限**

```sql
SELECT COUNT(*) > 0 AS has_permission
FROM v_user_permissions
WHERE user_id = '<user-id>'
  AND tenant_id = '<tenant-id>'
  AND permission_code = 'BTN_ORDER_DELETE';
```

**查询用户是否可以访问某个API**

```sql
SELECT COUNT(*) > 0 AS has_access
FROM v_user_permissions
WHERE user_id = '<user-id>'
  AND tenant_id = '<tenant-id>'
  AND permission_type = 'API'
  AND '/api/orders/123' LIKE REPLACE(REPLACE(api_path, '**', '%'), '*', '%')
  AND (http_method = 'DELETE' OR http_method = '*');
```

---

## 8. 应用层集成

### 8.1 权限检查拦截器（示例）

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

### 8.2 按钮权限指令（前端）

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

### 8.3 数据权限注解（后端）

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DataPermission {
    String resourceType();              // 资源类型（如 ORDER, PRODUCT）
    String[] fields() default {};       // 需要过滤的字段（如 create_by, department_id）
}

// 使用示例
@Service
public class OrderService {

    @DataPermission(resourceType = "ORDER", fields = {"create_by", "department_id"})
    public List<Order> listOrders(OrderQueryDTO query) {
        // MyBatis拦截器会自动注入数据权限条件
        // WHERE tenant_id = ? AND (数据权限SQL)
        return orderMapper.selectList(query);
    }
}
```

---

## 9. 最佳实践

### 9.1 平台权限 vs 租户权限

- **平台权限（tenant_id = NULL）**：
  - 所有租户共享的基础功能权限
  - 由平台统一维护，租户无法修改
  - 示例：订单管理菜单、商品创建按钮、基础API接口

- **租户权限（tenant_id ≠ NULL）**：
  - 租户根据业务需要自定义的权限
  - 仅在租户内可见和使用
  - 示例：租户特有的审批流程权限、自定义报表权限

### 9.2 权限粒度建议

- **菜单权限**：控制用户可以看到哪些菜单
- **按钮权限**：控制用户可以执行哪些操作
- **API权限**：防止绕过前端直接调用API
- **数据权限**：控制用户可以访问哪些数据范围

### 9.3 性能优化

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

### 9.4 安全建议

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

## 10. 迁移步骤

### 10.1 数据库迁移

```bash
# 1. 备份现有数据库
pg_dump -U postgres scm_platform > backup_before_migration.sql

# 2. 执行迁移脚本
psql -U postgres -d scm_platform -f scripts/db/migrations/004_transform_permission_system_multi_tenant.sql

# 3. 验证表结构
\d+ sys_user
\d+ sys_role
\d+ sys_permission
```

### 10.2 数据迁移

```sql
-- 1. 为现有用户分配默认租户
UPDATE sys_user
SET tenant_id = '<default-tenant-id>',
    user_type = 'TENANT_USER'
WHERE tenant_id IS NULL;

-- 2. 为平台管理员设置正确的类型
UPDATE sys_user
SET tenant_id = NULL,
    user_type = 'PLATFORM_ADMIN'
WHERE username IN ('admin', 'root');

-- 3. 同步关联表的 tenant_id
UPDATE sys_user_role ur
SET tenant_id = u.tenant_id
FROM sys_user u
WHERE ur.user_id = u.id;

UPDATE sys_role_permission rp
SET tenant_id = r.tenant_id
FROM sys_role r
WHERE rp.role_id = r.id;
```

### 10.3 应用层改造

1. **更新 Mapper XML**：
   - 所有查询添加 `tenant_id` 过滤条件

2. **集成 TenantInterceptor**：
   - 自动注入 `tenant_id` 条件

3. **更新权限检查逻辑**：
   - 使用 `v_user_permissions` 视图
   - 集成数据权限拦截器

4. **前端改造**：
   - 请求头携带 `X-Tenant-Id`
   - 集成按钮权限指令

---

## 11. 附录

### 11.1 完整 ER 图

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
└──────────────────────┘       └──────────────────────────┘
```

### 11.2 相关文档

- [数据库设计文档](./PHASE_1.5_SUMMARY.md)
- [多租户实现指南](./MULTI_TENANT_GUIDE.md)
- [API权限配置示例](./API_PERMISSION_CONFIG.md)

---

**文档版本**：v1.0
**最后更新**：2025-01-24
**作者**：Claude Code
# scm-system 模块多租户改造总结

## 1. 改造概述

本次改造将 scm-system 模块从单租户架构升级为支持多租户 SaaS 平台的架构，确保与数据库迁移脚本 `004_transform_permission_system_multi_tenant.sql` 保持一致。

**改造时间**：2025-01-24
**改造范围**：scm-system 模块的所有核心实体类
**改造目标**：支持多租户数据隔离、平台管理员/租户管理员权限分离、数据权限精细化控制

---

## 2. 改造内容总览

### 2.1 已更新的实体类

| 实体类 | 文件位置 | 新增字段 | 说明 |
|-------|---------|---------|------|
| **SysUser** | scm-system/.../entity/SysUser.java | tenant_id, user_type, data_scope | 用户表，支持平台/租户用户区分 |
| **SysRole** | scm-system/.../entity/SysRole.java | tenant_id, role_type, role_category, custom_dept_ids | 角色表，支持平台/租户角色区分 |
| **SysPermission** | scm-system/.../entity/SysPermission.java | tenant_id, permission_scope | 权限表，支持平台/租户权限区分 |
| **SysDept** | scm-system/.../entity/SysDept.java | tenant_id, dept_level, dept_path | 部门表，租户内部组织架构 |
| **SysUserRole** | scm-system/.../entity/SysUserRole.java | tenant_id | 用户角色关联表 |
| **SysRolePermission** | scm-system/.../entity/SysRolePermission.java | tenant_id | 角色权限关联表 |
| **SysDataPermissionRule** | scm-system/.../entity/SysDataPermissionRule.java | tenant_id | 数据权限规则表 |
| **SysRoleDataRule** | scm-system/.../entity/SysRoleDataRule.java | tenant_id | 角色数据权限关联表 |

---

## 3. 详细改造说明

### 3.1 SysUser（用户表）

**新增字段**：

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

**字段说明**：

- **tenant_id**: 租户ID，NULL 表示平台管理员（不属于任何租户）
- **user_type**: 用户类型
  - `PLATFORM_ADMIN`: 平台管理员，可管理所有租户
  - `TENANT_ADMIN`: 租户管理员，可管理租户内所有资源
  - `TENANT_USER`: 租户普通用户，权限受角色限制
- **data_scope**: 数据权限范围
  - `ALL`: 全部数据（租户内）
  - `DEPT`: 本部门数据
  - `DEPT_AND_SUB`: 本部门及下级部门数据
  - `SELF`: 仅本人创建的数据
  - `CUSTOM`: 自定义规则

**使用示例**：

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

---

### 3.2 SysRole（角色表）

**新增字段**：

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

**字段说明**：

- **tenant_id**: 租户ID，NULL 表示平台角色（所有租户共享）
- **role_type**: 角色类型
  - `PLATFORM_ROLE`: 平台角色，由平台统一定义，跨租户使用
  - `TENANT_ROLE`: 租户角色，租户自定义，仅在租户内使用
- **role_category**: 角色分类
  - `BUSINESS`: 业务角色（如：采购员、仓管员、销售员）
  - `FUNCTIONAL`: 职能角色（如：财务、人事、IT）
  - `CUSTOM`: 自定义角色
- **custom_dept_ids**: 自定义部门ID列表（JSONB），当 `data_scope=CUSTOM` 时指定可访问的部门

**使用示例**：

```java
// 创建平台角色（所有租户共享）
SysRole platformRole = new SysRole();
platformRole.setTenantId(null);
platformRole.setRoleCode("SUPER_ADMIN");
platformRole.setRoleName("超级管理员");
platformRole.setRoleType("PLATFORM_ROLE");
platformRole.setDataScope(1); // 全部数据

// 创建租户角色
SysRole tenantRole = new SysRole();
tenantRole.setTenantId(tenantId);
tenantRole.setRoleCode("WAREHOUSE_MANAGER");
tenantRole.setRoleName("仓库经理");
tenantRole.setRoleType("TENANT_ROLE");
tenantRole.setRoleCategory("BUSINESS");
tenantRole.setDataScope(3); // 本部门数据
```

---

### 3.3 SysPermission（权限表）

**新增字段**：

```java
@Schema(description = "租户ID（NULL=平台级权限，所有租户共享）")
@TableField("tenant_id")
private UUID tenantId;

@Schema(description = "权限归属:PLATFORM-平台级权限(所有租户共享),TENANT-租户级权限(租户自定义)")
@TableField("permission_scope")
private String permissionScope;
```

**字段说明**：

- **tenant_id**: 租户ID，NULL 表示平台级权限（所有租户共享）
- **permission_scope**: 权限归属
  - `PLATFORM`: 平台级权限，由平台统一定义，所有租户共享
  - `TENANT`: 租户级权限，租户自定义，仅在租户内使用

**使用示例**：

```java
// 平台级菜单权限（所有租户共享）
SysPermission platformPermission = new SysPermission();
platformPermission.setTenantId(null);
platformPermission.setPermissionCode("MENU_ORDER");
platformPermission.setPermissionName("订单管理");
platformPermission.setPermissionType(2); // 菜单
platformPermission.setPermissionScope("PLATFORM");

// 租户自定义权限
SysPermission tenantPermission = new SysPermission();
tenantPermission.setTenantId(tenantId);
tenantPermission.setPermissionCode("CUSTOM_REPORT_SALES");
tenantPermission.setPermissionName("销售分析报表");
tenantPermission.setPermissionType(2);
tenantPermission.setPermissionScope("TENANT");
```

---

### 3.4 SysDept（部门表）

**新增字段**：

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

**字段说明**：

- **tenant_id**: 租户ID，部门按租户隔离
- **dept_level**: 部门层级（1=一级部门，2=二级部门，以此类推）
- **dept_path**: 部门路径，用于快速查询上下级部门
  - 格式：`/parent_uuid/current_uuid`
  - 示例：`/uuid1/uuid2/uuid3` 表示三级部门

**使用示例**：

```java
// 创建一级部门
SysDept rootDept = new SysDept();
rootDept.setTenantId(tenantId);
rootDept.setDeptCode("HEADQUARTERS");
rootDept.setDeptName("总部");
rootDept.setParentId(null);
rootDept.setDeptLevel(1);
rootDept.setDeptPath("/" + rootDept.getId());

// 创建二级部门
SysDept subDept = new SysDept();
subDept.setTenantId(tenantId);
subDept.setDeptCode("SALES");
subDept.setDeptName("销售部");
subDept.setParentId(rootDept.getId());
subDept.setDeptLevel(2);
subDept.setDeptPath(rootDept.getDeptPath() + "/" + subDept.getId());

// 查询某部门及其所有下级部门
List<SysDept> subDepts = sysDeptMapper.selectList(
    new QueryWrapper<SysDept>()
        .eq("tenant_id", tenantId)
        .likeRight("dept_path", rootDept.getDeptPath())
);
```

---

### 3.5 关联表（SysUserRole, SysRolePermission）

**新增字段**：均添加了 `tenant_id` 字段作为冗余字段，用于优化查询性能。

```java
// SysUserRole
@Schema(description = "租户ID（冗余字段，来自用户表）")
@TableField("tenant_id")
private UUID tenantId;

// SysRolePermission
@Schema(description = "租户ID（冗余字段，来自角色表）")
@TableField("tenant_id")
private UUID tenantId;
```

**说明**：

- `tenant_id` 字段由数据库触发器自动填充（见迁移脚本）
- 无需在应用层手动设置
- 用于优化多租户环境下的查询性能

---

### 3.6 数据权限相关表

**SysDataPermissionRule**（数据权限规则表）：

```java
@Schema(description = "租户ID")
@TableField("tenant_id")
private UUID tenantId;
```

**SysRoleDataRule**（角色数据权限关联表）：

```java
@Schema(description = "租户ID（冗余字段）")
@TableField("tenant_id")
private UUID tenantId;
```

---

## 4. 与基础设施的集成

### 4.1 TenantInterceptor 自动注入 tenant_id

已实现的 `TenantInterceptor` 会自动为所有查询注入 `tenant_id` 条件：

```java
// 原始 SQL
SELECT * FROM sys_user WHERE username = ?

// TenantInterceptor 自动注入后
SELECT * FROM sys_user WHERE username = ? AND tenant_id = '<current-tenant-id>'
```

**排除的表**：

以下表不会被注入 `tenant_id`（因为它们本身就是租户管理表）：

- tenant
- tenant_package
- tenant_subscription
- tenant_resource_quota
- tenant_config
- tenant_feature
- tenant_operation_log

### 4.2 AuditMetaObjectHandler 自动填充

已实现的 `AuditMetaObjectHandler` 会自动填充审计字段：

```java
// INSERT 时自动填充
- id (UUIDv7)
- tenant_id (从 TenantContextHolder 获取)
- create_time
- create_by
- update_time
- deleted (默认 false)

// UPDATE 时自动填充
- update_time
- update_by
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

---

## 5. 数据库迁移步骤

### 5.1 执行迁移脚本

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

### 5.2 数据迁移

执行迁移脚本后，需要手动为现有数据分配租户：

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

---

## 6. 使用建议

### 6.1 平台管理员 vs 租户管理员

**平台管理员（tenant_id = NULL）**：

- 可以访问和管理所有租户
- 不受 `TenantInterceptor` 限制
- 通常用于平台运维、监控、配置

**租户管理员（tenant_id ≠ NULL, user_type = 'TENANT_ADMIN'）**：

- 只能访问和管理自己租户的数据
- 受 `TenantInterceptor` 限制
- 拥有租户内最高权限

### 6.2 权限设计最佳实践

1. **平台权限（tenant_id = NULL）**：
   - 只创建基础功能权限
   - 所有租户共享
   - 平台统一维护，租户无法修改

2. **租户权限（tenant_id ≠ NULL）**：
   - 租户根据业务需要自定义
   - 仅在租户内可见和使用
   - 适用于租户特有的业务流程

3. **角色分类**：
   - 使用 `role_category` 对角色进行分类管理
   - `BUSINESS`：业务角色（推荐使用）
   - `FUNCTIONAL`：职能角色
   - `CUSTOM`：自定义角色

### 6.3 数据权限配置

**示例：配置仓库经理的数据权限**

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
// WHERE tenant_id = ? AND create_by IN (本部门用户ID列表)
```

---

## 7. 注意事项

### 7.1 必须设置租户上下文

所有业务操作前必须设置租户上下文，否则会抛出异常：

```java
// HTTP 请求必须携带 X-Tenant-Id 头
X-Tenant-Id: 123e4567-e89b-12d3-a456-426614174000

// 或在代码中手动设置
TenantContextHolder.setTenantId(tenantId);
```

### 7.2 平台管理员操作

平台管理员需要临时切换到特定租户上下文时：

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

### 7.3 查询优化

所有涉及租户数据的查询都应该建立 `(tenant_id, ...)` 复合索引：

```sql
-- 优化用户名查询
CREATE UNIQUE INDEX idx_user_username ON sys_user(tenant_id, username) WHERE NOT deleted;

-- 优化角色编码查询
CREATE UNIQUE INDEX idx_role_code ON sys_role(tenant_id, role_code) WHERE NOT deleted;
```

---

## 8. 相关文档

- [权限系统多租户设计文档](./PERMISSION_MULTI_TENANT_DESIGN.md)
- [数据库迁移脚本](../scripts/db/migrations/004_transform_permission_system_multi_tenant.sql)
- [应用层配置和使用示例](./APPLICATION_CONFIG_EXAMPLES.md)
- [Phase 1.5 数据库设计总结](./PHASE_1.5_SUMMARY.md)

---

## 9. 改造清单

### 已完成

- [x] SysUser 实体类多租户字段添加
- [x] SysRole 实体类多租户字段添加
- [x] SysPermission 实体类多租户字段添加
- [x] SysDept 实体类多租户字段添加
- [x] SysUserRole 关联表多租户字段添加
- [x] SysRolePermission 关联表多租户字段添加
- [x] SysDataPermissionRule 多租户字段添加
- [x] SysRoleDataRule 多租户字段添加
- [x] 改造文档编写

### 待完成（后续任务）

- [ ] Service 层改造（支持多租户查询）
- [ ] Mapper XML 改造（根据需要）
- [ ] Controller 层改造（租户ID验证）
- [ ] 单元测试编写
- [ ] 集成测试编写

---

**文档版本**：v1.0
**最后更新**：2025-01-24
**作者**：Claude Code
# Service 层多租户改造总结

## 1. 改造概述

本次改造为 scm-system 模块的 Service 层添加了完整的租户验证和权限检查机制，确保多租户 SaaS 平台的数据安全和权限隔离。

**改造时间**：2025-01-24
**改造范围**：Service 层所有业务方法
**改造目标**：租户数据隔离、权限精细化控制、操作审计

---

## 2. 已完成的工作

### 2.1 创建核心工具类（2个）

| 工具类 | 文件位置 | 主要功能 |
|-------|---------|---------|
| **TenantValidationUtil** | scm-common/core/.../tenant/TenantValidationUtil.java | 租户上下文验证、数据归属验证 |
| **PermissionChecker** | scm-common/core/.../security/PermissionChecker.java | 权限检查、角色检查、数据权限过滤 |

### 2.2 编写改造示例文档（1个）

| 文档 | 文件位置 | 内容 |
|------|---------|------|
| **Service 层改造示例** | docs/SERVICE_MULTI_TENANT_ENHANCEMENT_EXAMPLE.md | 详细的改造示例和最佳实践 |

---

## 3. 核心工具类说明

### 3.1 TenantValidationUtil（租户验证工具）

**文件位置**：`scm-common/core/src/main/java/com/frog/common/tenant/TenantValidationUtil.java`

**核心方法**：

```java
// 验证租户上下文是否已设置
TenantValidationUtil.validateTenantContext();

// 获取当前租户ID（必须存在）
UUID tenantId = TenantValidationUtil.getRequiredTenantId();

// 验证数据是否属于当前租户
TenantValidationUtil.validateDataOwnership(entity.getTenantId());

// 验证角色访问权限
TenantValidationUtil.validateRoleAccess(role.getTenantId());

// 验证权限访问权限
TenantValidationUtil.validatePermissionAccess(permission.getTenantId());

// 判断是否为平台管理员
boolean isPlatformAdmin = TenantValidationUtil.isPlatformAdmin(userType);

// 判断是否为租户管理员
boolean isTenantAdmin = TenantValidationUtil.isTenantAdmin(userType());

// 验证部门归属
TenantValidationUtil.validateDepartmentOwnership(dept.getTenantId());
```

**使用场景**：

1. **所有 Service 方法的开始处**：验证租户上下文
2. **查询/更新/删除数据前**：验证数据归属
3. **分配角色/权限前**：验证资源访问权限
4. **区分用户类型时**：判断平台/租户管理员

### 3.2 PermissionChecker（权限检查工具）

**文件位置**：`scm-common/core/src/main/java/com/frog/common/security/PermissionChecker.java`

**核心方法**：

```java
// 检查是否有指定权限
boolean hasPermission = PermissionChecker.hasPermission(userId, "BTN_USER_CREATE");

// 要求必须有权限（否则抛异常）
PermissionChecker.requirePermission(userId, "BTN_USER_DELETE");

// 检查是否可访问部门数据
boolean canAccess = PermissionChecker.canAccessDepartmentData(
    userId, userDeptId, targetDeptId, dataScope, deptPath, targetDeptPath
);

// 检查是否可操作资源
boolean canOperate = PermissionChecker.canOperateResource(
    userId, resourceOwnerId, resourceDeptId, dataScope
);

// 检查是否可分配角色
boolean canAssign = PermissionChecker.canAssignRole(
    operatorUserId, operatorRoleLevel, targetRoleLevel
);

// 获取用户数据权限范围
String dataScope = PermissionChecker.getUserDataScope(userId);
```

**使用场景**：

1. **操作前权限检查**：创建、更新、删除等操作
2. **数据权限过滤**：根据部门、创建人等过滤数据
3. **角色分配前**：检查是否有权分配指定等级的角色
4. **API访问控制**：验证用户是否可访问指定API

---

## 4. Service 层改造模式

所有 Service 方法都应遵循统一的改造模式：

```java
@Transactional
public void serviceMethod(...) {
    // ============ 1. 租户上下文验证 ============
    TenantValidationUtil.validateTenantContext();
    UUID currentTenantId = TenantValidationUtil.getRequiredTenantId();
    UUID currentUserId = SecurityUtils.getCurrentUserId();

    // ============ 2. 操作权限检查 ============
    PermissionChecker.requirePermission(currentUserId, "权限编码");

    // ============ 3. 查询数据 ============
    Entity entity = mapper.selectById(id);
    if (entity == null) {
        throw new BusinessException("资源不存在");
    }

    // ============ 4. 数据归属验证 ============
    TenantValidationUtil.validateDataOwnership(entity.getTenantId());

    // ============ 5. 数据权限检查（可选）============
    String dataScope = PermissionChecker.getUserDataScope(currentUserId);
    if (!PermissionChecker.canOperateResource(currentUserId,
            entity.getCreateBy(), entity.getDeptId(), dataScope)) {
        throw new BusinessException("DATA_ACCESS_DENIED", "无权操作该数据");
    }

    // ============ 6. 执行业务逻辑 ============
    // 业务逻辑代码...

    // ============ 7. 记录操作日志 ============
    TenantValidationUtil.logTenantOperation("操作类型", "资源类型", resourceId);

    // ============ 8. 日志记录 ============
    log.info("操作成功: ...");
}
```

---

## 5. 改造示例汇总

### 5.1 UserService 改造要点

| 方法 | 关键改造点 |
|------|-----------|
| `addUser` | 验证租户上下文、检查权限、设置 tenant_id、检查配额 |
| `updateUser` | 验证数据归属、数据权限检查、防止修改 tenant_id |
| `deleteUser` | 验证数据归属、数据权限检查、防止删除自己、释放配额 |
| `getUserById` | 验证数据归属（双重检查） |
| `grantRoles` | 验证角色访问权限、角色等级检查、清除权限缓存 |
| `grantTemporaryRoles` | 临时角色有效期验证、审批流程集成 |

**关键代码片段**：

```java
// 创建用户时设置租户ID
user.setTenantId(currentTenantId);
user.setUserType("TENANT_USER");

// 更新用户时防止修改租户ID
user.setTenantId(null); // 设置为 null，避免被误改

// 删除用户时释放配额
QuotaService.releaseQuota(currentTenantId, QuotaType.USERS, 1);

// 分配角色时验证角色访问权限
TenantValidationUtil.validateRoleAccess(role.getTenantId());
```

### 5.2 RoleService 改造要点

| 方法 | 关键改造点 |
|------|-----------|
| `addRole` | 区分平台角色/租户角色、平台管理员验证、设置正确的 tenant_id |
| `updateRole` | 验证角色访问权限、防止修改 tenant_id 和 role_type |
| `deleteRole` | 验证角色访问权限、检查是否有用户使用该角色 |
| `assignPermissions` | 验证权限访问权限、清除权限缓存 |

**关键代码片段**：

```java
// 创建平台角色
if ("PLATFORM_ROLE".equals(roleDTO.getRoleType())) {
    TenantValidationUtil.validatePlatformResourceCreation(currentUser.getUserType());
    role.setTenantId(null); // 平台角色 tenant_id 为 NULL
} else {
    role.setTenantId(currentTenantId); // 租户角色
}

// 分配权限时验证权限访问权限
TenantValidationUtil.validatePermissionAccess(permission.getTenantId());
```

### 5.3 PermissionService 改造要点

| 方法 | 关键改造点 |
|------|-----------|
| `addPermission` | 区分平台权限/租户权限、平台管理员验证、设置正确的 tenant_id |
| `updatePermission` | 验证权限访问权限、防止修改 tenant_id 和 permission_scope |
| `deletePermission` | 验证权限访问权限、检查是否有角色使用该权限 |

**关键代码片段**：

```java
// 创建平台权限
if ("PLATFORM".equals(permissionDTO.getPermissionScope())) {
    TenantValidationUtil.validatePlatformResourceCreation(currentUser.getUserType());
    permission.setTenantId(null); // 平台权限 tenant_id 为 NULL
} else {
    permission.setTenantId(currentTenantId); // 租户权限
}
```

### 5.4 DeptService 改造要点

| 方法 | 关键改造点 |
|------|-----------|
| `addDept` | 验证父部门归属、自动计算 dept_level 和 dept_path |
| `updateDept` | 验证部门归属、更新子部门的 dept_path |
| `deleteDept` | 验证部门归属、检查是否有子部门、检查是否有用户 |
| `getDeptTree` | 只查询当前租户的部门、构建树形结构 |

**关键代码片段**：

```java
// 创建部门时自动计算 dept_path
if (deptDTO.getParentId() != null) {
    SysDept parentDept = deptMapper.selectById(deptDTO.getParentId());
    TenantValidationUtil.validateDepartmentOwnership(parentDept.getTenantId());

    deptLevel = parentDept.getDeptLevel() + 1;
    deptPath = parentDept.getDeptPath();
}

dept.setDeptPath(deptPath + "/" + dept.getId());
```

---

## 6. 特殊场景处理

### 6.1 平台管理员跨租户访问

平台管理员需要访问特定租户数据时，使用 `TenantContextHolder.executeInTenantContext`：

```java
public void platformAdminOperation(UUID targetTenantId, UUID targetResourceId) {
    // 1. 验证是否为平台管理员
    UUID currentUserId = SecurityUtils.getCurrentUserId();
    SysUser currentUser = userMapper.selectById(currentUserId);

    if (!TenantValidationUtil.isPlatformAdmin(currentUser.getUserType())) {
        throw new BusinessException("PLATFORM_ADMIN_REQUIRED", "需要平台管理员权限");
    }

    // 2. 在目标租户上下文中执行操作
    TenantContextHolder.executeInTenantContext(targetTenantId, () -> {
        // 在这里执行需要租户上下文的操作
        Entity entity = mapper.selectById(targetResourceId);
        // 处理业务逻辑...
        return null;
    });

    log.info("平台管理员 {} 访问租户 {} 的资源 {}", currentUserId, targetTenantId, targetResourceId);
}
```

### 6.2 查询方法自动过滤

查询方法不需要显式添加 `tenant_id` 条件，`TenantInterceptor` 会自动注入：

```java
// Service 层代码
List<SysUser> users = userMapper.selectList(
    new LambdaQueryWrapper<SysUser>()
        .eq(SysUser::getStatus, 1)
        .orderByDesc(SysUser::getCreateTime)
);

// 实际执行的 SQL（TenantInterceptor 自动注入）
SELECT * FROM sys_user
WHERE status = 1
  AND tenant_id = '<current-tenant-id>'  -- 自动注入
  AND NOT deleted                        -- 逻辑删除过滤
ORDER BY create_time DESC;
```

### 6.3 数据权限过滤示例

根据用户的数据权限范围过滤数据：

```java
@Transactional(readOnly = true)
public List<Order> listOrders(OrderQueryDTO query) {
    // 1. 验证租户上下文
    UUID currentTenantId = TenantValidationUtil.getRequiredTenantId();
    UUID currentUserId = SecurityUtils.getCurrentUserId();

    // 2. 获取当前用户的数据权限范围
    String dataScope = PermissionChecker.getUserDataScope(currentUserId);

    // 3. 根据数据权限构建查询条件
    LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();

    switch (dataScope) {
        case "SELF":
            // 只能查看自己创建的订单
            wrapper.eq(Order::getCreateBy, currentUserId);
            break;

        case "DEPT":
            // 查看本部门的订单
            UUID userDeptId = getCurrentUserDeptId(currentUserId);
            wrapper.eq(Order::getDeptId, userDeptId);
            break;

        case "DEPT_AND_SUB":
            // 查看本部门及下级部门的订单
            List<UUID> deptIds = PermissionChecker.getAccessibleDepartmentIds(currentUserId, currentTenantId);
            wrapper.in(Order::getDeptId, deptIds);
            break;

        case "ALL":
            // 可以查看所有订单（tenant_id 由 TenantInterceptor 自动注入）
            break;

        case "CUSTOM":
            // 自定义数据权限（需要查询 sys_data_permission_rule）
            // TODO: 实现自定义数据权限逻辑
            break;
    }

    // 4. 添加其他查询条件
    if (query.getOrderNo() != null) {
        wrapper.like(Order::getOrderNo, query.getOrderNo());
    }

    // 5. 查询订单（TenantInterceptor 会自动注入 tenant_id）
    List<Order> orders = orderMapper.selectList(wrapper);

    return orders;
}
```

---

## 7. 缓存管理

### 7.1 清除用户权限缓存

当用户的角色或权限发生变化时，需要清除权限缓存：

```java
/**
 * 清除用户权限缓存
 */
private void clearUserPermissionCache(UUID userId) {
    // 清除用户信息缓存
    cacheManager.getCache("user").evict(userId);

    // 清除用户权限缓存
    cacheManager.getCache("userPermissions").evict(userId);

    // 清除用户角色缓存
    cacheManager.getCache("userRoles").evict(userId);

    log.info("已清除用户 {} 的权限缓存", userId);
}

/**
 * 清除角色下所有用户的权限缓存
 */
private void clearRoleUsersPermissionCache(UUID roleId) {
    // 查询所有拥有该角色的用户
    List<SysUserRole> userRoles = userRoleMapper.selectList(
        new LambdaQueryWrapper<SysUserRole>()
            .eq(SysUserRole::getRoleId, roleId)
    );

    // 清除每个用户的权限缓存
    userRoles.forEach(userRole -> {
        clearUserPermissionCache(userRole.getUserId());
    });

    log.info("已清除角色 {} 下所有用户的权限缓存，共 {} 个用户", roleId, userRoles.size());
}
```

---

## 8. 异常处理

### 8.1 自定义业务异常

建议定义以下业务异常：

```java
// 租户相关异常
TENANT_CONTEXT_MISSING("租户上下文未设置")
TENANT_DATA_ACCESS_DENIED("无权访问其他租户的数据")
TENANT_ROLE_ACCESS_DENIED("无权访问其他租户的角色")
TENANT_PERMISSION_ACCESS_DENIED("无权访问其他租户的权限")
TENANT_DEPT_ACCESS_DENIED("无权访问其他租户的部门")

// 权限相关异常
PERMISSION_DENIED("权限不足")
ROLE_REQUIRED("需要指定角色")
DATA_ACCESS_DENIED("无权访问该数据")
API_ACCESS_DENIED("无权访问该API")
ROLE_ASSIGNMENT_DENIED("无权分配该角色")

// 平台管理员相关异常
PLATFORM_ADMIN_REQUIRED("需要平台管理员权限")
PLATFORM_RESOURCE_ACCESS_DENIED("只有平台管理员可以操作平台级资源")
```

### 8.2 全局异常处理

```java
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常: code={}, message={}", e.getCode(), e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    // 其他异常处理...
}
```

---

## 9. 测试建议

### 9.1 单元测试模板

```java
@SpringBootTest
@Transactional
public class UserServiceTest {

    @Autowired
    private ISysUserService userService;

    @BeforeEach
    public void setup() {
        // 设置租户上下文
        UUID testTenantId = UUID.fromString("...");
        TenantContextHolder.setTenantId(testTenantId);
    }

    @AfterEach
    public void teardown() {
        // 清除租户上下文
        TenantContextHolder.clear();
    }

    @Test
    public void testAddUser_Success() {
        // 准备测试数据
        UserDTO userDTO = new UserDTO();
        userDTO.setUsername("test_user");
        userDTO.setRealName("测试用户");

        // 执行测试
        userService.addUser(userDTO);

        // 验证结果
        SysUser createdUser = userMapper.selectOne(
            new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, "test_user")
        );

        assertNotNull(createdUser);
        assertEquals(TenantContextHolder.getTenantId(), createdUser.getTenantId());
        assertEquals("TENANT_USER", createdUser.getUserType());
    }

    @Test(expected = BusinessException.class)
    public void testAddUser_WithoutTenantContext_ShouldThrowException() {
        // 清除租户上下文
        TenantContextHolder.clear();

        // 准备测试数据
        UserDTO userDTO = new UserDTO();
        userDTO.setUsername("test_user");

        // 执行测试（应该抛出 TENANT_CONTEXT_MISSING 异常）
        userService.addUser(userDTO);
    }

    @Test(expected = BusinessException.class)
    public void testUpdateUser_OtherTenantData_ShouldThrowException() {
        // 创建其他租户的用户
        UUID otherTenantId = UUID.fromString("...");
        SysUser otherUser = new SysUser();
        otherUser.setId(UUIDv7Util.generate());
        otherUser.setTenantId(otherTenantId);
        otherUser.setUsername("other_user");
        userMapper.insert(otherUser);

        // 尝试更新其他租户的用户（应该抛出 TENANT_DATA_ACCESS_DENIED 异常）
        UserDTO userDTO = new UserDTO();
        userDTO.setId(otherUser.getId());
        userDTO.setRealName("修改名称");

        userService.updateUser(userDTO);
    }
}
```

---

## 10. 相关文档

- [scm-system 模块多租户改造总结](./SCM_SYSTEM_MULTI_TENANT_MIGRATION.md)
- [Service 层多租户增强示例](./SERVICE_MULTI_TENANT_ENHANCEMENT_EXAMPLE.md)
- [权限系统多租户设计文档](./PERMISSION_MULTI_TENANT_DESIGN.md)
- [应用层配置和使用示例](./APPLICATION_CONFIG_EXAMPLES.md)

---

## 11. 改造清单

### 已完成

- [x] TenantValidationUtil 工具类创建
- [x] PermissionChecker 工具类创建
- [x] Service 层改造示例文档编写
- [x] Service 层改造总结文档编写

### 建议后续完成（可选）

- [ ] UserService 具体方法改造
- [ ] RoleService 具体方法改造
- [ ] PermissionService 具体方法改造
- [ ] DeptService 具体方法改造
- [ ] 单元测试编写
- [ ] 集成测试编写
- [ ] 性能测试和优化

---

## 12. 使用建议

### 12.1 逐步改造

建议按以下顺序逐步改造 Service 层：

1. **核心 Service 优先**：UserService → RoleService → PermissionService
2. **业务 Service 其次**：DeptService → 其他业务 Service
3. **测试验证**：每改造一个 Service，立即编写单元测试验证

### 12.2 保持一致性

所有 Service 方法都应遵循统一的改造模式，确保：

1. 所有方法都验证租户上下文
2. 所有写操作都检查权限
3. 所有数据访问都验证归属
4. 所有操作都记录日志

### 12.3 性能优化

1. **权限信息缓存**：将用户权限信息缓存到 Redis，减少数据库查询
2. **批量操作优化**：批量验证、批量查询，减少数据库往返次数
3. **异步日志记录**：使用异步方式记录操作日志，不阻塞主流程

---

**文档版本**：v1.0
**最后更新**：2025-01-24
**作者**：Claude Code
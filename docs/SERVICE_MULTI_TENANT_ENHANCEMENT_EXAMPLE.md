# Service 层多租户增强示例

本文档展示如何在 Service 层添加租户验证和权限检查，确保多租户环境下的数据安全。

---

## 1. 核心工具类

已创建两个核心工具类：

### 1.1 TenantValidationUtil（租户验证工具）

文件位置：`scm-common/core/src/main/java/com/frog/common/tenant/TenantValidationUtil.java`

**主要方法**：

| 方法 | 说明 | 使用场景 |
|------|------|----------|
| `validateTenantContext()` | 验证租户上下文是否已设置 | 所有业务方法开始处 |
| `getRequiredTenantId()` | 获取当前租户ID（必须存在） | 需要租户ID时 |
| `validateDataOwnership(UUID)` | 验证数据是否属于当前租户 | 查询/更新/删除数据前 |
| `validateRoleAccess(UUID)` | 验证角色访问权限 | 分配角色前 |
| `validatePermissionAccess(UUID)` | 验证权限访问权限 | 分配权限前 |
| `isPlatformAdmin(String)` | 判断是否为平台管理员 | 需要区分用户类型时 |
| `isTenantAdmin(String)` | 判断是否为租户管理员 | 需要管理员权限时 |

### 1.2 PermissionChecker（权限检查工具）

文件位置：`scm-common/core/src/main/java/com/frog/common/security/PermissionChecker.java`

**主要方法**：

| 方法 | 说明 | 使用场景 |
|------|------|----------|
| `hasPermission(UUID, String)` | 检查是否有指定权限 | 操作前权限检查 |
| `requirePermission(UUID, String)` | 要求必须有权限（否则抛异常） | 严格权限控制 |
| `canAccessDepartmentData(...)` | 检查是否可访问部门数据 | 数据权限过滤 |
| `canOperateResource(...)` | 检查是否可操作资源 | 编辑/删除资源前 |
| `canAssignRole(...)` | 检查是否可分配角色 | 角色分配前 |

---

## 2. SysUserService 改造示例

### 2.1 添加用户（addUser）

**改造前**：

```java
@Transactional
public void addUser(UserDTO userDTO) {
    // 直接创建用户
    SysUser user = new SysUser();
    BeanUtils.copyProperties(userDTO, user);
    user.setId(UUIDv7Util.generate());
    user.setPassword(passwordEncoder.encode(defaultPassword));

    userMapper.insert(user);
}
```

**改造后**：

```java
@Transactional
public void addUser(UserDTO userDTO) {
    // 1. 验证租户上下文
    TenantValidationUtil.validateTenantContext();
    UUID currentTenantId = TenantValidationUtil.getRequiredTenantId();

    // 2. 获取当前操作用户
    UUID currentUserId = SecurityUtils.getCurrentUserId();

    // 3. 检查是否有创建用户的权限
    PermissionChecker.requirePermission(currentUserId, "BTN_USER_CREATE");

    // 4. 检查配额（可选）
    // QuotaService.checkAndConsumeQuota(currentTenantId, QuotaType.USERS, 1);

    // 5. 创建用户
    SysUser user = new SysUser();
    BeanUtils.copyProperties(userDTO, user);
    user.setId(UUIDv7Util.generate());
    user.setTenantId(currentTenantId); // 设置租户ID
    user.setUserType("TENANT_USER"); // 默认为租户用户
    user.setPassword(passwordEncoder.encode(defaultPassword));

    // 6. 保存用户（AuditMetaObjectHandler 会自动填充审计字段）
    userMapper.insert(user);

    // 7. 记录操作日志
    TenantValidationUtil.logTenantOperation("CREATE_USER", "SysUser", user.getId());

    log.info("租户 {} 创建用户成功: userId={}, username={}", currentTenantId, user.getId(), user.getUsername());
}
```

### 2.2 更新用户（updateUser）

**改造后**：

```java
@Transactional
@CacheEvict(value = "user", key = "#userDTO.id")
public void updateUser(UserDTO userDTO) {
    // 1. 验证租户上下文
    TenantValidationUtil.validateTenantContext();
    UUID currentTenantId = TenantValidationUtil.getRequiredTenantId();
    UUID currentUserId = SecurityUtils.getCurrentUserId();

    // 2. 检查权限
    PermissionChecker.requirePermission(currentUserId, "BTN_USER_UPDATE");

    // 3. 查询原用户数据
    SysUser existingUser = userMapper.selectById(userDTO.getId());
    if (existingUser == null) {
        throw new BusinessException("USER_NOT_FOUND", "用户不存在");
    }

    // 4. 验证数据归属（确保用户属于当前租户）
    TenantValidationUtil.validateDataOwnership(existingUser.getTenantId());

    // 5. 数据权限检查：是否可以编辑该用户
    String dataScope = PermissionChecker.getUserDataScope(currentUserId);
    if (!PermissionChecker.canOperateResource(currentUserId, existingUser.getCreateBy(),
                                               existingUser.getDeptId(), dataScope)) {
        throw new BusinessException("DATA_ACCESS_DENIED", "无权编辑该用户");
    }

    // 6. 更新用户
    SysUser user = new SysUser();
    BeanUtils.copyProperties(userDTO, user);
    // tenant_id 不允许修改
    user.setTenantId(null); // 设置为null，避免被误改

    userMapper.updateById(user);

    // 7. 记录操作日志
    TenantValidationUtil.logTenantOperation("UPDATE_USER", "SysUser", user.getId());

    log.info("租户 {} 更新用户成功: userId={}", currentTenantId, user.getId());
}
```

### 2.3 删除用户（deleteUser）

**改造后**：

```java
@Transactional
@CacheEvict(value = "user", key = "#id")
public void deleteUser(UUID id) {
    // 1. 验证租户上下文
    TenantValidationUtil.validateTenantContext();
    UUID currentTenantId = TenantValidationUtil.getRequiredTenantId();
    UUID currentUserId = SecurityUtils.getCurrentUserId();

    // 2. 检查权限
    PermissionChecker.requirePermission(currentUserId, "BTN_USER_DELETE");

    // 3. 查询用户
    SysUser user = userMapper.selectById(id);
    if (user == null) {
        throw new BusinessException("USER_NOT_FOUND", "用户不存在");
    }

    // 4. 验证数据归属
    TenantValidationUtil.validateDataOwnership(user.getTenantId());

    // 5. 数据权限检查
    String dataScope = PermissionChecker.getUserDataScope(currentUserId);
    if (!PermissionChecker.canOperateResource(currentUserId, user.getCreateBy(),
                                               user.getDeptId(), dataScope)) {
        throw new BusinessException("DATA_ACCESS_DENIED", "无权删除该用户");
    }

    // 6. 防止删除自己
    if (id.equals(currentUserId)) {
        throw new BusinessException("CANNOT_DELETE_SELF", "不能删除自己");
    }

    // 7. 软删除用户（MyBatis-Plus 会自动设置 deleted=true）
    userMapper.deleteById(id);

    // 8. 释放配额（可选）
    // QuotaService.releaseQuota(currentTenantId, QuotaType.USERS, 1);

    // 9. 记录操作日志
    TenantValidationUtil.logTenantOperation("DELETE_USER", "SysUser", id);

    log.info("租户 {} 删除用户成功: userId={}", currentTenantId, id);
}
```

### 2.4 查询用户（getUserById）

**改造后**：

```java
@Slave
@Cacheable(value = "user", key = "#id")
public UserDTO getUserById(UUID id) {
    // 1. 验证租户上下文
    TenantValidationUtil.validateTenantContext();
    UUID currentTenantId = TenantValidationUtil.getRequiredTenantId();

    // 2. 查询用户（TenantInterceptor 会自动注入 tenant_id 条件）
    SysUser user = userMapper.selectById(id);
    if (user == null) {
        throw new BusinessException("USER_NOT_FOUND", "用户不存在");
    }

    // 3. 双重验证：确保数据属于当前租户
    TenantValidationUtil.validateDataOwnership(user.getTenantId());

    // 4. 转换为 DTO
    UserDTO userDTO = convertToDTO(user);

    return userDTO;
}
```

### 2.5 分配角色（grantRoles）

**改造后**：

```java
@Transactional
public void grantRoles(UUID userId, List<UUID> roleIds) {
    // 1. 验证租户上下文
    TenantValidationUtil.validateTenantContext();
    UUID currentTenantId = TenantValidationUtil.getRequiredTenantId();
    UUID currentUserId = SecurityUtils.getCurrentUserId();

    // 2. 检查权限
    PermissionChecker.requirePermission(currentUserId, "BTN_USER_GRANT_ROLE");

    // 3. 验证目标用户属于当前租户
    SysUser targetUser = userMapper.selectById(userId);
    if (targetUser == null) {
        throw new BusinessException("USER_NOT_FOUND", "用户不存在");
    }
    TenantValidationUtil.validateDataOwnership(targetUser.getTenantId());

    // 4. 验证所有角色的访问权限
    for (UUID roleId : roleIds) {
        SysRole role = roleMapper.selectById(roleId);
        if (role == null) {
            throw new BusinessException("ROLE_NOT_FOUND", "角色不存在: " + roleId);
        }

        // 验证角色是否可访问（平台角色或本租户角色）
        TenantValidationUtil.validateRoleAccess(role.getTenantId());

        // 检查是否可以分配该角色（角色等级检查）
        SysUser currentUser = userMapper.selectById(currentUserId);
        Integer currentUserMaxRoleLevel = getMaxRoleLevel(currentUserId);
        PermissionChecker.requireRoleAssignmentPermission(currentUserId, currentUserMaxRoleLevel, role.getRoleLevel());
    }

    // 5. 删除原有角色
    userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>()
        .eq(SysUserRole::getUserId, userId));

    // 6. 分配新角色
    for (UUID roleId : roleIds) {
        SysUserRole userRole = new SysUserRole();
        userRole.setUserId(userId);
        userRole.setRoleId(roleId);
        // tenant_id 由触发器自动填充
        userRoleMapper.insert(userRole);
    }

    // 7. 清除权限缓存
    clearUserPermissionCache(userId);

    // 8. 记录操作日志
    TenantValidationUtil.logTenantOperation("GRANT_ROLES", "SysUser", userId);

    log.info("租户 {} 为用户 {} 分配角色: {}", currentTenantId, userId, roleIds);
}
```

---

## 3. SysRoleService 改造示例

### 3.1 创建角色（addRole）

**改造后**：

```java
@Transactional
public void addRole(RoleDTO roleDTO) {
    // 1. 验证租户上下文
    TenantValidationUtil.validateTenantContext();
    UUID currentTenantId = TenantValidationUtil.getRequiredTenantId();
    UUID currentUserId = SecurityUtils.getCurrentUserId();

    // 2. 检查权限
    PermissionChecker.requirePermission(currentUserId, "BTN_ROLE_CREATE");

    // 3. 判断是否创建平台角色
    if (roleDTO.getRoleType() == null) {
        roleDTO.setRoleType("TENANT_ROLE"); // 默认为租户角色
    }

    if ("PLATFORM_ROLE".equals(roleDTO.getRoleType())) {
        // 只有平台管理员可以创建平台角色
        SysUser currentUser = userMapper.selectById(currentUserId);
        TenantValidationUtil.validatePlatformResourceCreation(currentUser.getUserType());
    }

    // 4. 创建角色
    SysRole role = new SysRole();
    BeanUtils.copyProperties(roleDTO, role);
    role.setId(UUIDv7Util.generate());

    if ("PLATFORM_ROLE".equals(roleDTO.getRoleType())) {
        role.setTenantId(null); // 平台角色 tenant_id 为 NULL
    } else {
        role.setTenantId(currentTenantId); // 租户角色
    }

    // 5. 保存角色
    roleMapper.insert(role);

    // 6. 记录操作日志
    TenantValidationUtil.logTenantOperation("CREATE_ROLE", "SysRole", role.getId());

    log.info("创建角色成功: roleId={}, roleType={}, tenantId={}",
        role.getId(), role.getRoleType(), role.getTenantId());
}
```

### 3.2 分配权限给角色（assignPermissions）

**改造后**：

```java
@Transactional
public void assignPermissions(UUID roleId, List<UUID> permissionIds) {
    // 1. 验证租户上下文
    TenantValidationUtil.validateTenantContext();
    UUID currentTenantId = TenantValidationUtil.getRequiredTenantId();
    UUID currentUserId = SecurityUtils.getCurrentUserId();

    // 2. 检查权限
    PermissionChecker.requirePermission(currentUserId, "BTN_ROLE_ASSIGN_PERMISSION");

    // 3. 验证角色访问权限
    SysRole role = roleMapper.selectById(roleId);
    if (role == null) {
        throw new BusinessException("ROLE_NOT_FOUND", "角色不存在");
    }
    TenantValidationUtil.validateRoleAccess(role.getTenantId());

    // 4. 验证所有权限的访问权限
    for (UUID permissionId : permissionIds) {
        SysPermission permission = permissionMapper.selectById(permissionId);
        if (permission == null) {
            throw new BusinessException("PERMISSION_NOT_FOUND", "权限不存在: " + permissionId);
        }

        // 验证权限是否可访问（平台权限或本租户权限）
        TenantValidationUtil.validatePermissionAccess(permission.getTenantId());
    }

    // 5. 删除原有权限
    rolePermissionMapper.delete(new LambdaQueryWrapper<SysRolePermission>()
        .eq(SysRolePermission::getRoleId, roleId));

    // 6. 分配新权限
    for (UUID permissionId : permissionIds) {
        SysRolePermission rolePermission = new SysRolePermission();
        rolePermission.setRoleId(roleId);
        rolePermission.setPermissionId(permissionId);
        // tenant_id 由触发器自动填充
        rolePermissionMapper.insert(rolePermission);
    }

    // 7. 清除所有拥有该角色用户的权限缓存
    clearRoleUsersPermissionCache(roleId);

    // 8. 记录操作日志
    TenantValidationUtil.logTenantOperation("ASSIGN_PERMISSIONS", "SysRole", roleId);

    log.info("为角色 {} 分配权限: {}", roleId, permissionIds);
}
```

---

## 4. SysPermissionService 改造示例

### 4.1 创建权限（addPermission）

**改造后**：

```java
@Transactional
public void addPermission(PermissionDTO permissionDTO) {
    // 1. 验证租户上下文
    TenantValidationUtil.validateTenantContext();
    UUID currentTenantId = TenantValidationUtil.getRequiredTenantId();
    UUID currentUserId = SecurityUtils.getCurrentUserId();

    // 2. 检查权限
    PermissionChecker.requirePermission(currentUserId, "BTN_PERMISSION_CREATE");

    // 3. 判断是否创建平台权限
    if (permissionDTO.getPermissionScope() == null) {
        permissionDTO.setPermissionScope("TENANT"); // 默认为租户权限
    }

    if ("PLATFORM".equals(permissionDTO.getPermissionScope())) {
        // 只有平台管理员可以创建平台权限
        SysUser currentUser = userMapper.selectById(currentUserId);
        TenantValidationUtil.validatePlatformResourceCreation(currentUser.getUserType());
    }

    // 4. 创建权限
    SysPermission permission = new SysPermission();
    BeanUtils.copyProperties(permissionDTO, permission);
    permission.setId(UUIDv7Util.generate());

    if ("PLATFORM".equals(permissionDTO.getPermissionScope())) {
        permission.setTenantId(null); // 平台权限 tenant_id 为 NULL
    } else {
        permission.setTenantId(currentTenantId); // 租户权限
    }

    // 5. 保存权限
    permissionMapper.insert(permission);

    // 6. 记录操作日志
    TenantValidationUtil.logTenantOperation("CREATE_PERMISSION", "SysPermission", permission.getId());

    log.info("创建权限成功: permissionId={}, permissionScope={}, tenantId={}",
        permission.getId(), permission.getPermissionScope(), permission.getTenantId());
}
```

---

## 5. SysDeptService 改造示例

### 5.1 创建部门（addDept）

**改造后**：

```java
@Transactional
public void addDept(DeptDTO deptDTO) {
    // 1. 验证租户上下文
    TenantValidationUtil.validateTenantContext();
    UUID currentTenantId = TenantValidationUtil.getRequiredTenantId();
    UUID currentUserId = SecurityUtils.getCurrentUserId();

    // 2. 检查权限
    PermissionChecker.requirePermission(currentUserId, "BTN_DEPT_CREATE");

    // 3. 验证父部门（如果有）
    Integer deptLevel = 1;
    String deptPath = "";

    if (deptDTO.getParentId() != null) {
        SysDept parentDept = deptMapper.selectById(deptDTO.getParentId());
        if (parentDept == null) {
            throw new BusinessException("PARENT_DEPT_NOT_FOUND", "父部门不存在");
        }

        // 验证父部门属于当前租户
        TenantValidationUtil.validateDepartmentOwnership(parentDept.getTenantId());

        deptLevel = parentDept.getDeptLevel() + 1;
        deptPath = parentDept.getDeptPath();
    }

    // 4. 创建部门
    SysDept dept = new SysDept();
    BeanUtils.copyProperties(deptDTO, dept);
    dept.setId(UUIDv7Util.generate());
    dept.setTenantId(currentTenantId);
    dept.setDeptLevel(deptLevel);
    dept.setDeptPath(deptPath + "/" + dept.getId());

    // 5. 保存部门
    deptMapper.insert(dept);

    // 6. 记录操作日志
    TenantValidationUtil.logTenantOperation("CREATE_DEPT", "SysDept", dept.getId());

    log.info("租户 {} 创建部门成功: deptId={}, deptPath={}", currentTenantId, dept.getId(), dept.getDeptPath());
}
```

### 5.2 查询部门树（getDeptTree）

**改造后**：

```java
@Transactional(readOnly = true)
public List<DeptTreeVO> getDeptTree() {
    // 1. 验证租户上下文
    TenantValidationUtil.validateTenantContext();
    UUID currentTenantId = TenantValidationUtil.getRequiredTenantId();

    // 2. 查询租户的所有部门（TenantInterceptor 会自动注入 tenant_id）
    List<SysDept> allDepts = deptMapper.selectList(
        new LambdaQueryWrapper<SysDept>()
            .orderByAsc(SysDept::getDeptLevel, SysDept::getSortOrder)
    );

    // 3. 构建树形结构
    List<DeptTreeVO> tree = buildDeptTree(allDepts, null);

    return tree;
}

private List<DeptTreeVO> buildDeptTree(List<SysDept> allDepts, UUID parentId) {
    return allDepts.stream()
        .filter(dept -> Objects.equals(dept.getParentId(), parentId))
        .map(dept -> {
            DeptTreeVO node = new DeptTreeVO();
            BeanUtils.copyProperties(dept, node);
            node.setChildren(buildDeptTree(allDepts, dept.getId()));
            return node;
        })
        .collect(Collectors.toList());
}
```

---

## 6. 改造总结

### 6.1 改造要点

所有 Service 方法都应遵循以下模式：

```java
public void serviceMethod(...) {
    // 1. 验证租户上下文
    TenantValidationUtil.validateTenantContext();
    UUID currentTenantId = TenantValidationUtil.getRequiredTenantId();
    UUID currentUserId = SecurityUtils.getCurrentUserId();

    // 2. 检查操作权限
    PermissionChecker.requirePermission(currentUserId, "权限编码");

    // 3. 查询数据
    Entity entity = mapper.selectById(id);

    // 4. 验证数据归属
    TenantValidationUtil.validateDataOwnership(entity.getTenantId());

    // 5. 数据权限检查（如果需要）
    String dataScope = PermissionChecker.getUserDataScope(currentUserId);
    if (!PermissionChecker.canOperateResource(...)) {
        throw new BusinessException("DATA_ACCESS_DENIED", "无权操作");
    }

    // 6. 执行业务逻辑
    // ...

    // 7. 记录操作日志
    TenantValidationUtil.logTenantOperation("操作类型", "资源类型", resourceId);

    // 8. 日志记录
    log.info("操作成功: ...");
}
```

### 6.2 平台管理员特殊处理

平台管理员需要访问特定租户数据时：

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
        // ...
        return null;
    });
}
```

### 6.3 查询方法优化

查询方法不需要显式添加 `tenant_id` 条件，`TenantInterceptor` 会自动注入：

```java
// 原始查询
List<SysUser> users = userMapper.selectList(
    new LambdaQueryWrapper<SysUser>()
        .eq(SysUser::getStatus, 1)
);

// TenantInterceptor 自动注入后实际执行的 SQL：
// SELECT * FROM sys_user WHERE status = 1 AND tenant_id = '<current-tenant-id>' AND NOT deleted
```

---

## 7. 配置建议

### 7.1 启用方法级安全注解

在 Spring Security 配置类中启用：

```java
@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig {
    // ...
}
```

### 7.2 使用注解简化权限检查

```java
// 方法级权限检查
@PreAuthorize("hasPermission(null, 'BTN_USER_CREATE')")
public void addUser(UserDTO userDTO) {
    // ...
}

// 角色检查
@PreAuthorize("hasRole('TENANT_ADMIN')")
public void adminOperation() {
    // ...
}
```

---

## 8. 测试建议

### 8.1 单元测试

```java
@Test
public void testAddUser_WithTenantContext() {
    // 设置租户上下文
    UUID testTenantId = UUID.fromString("...");
    TenantContextHolder.setTenantId(testTenantId);

    try {
        // 创建用户
        UserDTO userDTO = new UserDTO();
        userDTO.setUsername("test_user");

        userService.addUser(userDTO);

        // 验证用户的 tenant_id 正确
        SysUser createdUser = userMapper.selectOne(
            new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, "test_user")
        );
        assertEquals(testTenantId, createdUser.getTenantId());
    } finally {
        TenantContextHolder.clear();
    }
}

@Test(expected = BusinessException.class)
public void testAddUser_WithoutTenantContext() {
    // 不设置租户上下文，应该抛出异常
    TenantContextHolder.clear();

    UserDTO userDTO = new UserDTO();
    userDTO.setUsername("test_user");

    userService.addUser(userDTO); // 应该抛出 TENANT_CONTEXT_MISSING 异常
}
```

---

**文档版本**：v1.0
**最后更新**：2025-01-24
**作者**：Claude Code
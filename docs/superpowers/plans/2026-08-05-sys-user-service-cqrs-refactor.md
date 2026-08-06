# SysUserService CQRS Refactor Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Split the 766-line `SysUserServiceImpl` into three CQRS-aligned services (query / command / role-command) plus a shared cross-cutting template, with tenant-scoped cache keys and explicit `@Slave` on every read.

**Architecture:** Hard split — delete `ISysUserService` / `SysUserServiceImpl`, replace with `ISysUserQueryService` + `ISysUserCommandService` + `IUserRoleCommandService` under `service/query/` and `service/command/`. A new `UserCommandTemplate` encapsulates the repeated 8-step boilerplate (tenant → permission → load → 404 → ownership → data-scope → body → audit log). All caches switch to tenant-scoped key generation except the auth-path `userDetails` cache (which stays username-keyed to preserve cross-tenant login lookup).

**Tech Stack:** Java 21, Spring Boot 4.0.6, MyBatis-Plus 3.5.15, Spring Cache with `TenantAwareCacheKeyGenerator`, Lombok, baomidou `@DS` / `@Master` / `@Slave`.

**Note on TDD:** The user explicitly opted out of new test coverage. Each task verifies by `mvn compile` only — no failing-test-first cycle. Existing tests must continue to pass at the end.

---

## File Map

| Layer | Path |
|---|---|
| Spec | `docs/superpowers/specs/2026-08-05-sys-user-service-cqrs-refactor-design.md` |
| Plan | `docs/superpowers/plans/2026-08-05-sys-user-service-cqrs-refactor.md` |
| Build | `com.scm.parent/pom.xml` — always `-f com.scm.parent/pom.xml` |

---

## Task 1: Add `ResultCode` entries for password errors

**Files:**
- Modify: `scm-common/core/src/main/java/com/scmcloud/common/response/ResultCode.java`

- [ ] **Step 1: Insert two new entries after `USER_CANNOT_DELETE_SELF` (line 39)**

Edit the enum body to add, between the existing line 39 (`USER_CANNOT_DELETE_SELF`) and line 40 (`USER_NEED_LOGIN`):

```java
    USER_PASSWORD_INCORRECT_OLD(1010, "Old Password Incorrect"),
    USER_PASSWORD_SAME_AS_OLD(1011, "New Password Must Differ From Old"),
```

- [ ] **Step 2: Verify compile**

Run:
```bash
mvn clean install -pl scm-common/core -am -DskipTests -f com.scm.parent/pom.xml
```
Expected: BUILD SUCCESS.

- [ ] **Step 3: Commit**

```bash
git add scm-common/core/src/main/java/com/scmcloud/common/response/ResultCode.java
git commit -m "feat(system): add ResultCode entries for password validation"
```

---

## Task 2: Add `UserStatus` enum

**Files:**
- Create: `scm-system/service/src/main/java/com/scmcloud/system/domain/enums/UserStatus.java`

- [ ] **Step 1: Create the enum file**

```java
package com.scmcloud.system.domain.enums;

import java.util.Arrays;

/**
 * User account status code mapping.
 *
 * <p>Replaces the ad-hoc int/0/1/2 literals scattered across the user service.</p>
 */
public enum UserStatus {

    INACTIVE(0),
    ACTIVE(1),
    LOCKED(2);

    private final int code;

    UserStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static UserStatus fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        return Arrays.stream(values())
                .filter(s -> s.code == code)
                .findFirst()
                .orElse(null);
    }
}
```

- [ ] **Step 2: Verify compile**

```bash
mvn clean install -pl scm-system/service -am -DskipTests -f com.scm.parent/pom.xml
```
Expected: BUILD SUCCESS.

- [ ] **Step 3: Commit**

```bash
git add scm-system/service/src/main/java/com/scmcloud/system/domain/enums/UserStatus.java
git commit -m "feat(system): add UserStatus enum"
```

---

## Task 3: Add admin user id config property

**Files:**
- Modify: `scm-system/service/src/main/resources/application.yml`

- [ ] **Step 1: Find the application's properties file**

Run:
```bash
Get-ChildItem -Path scm-system/service/src/main/resources -Recurse -Filter application*.yml -ErrorAction SilentlyContinue
```

If no `application*.yml` exists in that directory, this task becomes a no-op — skip the rest of this task. The admin id will default to the existing hardcoded UUID via a `@Value` default.

- [ ] **Step 2: If a properties file exists, append the property**

Add at the bottom of the file (preserve all existing content):

```yaml
app:
  security:
    admin-user-id: 019a0aee-3b74-7bfc-b34f-48b5428d4875
```

- [ ] **Step 3: Commit (only if Step 2 added content)**

```bash
git add scm-system/service/src/main/resources/application.yml
git commit -m "config(system): add app.security.admin-user-id property"
```

Otherwise skip the commit.

---

## Task 4: Create `UserCommandTemplate` helper

**Files:**
- Create: `scm-system/service/src/main/java/com/scmcloud/system/service/command/UserCommandTemplate.java`

- [ ] **Step 1: Create the file with the boilerplate helper**

```java
package com.scmcloud.system.service.command;

import com.scmcloud.common.exception.BusinessException;
import com.scmcloud.common.response.ResultCode;
import com.scmcloud.common.security.PermissionChecker;
import com.scmcloud.common.tenant.TenantValidationUtil;
import com.scmcloud.common.web.util.SecurityUtils;
import com.scmcloud.system.domain.entity.SysUser;
import com.scmcloud.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.function.BiConsumer;

/**
 * Cross-cutting template for user command operations.
 *
 * <p>Encapsulates the repeated 8-step boilerplate shared by most user write methods:</p>
 * <ol>
 *   <li>Validate tenant context</li>
 *   <li>Resolve current operator UUID</li>
 *   <li>Check operator permission</li>
 *   <li>Load the target user (404 if absent)</li>
 *   <li>Validate tenant ownership</li>
 *   <li>Check operator's data scope against target</li>
 *   <li>Run caller-provided mutation body</li>
 *   <li>Write tenant operation audit log</li>
 * </ol>
 *
 * <p>Callers are still responsible for: business-rule validation, persistence
 * (save/update/delete), event publishing, and the final info log line.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserCommandTemplate {

    private final SysUserMapper userMapper;
    private final PermissionChecker permissionChecker;

    /**
     * Execute a user command that mutates an existing user.
     *
     * @param operation         audit log operation name (e.g. "UPDATE", "DELETE")
     * @param operatorPermission permission code required from current operator
     * @param userId            target user id
     * @param body              mutation body; receives the loaded user and operator id
     * @return the loaded SysUser (so the caller can read fields like username for logging)
     */
    public SysUser execute(String operation,
                           String operatorPermission,
                           UUID userId,
                           BiConsumer<SysUser, UUID> body) {
        TenantValidationUtil.getRequiredTenantId();
        UUID operatorId = SecurityUtils.getCurrentUserUuid().orElse(null);
        permissionChecker.requirePermission(operatorId, operatorPermission);

        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND.getCode(),
                    ResultCode.USER_NOT_FOUND.getMessage());
        }

        TenantValidationUtil.validateDataOwnership(user.getTenantId());

        String dataScope = permissionChecker.getUserDataScope(operatorId);
        if (permissionChecker.cannotOperateResource(operatorId, user.getCreateBy(),
                user.getDeptId(), dataScope)) {
            throw new BusinessException(ResultCode.DATA_ACCESS_DENIED.getCode(),
                    "无权操作该用户数据");
        }

        body.accept(user, operatorId);

        TenantValidationUtil.logTenantOperation(operation, "USER", userId);
        return user;
    }
}
```

- [ ] **Step 2: Verify compile**

```bash
mvn clean install -pl scm-system/service -am -DskipTests -f com.scm.parent/pom.xml
```
Expected: BUILD SUCCESS.

- [ ] **Step 3: Commit**

```bash
git add scm-system/service/src/main/java/com/scmcloud/system/service/command/UserCommandTemplate.java
git commit -m "feat(system): add UserCommandTemplate cross-cutting helper"
```

---

## Task 5: Create `ISysUserQueryService` interface

**Files:**
- Create: `scm-system/service/src/main/java/com/scmcloud/system/service/query/ISysUserQueryService.java`

- [ ] **Step 1: Create the query interface**

```java
package com.scmcloud.system.service.query;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.common.dto.user.UserDTO;
import com.scmcloud.common.dto.user.UserInfo;
import com.scmcloud.common.web.domain.SecurityUser;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Query-side service for {@link com.scmcloud.system.domain.entity.SysUser}.
 *
 * <p>All methods are read-only. Implementations must be annotated with
 * {@link com.scmcloud.common.data.rw.annotation.Slave}.</p>
 */
public interface ISysUserQueryService {

    Page<UserDTO> listUsers(Integer pageNum, Integer pageSize, String username, Integer status);

    SecurityUser getUserByUsername(String username);

    UserDTO getUserById(UUID id);

    UserInfo getUserInfo(UUID userId);

    List<Map<String, Object>> getUserTemporaryRoles(UUID userId);

    boolean canAccessDept(UUID userId, UUID deptId);

    Integer getUserDataScope(UUID userId);

    Map<String, Object> getUserStatistics(UUID userId);
}
```

- [ ] **Step 2: Verify compile**

```bash
mvn clean install -pl scm-system/service -am -DskipTests -f com.scm.parent/pom.xml
```
Expected: BUILD SUCCESS.

- [ ] **Step 3: Commit**

```bash
git add scm-system/service/src/main/java/com/scmcloud/system/service/query/ISysUserQueryService.java
git commit -m "feat(system): add ISysUserQueryService interface"
```

---

## Task 6: Implement `SysUserQueryServiceImpl`

**Files:**
- Create: `scm-system/service/src/main/java/com/scmcloud/system/service/query/SysUserQueryServiceImpl.java`

- [ ] **Step 1: Create the query implementation**

```java
package com.scmcloud.system.service.query;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.common.cache.TenantAwareCacheKeyGenerator;
import com.scmcloud.common.data.rw.annotation.Slave;
import com.scmcloud.common.dto.permission.PermissionDTO;
import com.scmcloud.common.dto.user.UserDTO;
import com.scmcloud.common.dto.user.UserInfo;
import com.scmcloud.common.exception.BusinessException;
import com.scmcloud.common.response.ResultCode;
import com.scmcloud.common.security.PermissionChecker;
import com.scmcloud.common.tenant.TenantValidationUtil;
import com.scmcloud.common.web.domain.SecurityUser;
import com.scmcloud.common.web.util.SecurityUtils;
import com.scmcloud.system.domain.entity.SysUser;
import com.scmcloud.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Read-side service implementation. All methods route to a slave database via {@link Slave}.
 * Caches (except {@code userDetails}) use {@link TenantAwareCacheKeyGenerator} for tenant
 * isolation. The {@code userDetails} cache stays username-keyed because authentication
 * happens before the tenant context is established.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysUserQueryServiceImpl implements ISysUserQueryService {

    private static final String KEY_GEN = "tenantAwareCacheKeyGenerator";

    private final SysUserMapper userMapper;
    private final UserCrossDatabaseQueryService userQueryService;
    private final RoleCrossDatabaseQueryService roleQueryService;
    private final DeptCrossDatabaseQueryService deptQueryService;
    private final PermissionCrossDatabaseQueryService permissionQueryService;
    private final PermissionChecker permissionChecker;

    @Override
    @Slave
    @Transactional(readOnly = true)
    public Page<UserDTO> listUsers(Integer pageNum, Integer pageSize,
                                   String username, Integer status) {
        UUID tenantId = TenantValidationUtil.getRequiredTenantId();
        UUID operatorId = SecurityUtils.getCurrentUserUuid().orElse(null);
        String dataScope = permissionChecker.getUserDataScope(operatorId);

        Page<SysUser> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(username != null && !username.isEmpty(), SysUser::getUsername, username)
                .eq(status != null, SysUser::getStatus, status);

        if (!"ALL".equals(dataScope)) {
            List<UUID> accessibleDeptIds =
                    permissionChecker.getAccessibleDepartmentIds(operatorId, tenantId);
            if ("SELF".equals(dataScope)) {
                wrapper.eq(SysUser::getCreateBy, operatorId);
            } else if (!accessibleDeptIds.isEmpty()) {
                wrapper.in(SysUser::getDeptId, accessibleDeptIds);
            } else {
                return new Page<>(pageNum, pageSize, 0);
            }
        }

        wrapper.orderByDesc(SysUser::getCreateTime);

        Page<SysUser> userPage = userMapper.selectPage(page, wrapper);

        Page<UserDTO> dtoPage = new Page<>(pageNum, pageSize, userPage.getTotal());
        List<UserDTO> dtos = userPage.getRecords().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        dtoPage.setRecords(dtos);
        return dtoPage;
    }

    @Override
    @Slave
    @Cacheable(value = "userDetails", key = "#username")
    public SecurityUser getUserByUsername(String username) {
        SysUser user = userMapper.findByUsername(username);
        if (user == null) {
            return null;
        }
        Set<String> roles = userQueryService.findRoleCodesByUserId(user.getId());
        Set<String> permissions = userQueryService.findPermissionCodesByUserId(user.getId());
        return SecurityUser.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .password(user.getPassword())
                .realName(user.getRealName())
                .deptId(user.getDeptId())
                .status(user.getStatus())
                .accountType(user.getAccountType())
                .userLevel(user.getUserLevel())
                .roles(roles != null ? roles : Collections.emptySet())
                .permissions(permissions != null ? permissions : Collections.emptySet())
                .twoFactorSecret(user.getTwoFactorSecret())
                .twoFactorEnabled(user.getTwoFactorEnabled())
                .passwordExpireTime(user.getPasswordExpireTime())
                .forceChangePassword(user.getForceChangePassword())
                .build();
    }

    @Override
    @Slave
    @Cacheable(value = "user", keyGenerator = KEY_GEN)
    public UserDTO getUserById(UUID id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND.getCode(),
                    ResultCode.USER_NOT_FOUND.getMessage());
        }
        UserDTO dto = convertToDTO(user);
        List<Map<String, Object>> roles = userQueryService.findUserRolesWithNames(id);
        if (!roles.isEmpty()) {
            List<UUID> roleIds = roles.stream()
                    .map(r -> (UUID) r.get("id"))
                    .toList();
            List<String> roleNames = roles.stream()
                    .map(r -> (String) r.get("name"))
                    .toList();
            dto.setRoleIds(roleIds);
            dto.setRoleNames(roleNames);
        }
        return dto;
    }

    @Override
    @Slave
    @Cacheable(value = "userInfo", keyGenerator = KEY_GEN)
    public UserInfo getUserInfo(UUID userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND.getCode(),
                    ResultCode.USER_NOT_FOUND.getMessage());
        }
        UserInfo info = UserInfo.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .avatar(user.getAvatar())
                .email(user.getEmail())
                .phone(user.getPhone())
                .deptId(user.getDeptId())
                .userLevel(user.getUserLevel())
                .build();

        Set<String> roles = userQueryService.findRoleCodesByUserId(userId);
        Set<String> permissions = userQueryService.findPermissionCodesByUserId(userId);
        info.setRoles(roles);
        info.setPermissions(permissions);

        List<PermissionDTO> menuTree = permissionQueryService.findMenuTreeByUserId(userId);
        info.setMenuTree(new HashSet<>(menuTree));
        return info;
    }

    @Override
    @Slave
    @Cacheable(value = "userTemporaryRoles", keyGenerator = KEY_GEN, unless = "#result.isEmpty()")
    public List<Map<String, Object>> getUserTemporaryRoles(UUID userId) {
        return userQueryService.findTemporaryRolesByUserId(userId);
    }

    @Override
    @Slave
    public boolean canAccessDept(UUID userId, UUID deptId) {
        return deptQueryService.hasAccessToDept(userId, deptId);
    }

    @Override
    @Slave
    @Cacheable(value = "userDataScope", keyGenerator = KEY_GEN)
    public Integer getUserDataScope(UUID userId) {
        return userQueryService.getUserDataScope(userId);
    }

    @Override
    @Slave
    public Map<String, Object> getUserStatistics(UUID userId) {
        Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("roleCount", userQueryService.countUserRoles(userId));
        stats.put("temporaryRoleCount", userQueryService.countTemporaryRoles(userId));
        stats.put("expiringRoleCount", userQueryService.countExpiringRoles(userId, 7));
        stats.put("dataScope", userQueryService.getUserDataScope(userId));
        stats.put("maxApprovalAmount", userQueryService.getMaxApprovalAmount(userId));
        return stats;
    }

    private UserDTO convertToDTO(SysUser user) {
        UserDTO dto = new UserDTO();
        BeanUtils.copyProperties(user, dto);
        return dto;
    }
}
```

- [ ] **Step 2: Verify compile**

```bash
mvn clean install -pl scm-system/service -am -DskipTests -f com.scm.parent/pom.xml
```
Expected: BUILD SUCCESS. The old `ISysUserService` still exists, but `SysUserQueryServiceImpl` only implements the new interface — no conflict.

- [ ] **Step 3: Commit**

```bash
git add scm-system/service/src/main/java/com/scmcloud/system/service/query/SysUserQueryServiceImpl.java
git commit -m "feat(system): implement SysUserQueryServiceImpl with tenant-scoped caches"
```

---

## Task 7: Create `ISysUserCommandService` interface

**Files:**
- Create: `scm-system/service/src/main/java/com/scmcloud/system/service/command/ISysUserCommandService.java`

- [ ] **Step 1: Create the command interface**

```java
package com.scmcloud.system.service.command;

import com.scmcloud.common.dto.user.UserDTO;

import java.util.List;
import java.util.UUID;

/**
 * Command-side service for {@link com.scmcloud.system.domain.entity.SysUser}.
 *
 * <p>All methods are mutating. Implementations must run inside a write transaction
 * and must evict tenant-scoped cache entries after mutation.</p>
 */
public interface ISysUserCommandService {

    void addUser(UserDTO userDTO);

    void updateUser(UserDTO userDTO);

    void deleteUser(UUID id);

    String resetPassword(UUID id);

    void changePassword(UUID userId, String oldPassword, String newPassword);

    void grantRoles(UUID userId, List<UUID> roleIds);

    void lockUser(UUID id, Boolean lock);

    void updateLastLogin(UUID userId, String ipAddress);
}
```

- [ ] **Step 2: Verify compile**

```bash
mvn clean install -pl scm-system/service -am -DskipTests -f com.scm.parent/pom.xml
```
Expected: BUILD SUCCESS.

- [ ] **Step 3: Commit**

```bash
git add scm-system/service/src/main/java/com/scmcloud/system/service/command/ISysUserCommandService.java
git commit -m "feat(system): add ISysUserCommandService interface"
```

---

## Task 8: Implement `SysUserCommandServiceImpl`

**Files:**
- Create: `scm-system/service/src/main/java/com/scmcloud/system/service/command/SysUserCommandServiceImpl.java`

- [ ] **Step 1: Create the command implementation**

```java
package com.scmcloud.system.service.command;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.scmcloud.common.dto.user.UserDTO;
import com.scmcloud.common.exception.BusinessException;
import com.scmcloud.common.response.ResultCode;
import com.scmcloud.common.security.PermissionChecker;
import com.scmcloud.common.status.StatusValidator;
import com.scmcloud.common.tenant.TenantValidationUtil;
import com.scmcloud.common.util.UUIDv7Util;
import com.scmcloud.common.web.util.SecurityUtils;
import com.scmcloud.system.domain.entity.SysUser;
import com.scmcloud.system.domain.enums.UserStatus;
import com.scmcloud.system.event.DataSyncEventPublisher;
import com.scmcloud.system.mapper.SysUserMapper;
import com.scmcloud.system.service.query.RoleCrossDatabaseQueryService;
import com.scmcloud.system.service.query.UserCrossDatabaseQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Write-side service for {@link SysUser}. Uses {@link UserCommandTemplate} to
 * encapsulate the standard 8-step boilerplate for commands that mutate an existing
 * user (updateUser, deleteUser, grantRoles). {@code addUser}, {@code resetPassword},
 * {@code changePassword}, {@code lockUser}, and {@code updateLastLogin} follow
 * simpler flows and inline their checks.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysUserCommandServiceImpl implements ISysUserCommandService {

    private static final String KEY_GEN = "tenantAwareCacheKeyGenerator";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String PASSWORD_CHARS =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789@#$%";

    private final SysUserMapper userMapper;
    private final UserCrossDatabaseQueryService userQueryService;
    private final RoleCrossDatabaseQueryService roleQueryService;
    private final UserRoleCrossDatabaseCommandService userRoleCommandService;
    private final PasswordEncoder passwordEncoder;
    private final DataSyncEventPublisher dataSyncEventPublisher;
    private final PermissionChecker permissionChecker;
    private final StatusValidator statusValidator;
    private final UserCommandTemplate commandTemplate;

    @Value("${app.security.admin-user-id:019a0aee-3b74-7bfc-b34f-48b5428d4875}")
    private String adminUserId;

    @Value("${spring.security.default-password}")
    private String defaultPassword;

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = {"user", "userDetails", "userInfo"}, allEntries = true)
    public void addUser(UserDTO userDTO) {
        UUID tenantId = TenantValidationUtil.getRequiredTenantId();
        UUID operatorId = SecurityUtils.getCurrentUserUuid().orElse(null);
        permissionChecker.requirePermission(operatorId, "user:add");

        if (userMapper.existsByUsername(userDTO.getUsername())) {
            throw new BusinessException(ResultCode.USER_EXIST.getCode(),
                    ResultCode.USER_EXIST.getMessage());
        }

        String encoded = (userDTO.getPassword() != null && !userDTO.getPassword().isEmpty())
                ? passwordEncoder.encode(userDTO.getPassword())
                : passwordEncoder.encode(defaultPassword);

        SysUser user = new SysUser();
        BeanUtils.copyProperties(userDTO, user);
        user.setPassword(encoded);
        user.setId(UUIDv7Util.generate());
        user.setTenantId(tenantId);
        user.setPasswordExpireTime(LocalDateTime.now().plusDays(90));
        user.setForceChangePassword(true);

        userMapper.insert(user);

        if (userDTO.getRoleIds() != null && !userDTO.getRoleIds().isEmpty()) {
            int inserted = userRoleCommandService.batchInsertUserRoles(
                    user.getId(), userDTO.getRoleIds(), operatorId);
            log.debug("创建用户时分配角色: user={}, roleCount={}", user.getUsername(), inserted);
        }

        dataSyncEventPublisher.publishUserCreated(user);
        TenantValidationUtil.logTenantOperation("CREATE", "USER", user.getId());

        log.info("用户创建成功: username={}, operator={}", user.getUsername(),
                SecurityUtils.getCurrentUsername());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = {"user", "userDetails", "userInfo"}, keyGenerator = KEY_GEN)
    public void updateUser(UserDTO userDTO) {
        UUID operatorId = SecurityUtils.getCurrentUserUuid().orElse(null);
        SysUser user = commandTemplate.execute("UPDATE", "user:update", userDTO.getId(),
                (loaded, op) -> {
                    SysUser update = new SysUser();
                    BeanUtils.copyProperties(userDTO, update);
                    update.setPassword(null);
                    userMapper.updateById(update);

                    if (userDTO.getRoleIds() != null) {
                        int deleted = userRoleCommandService.deleteUserRoles(loaded.getId());
                        log.debug("更新用户时清除旧角色: user={}, deletedCount={}",
                                loaded.getUsername(), deleted);
                        if (!userDTO.getRoleIds().isEmpty()) {
                            int inserted = userRoleCommandService.batchInsertUserRoles(
                                    loaded.getId(), userDTO.getRoleIds(), op);
                            log.debug("更新用户时重新分配角色: user={}, newRoleCount={}",
                                    loaded.getUsername(), inserted);
                        }
                    }
                });

        SysUser updated = userMapper.selectById(user.getId());
        dataSyncEventPublisher.publishUserUpdated(updated);

        log.info("用户更新成功: username={}, operator={}", user.getUsername(),
                SecurityUtils.getCurrentUsername());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = {"user", "userDetails", "userInfo"}, keyGenerator = KEY_GEN)
    public void deleteUser(UUID id) {
        SysUser user = commandTemplate.execute("DELETE", "user:delete", id,
                (loaded, op) -> {
                    UUID adminId = UUID.fromString(adminUserId);
                    if (loaded.getId().equals(adminId)) {
                        throw new BusinessException(
                                ResultCode.USER_CANNOT_DELETE_ADMIN.getCode(),
                                ResultCode.USER_CANNOT_DELETE_ADMIN.getMessage());
                    }
                    if (loaded.getId().equals(op)) {
                        throw new BusinessException(
                                ResultCode.USER_CANNOT_DELETE_SELF.getCode(),
                                ResultCode.USER_CANNOT_DELETE_SELF.getMessage());
                    }
                    userMapper.deleteById(loaded.getId());
                    dataSyncEventPublisher.publishUserDeleted(loaded.getId());
                });

        log.info("用户删除成功: username={}, operator={}", user.getUsername(),
                SecurityUtils.getCurrentUsername());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = {"user", "userDetails"}, keyGenerator = KEY_GEN)
    public String resetPassword(UUID id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND.getCode(),
                    ResultCode.USER_NOT_FOUND.getMessage());
        }
        TenantValidationUtil.validateDataOwnership(user.getTenantId());

        String newPassword = generateRandomPassword();
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setForceChangePassword(true);
        userMapper.updateById(user);

        log.info("Password reset for user: {}, by: {}",
                user.getUsername(), SecurityUtils.getCurrentUsername());
        return newPassword;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = {"user", "userDetails"}, keyGenerator = KEY_GEN)
    public void changePassword(UUID userId, String oldPassword, String newPassword) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND.getCode(),
                    ResultCode.USER_NOT_FOUND.getMessage());
        }
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException(
                    ResultCode.USER_PASSWORD_INCORRECT_OLD.getCode(),
                    ResultCode.USER_PASSWORD_INCORRECT_OLD.getMessage());
        }
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new BusinessException(
                    ResultCode.USER_PASSWORD_SAME_AS_OLD.getCode(),
                    ResultCode.USER_PASSWORD_SAME_AS_OLD.getMessage());
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setForceChangePassword(false);
        user.setLastPasswordChangeTime(LocalDateTime.now());
        user.setPasswordExpireTime(LocalDateTime.now().plusDays(90));
        userMapper.updateById(user);

        log.info("Password changed for user: {}", user.getUsername());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = {"user", "userDetails", "userInfo"}, keyGenerator = KEY_GEN)
    public void grantRoles(UUID userId, List<UUID> roleIds) {
        UUID operatorId = SecurityUtils.getCurrentUserUuid().orElse(null);
        SysUser user = commandTemplate.execute("GRANT_ROLES", "user:grant-role", userId,
                (loaded, op) -> {
                    if (roleIds != null && !roleIds.isEmpty()) {
                        Integer operatorMaxRoleLevel = userQueryService.getUserMaxRoleLevel(op);
                        for (UUID roleId : roleIds) {
                            Integer roleLevel = roleQueryService.getRoleLevel(roleId);
                            permissionChecker.requireRoleAssignmentPermission(
                                    op, operatorMaxRoleLevel, roleLevel);
                            UUID roleTenantId = roleQueryService.getRoleTenantId(roleId);
                            TenantValidationUtil.validateRoleAccess(roleTenantId);
                        }
                    }

                    int deleted = userRoleCommandService.deleteUserRoles(loaded.getId());
                    log.debug("授权操作清除原有角色: user={}, deletedCount={}",
                            loaded.getUsername(), deleted);
                    if (roleIds != null && !roleIds.isEmpty()) {
                        int inserted = userRoleCommandService.batchInsertUserRoles(
                                loaded.getId(), roleIds, op);
                        log.debug("授权操作分配新角色: user={}, grantedCount={}",
                                loaded.getUsername(), inserted);
                    }
                });

        log.info("角色授予操作完成: user={}, roleIds={}, operator={}",
                user.getUsername(), roleIds, SecurityUtils.getCurrentUsername());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = {"user", "userDetails"}, keyGenerator = KEY_GEN)
    public void lockUser(UUID id, Boolean lock) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND.getCode(),
                    ResultCode.USER_NOT_FOUND.getMessage());
        }

        UserStatus fromStatus = UserStatus.fromCode(user.getStatus());
        if (lock) {
            statusValidator.validateTransition("USER",
                    fromStatus != null ? fromStatus.name() : String.valueOf(user.getStatus()),
                    "LOCKED");
            user.setStatus(UserStatus.LOCKED.getCode());
            user.setLockedUntil(LocalDateTime.now().plusHours(24));
        } else {
            statusValidator.validateTransition("USER",
                    fromStatus != null ? fromStatus.name() : String.valueOf(user.getStatus()),
                    "ACTIVE");
            user.setStatus(UserStatus.ACTIVE.getCode());
            user.setLockedUntil(null);
            user.setLoginAttempts(0);
        }
        userMapper.updateById(user);

        log.info("User {} {}, by: {}",
                user.getUsername(), lock ? "locked" : "unlocked",
                SecurityUtils.getCurrentUsername());
    }

    @Override
    public void updateLastLogin(UUID userId, String ipAddress) {
        LambdaUpdateWrapper<SysUser> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SysUser::getId, userId)
                .set(SysUser::getLastLoginTime, LocalDateTime.now())
                .set(SysUser::getLastLoginIp, ipAddress);
        userMapper.update(null, wrapper);
    }

    private String generateRandomPassword() {
        StringBuilder pwd = new StringBuilder();
        pwd.append(PASSWORD_CHARS.charAt(RANDOM.nextInt(26)));
        pwd.append(PASSWORD_CHARS.charAt(26 + RANDOM.nextInt(26)));
        pwd.append(PASSWORD_CHARS.charAt(52 + RANDOM.nextInt(10)));
        pwd.append(PASSWORD_CHARS.charAt(62 + RANDOM.nextInt(4)));
        for (int i = 0; i < 8; i++) {
            pwd.append(PASSWORD_CHARS.charAt(RANDOM.nextInt(PASSWORD_CHARS.length())));
        }
        List<Character> chars = pwd.chars().mapToObj(c -> (char) c).collect(java.util.stream.Collectors.toList());
        Collections.shuffle(chars, RANDOM);
        return chars.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining());
    }
}
```

- [ ] **Step 2: Verify compile**

```bash
mvn clean install -pl scm-system/service -am -DskipTests -f com.scm.parent/pom.xml
```
Expected: BUILD SUCCESS. The old `ISysUserService` still exists; both the old and new implementations coexist temporarily.

- [ ] **Step 3: Commit**

```bash
git add scm-system/service/src/main/java/com/scmcloud/system/service/command/SysUserCommandServiceImpl.java
git commit -m "feat(system): implement SysUserCommandServiceImpl with UserCommandTemplate"
```

---

## Task 9: Create `IUserRoleCommandService` interface

**Files:**
- Create: `scm-system/service/src/main/java/com/scmcloud/system/service/command/IUserRoleCommandService.java`

- [ ] **Step 1: Create the role-command interface**

```java
package com.scmcloud.system.service.command;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Command-side service for the user-role association lifecycle, including
 * temporary role grants.
 *
 * <p>Backed by {@link UserRoleCrossDatabaseCommandService}; this is the user-facing
 * facade for {@code db_permission} mutations that don't load a {@code SysUser}
 * entity directly.</p>
 */
public interface IUserRoleCommandService {

    void grantTemporaryRoles(UUID userId, List<UUID> roleIds,
                             LocalDateTime effectiveTime, LocalDateTime expireTime);

    void extendTemporaryRole(UUID userId, UUID roleId, LocalDateTime newExpireTime);

    void terminateTemporaryRole(UUID userId, UUID roleId);
}
```

- [ ] **Step 2: Verify compile**

```bash
mvn clean install -pl scm-system/service -am -DskipTests -f com.scm.parent/pom.xml
```
Expected: BUILD SUCCESS.

- [ ] **Step 3: Commit**

```bash
git add scm-system/service/src/main/java/com/scmcloud/system/service/command/IUserRoleCommandService.java
git commit -m "feat(system): add IUserRoleCommandService interface"
```

---

## Task 10: Implement `UserRoleCommandServiceImpl`

**Files:**
- Create: `scm-system/service/src/main/java/com/scmcloud/system/service/command/UserRoleCommandServiceImpl.java`

- [ ] **Step 1: Create the role-command implementation**

```java
package com.scmcloud.system.service.command;

import com.scmcloud.common.exception.BusinessException;
import com.scmcloud.common.response.ResultCode;
import com.scmcloud.common.web.util.SecurityUtils;
import com.scmcloud.system.domain.entity.SysUser;
import com.scmcloud.system.mapper.SysUserMapper;
import com.scmcloud.system.service.query.UserCrossDatabaseQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * User-role lifecycle facade. Behaviour matches the original
 * {@code SysUserServiceImpl} methods; no permission or tenant checks are added
 * (pre-existing gaps are explicitly out of scope).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserRoleCommandServiceImpl implements IUserRoleCommandService {

    private static final String KEY_GEN = "tenantAwareCacheKeyGenerator";

    private final SysUserMapper userMapper;
    private final UserCrossDatabaseQueryService userQueryService;
    private final UserRoleCrossDatabaseCommandService userRoleCommandService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = {"user", "userDetails", "userInfo"}, keyGenerator = KEY_GEN)
    public void grantTemporaryRoles(UUID userId, List<UUID> roleIds,
                                    LocalDateTime effectiveTime, LocalDateTime expireTime) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND.getCode(),
                    ResultCode.USER_NOT_FOUND.getMessage());
        }

        if (expireTime != null && expireTime.isBefore(LocalDateTime.now())) {
            throw new BusinessException("过期时间不能早于当前时间");
        }
        if (effectiveTime != null && expireTime != null && effectiveTime.isAfter(expireTime)) {
            throw new BusinessException("生效时间不能晚于过期时间");
        }

        if (roleIds != null && !roleIds.isEmpty()) {
            UUID operatorId = SecurityUtils.getCurrentUserUuid().orElse(null);
            int inserted = userRoleCommandService.batchInsertTemporaryUserRoles(
                    userId,
                    roleIds,
                    effectiveTime != null ? effectiveTime : LocalDateTime.now(),
                    expireTime,
                    operatorId);
            log.debug("为用户 {} 授予 {} 个临时角色", user.getUsername(), inserted);
        }

        log.info("Temporary roles granted to user: {}, roles: {}, expireTime: {}, by: {}",
                user.getUsername(), roleIds, expireTime, SecurityUtils.getCurrentUsername());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = {"user", "userDetails", "userInfo"}, keyGenerator = KEY_GEN)
    public void extendTemporaryRole(UUID userId, UUID roleId, LocalDateTime newExpireTime) {
        if (!userQueryService.hasTemporaryRole(userId, roleId)) {
            throw new BusinessException("用户不存在该临时角色或已过期");
        }
        if (newExpireTime.isBefore(LocalDateTime.now())) {
            throw new BusinessException("新的过期时间不能早于当前时间");
        }

        int updated = userRoleCommandService.extendTemporaryRole(userId, roleId, newExpireTime);
        if (updated == 0) {
            throw new BusinessException("延长临时角色失败");
        }

        log.info("Temporary role extended: userId={}, roleId={}, newExpireTime={}, by={}",
                userId, roleId, newExpireTime, SecurityUtils.getCurrentUsername());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = {"user", "userDetails", "userInfo"}, keyGenerator = KEY_GEN)
    public void terminateTemporaryRole(UUID userId, UUID roleId) {
        int updated = userRoleCommandService.terminateTemporaryRole(userId, roleId);
        if (updated == 0) {
            throw new BusinessException("终止临时角色失败，可能该角色不存在或已过期");
        }

        log.info("Temporary role terminated: userId={}, roleId={}, by={}",
                userId, roleId, SecurityUtils.getCurrentUsername());
    }
}
```

- [ ] **Step 2: Verify compile**

```bash
mvn clean install -pl scm-system/service -am -DskipTests -f com.scm.parent/pom.xml
```
Expected: BUILD SUCCESS.

- [ ] **Step 3: Commit**

```bash
git add scm-system/service/src/main/java/com/scmcloud/system/service/command/UserRoleCommandServiceImpl.java
git commit -m "feat(system): implement UserRoleCommandServiceImpl"
```

---

## Task 11: Update `SysUserController` to use the new services

**Files:**
- Modify: `scm-system/service/src/main/java/com/scmcloud/system/controller/SysUserController.java`

- [ ] **Step 1: Replace the import on line 12**

Old:
```java
import com.scmcloud.system.service.ISysUserService;
```
New:
```java
import com.scmcloud.system.service.command.ISysUserCommandService;
import com.scmcloud.system.service.command.IUserRoleCommandService;
import com.scmcloud.system.service.query.ISysUserQueryService;
```

- [ ] **Step 2: Replace the field on line 35**

Old:
```java
    private final ISysUserService userService;
```
New:
```java
    private final ISysUserQueryService userQueryService;
    private final ISysUserCommandService userCommandService;
    private final IUserRoleCommandService userRoleCommandService;
```

- [ ] **Step 3: Update each controller method body to use the new field names**

For every `userService.listUsers/getUserById/addUser/updateUser/deleteUser/changePassword/resetPassword/grantRoles/lockUser/grantTemporaryRoles/extendTemporaryRole/terminateTemporaryRole/getUserTemporaryRoles/getUserStatistics/updateLastLogin`, replace with the corresponding call on the appropriate new service:

| Old call | New call |
|---|---|
| `userService.listUsers(...)` | `userQueryService.listUsers(...)` |
| `userService.getUserById(id)` | `userQueryService.getUserById(id)` |
| `userService.addUser(dto)` | `userCommandService.addUser(dto)` |
| `userService.updateUser(dto)` | `userCommandService.updateUser(dto)` |
| `userService.deleteUser(id)` | `userCommandService.deleteUser(id)` |
| `userService.changePassword(...)` | `userCommandService.changePassword(...)` |
| `userService.resetPassword(id)` | `userCommandService.resetPassword(id)` |
| `userService.grantRoles(...)` | `userCommandService.grantRoles(...)` |
| `userService.lockUser(...)` | `userCommandService.lockUser(...)` |
| `userService.grantTemporaryRoles(...)` | `userRoleCommandService.grantTemporaryRoles(...)` |
| `userService.extendTemporaryRole(...)` | `userRoleCommandService.extendTemporaryRole(...)` |
| `userService.terminateTemporaryRole(...)` | `userRoleCommandService.terminateTemporaryRole(...)` |
| `userService.getUserTemporaryRoles(id)` | `userQueryService.getUserTemporaryRoles(id)` |
| `userService.getUserStatistics(id)` | `userQueryService.getUserStatistics(id)` |
| `userService.updateLastLogin(...)` | `userCommandService.updateLastLogin(...)` |

- [ ] **Step 4: Verify compile**

```bash
mvn clean install -pl scm-system/service -am -DskipTests -f com.scm.parent/pom.xml
```
Expected: BUILD SUCCESS.

- [ ] **Step 5: Commit**

```bash
git add scm-system/service/src/main/java/com/scmcloud/system/controller/SysUserController.java
git commit -m "refactor(system): route SysUserController through new CQRS services"
```

---

## Task 12: Update `UserDubboServiceImpl` to use the new services

**Files:**
- Modify: `scm-system/service/src/main/java/com/scmcloud/system/service/dubbo/UserDubboServiceImpl.java`

- [ ] **Step 1: Replace the import on line 7**

Old:
```java
import com.scmcloud.system.service.ISysUserService;
```
New:
```java
import com.scmcloud.system.service.command.ISysUserCommandService;
import com.scmcloud.system.service.query.ISysUserQueryService;
```

- [ ] **Step 2: Replace the field on line 28**

Old:
```java
    private final ISysUserService sysUserService;
```
New:
```java
    private final ISysUserQueryService sysUserQueryService;
    private final ISysUserCommandService sysUserCommandService;
```

- [ ] **Step 3: Update each method body**

Replace the following `sysUserService.xxx` calls:

| Method body | Change |
|---|---|
| `sysUserService.getUserByUsername(...)` | `sysUserQueryService.getUserByUsername(...)` |
| `sysUserService.getUserInfo(...)` | `sysUserQueryService.getUserInfo(...)` |
| `sysUserService.getUserById(...)` | `sysUserQueryService.getUserById(...)` |
| `sysUserService.updateLastLogin(...)` | `sysUserCommandService.updateLastLogin(...)` |

- [ ] **Step 4: Verify compile**

```bash
mvn clean install -pl scm-system/service -am -DskipTests -f com.scm.parent/pom.xml
```
Expected: BUILD SUCCESS.

- [ ] **Step 5: Commit**

```bash
git add scm-system/service/src/main/java/com/scmcloud/system/service/dubbo/UserDubboServiceImpl.java
git commit -m "refactor(system): route UserDubboServiceImpl through new CQRS services"
```

---

## Task 13: Delete the old `ISysUserService` and its impl

**Files:**
- Delete: `scm-system/service/src/main/java/com/scmcloud/system/service/ISysUserService.java`
- Delete: `scm-system/service/src/main/java/com/scmcloud/system/service/Impl/SysUserServiceImpl.java`
- Delete (if empty): `scm-system/service/src/main/java/com/scmcloud/system/service/Impl/` directory

- [ ] **Step 1: Verify no remaining references**

Run:
```bash
git grep -n "ISysUserService\|SysUserServiceImpl" -- 'scm-system/**' 'scm-auth/**' 'scm-gateway/**' 'scm-common/**'
```
Expected: no output.

If any hits appear, do not proceed — fix the caller (must reference the new query/command services) and re-run.

- [ ] **Step 2: Delete the files**

Run (PowerShell):
```powershell
Remove-Item -LiteralPath "scm-system/service/src/main/java/com/scmcloud/system/service/ISysUserService.java"
Remove-Item -LiteralPath "scm-system/service/src/main/java/com/scmcloud/system/service/Impl/SysUserServiceImpl.java"
if ((Get-ChildItem -LiteralPath "scm-system/service/src/main/java/com/scmcloud/system/service/Impl" -ErrorAction SilentlyContinue | Measure-Object).Count -eq 0) {
    Remove-Item -LiteralPath "scm-system/service/src/main/java/com/scmcloud/system/service/Impl" -Recurse
}
```

- [ ] **Step 3: Verify compile**

```bash
mvn clean install -pl scm-system/service -am -DskipTests -f com.scm.parent/pom.xml
```
Expected: BUILD SUCCESS.

- [ ] **Step 4: Commit**

```bash
git add -A scm-system/service/src/main/java/com/scmcloud/system/service/
git commit -m "refactor(system): delete legacy ISysUserService and SysUserServiceImpl"
```

---

## Task 14: Final verification — build, tests, grep

**Files:** (none)

- [ ] **Step 1: Confirm no stale references anywhere**

Run:
```bash
git grep -n "ISysUserService\|SysUserServiceImpl"
```
Expected: no output.

- [ ] **Step 2: Run scm-system unit tests**

Run:
```bash
mvn test -pl scm-system/service -f com.scm.parent/pom.xml
```
Expected: BUILD SUCCESS. Existing `UserCrossDatabaseQueryServiceTest`, `RoleCrossDatabaseQueryServiceTest`, etc. must still pass.

- [ ] **Step 3: Full build**

Run:
```bash
mvn clean install -DskipTests -f com.scm.parent/pom.xml
```
Expected: BUILD SUCCESS across all modules (catches callers in any service that imported `ISysUserService` via Dubbo interface inheritance).

- [ ] **Step 4: Commit (no changes expected; only if something slipped)**

If any incidental edits were required (e.g., an extra caller surfaced in Step 1), commit them now. Otherwise this task is complete.

```bash
git status
git add -A
git diff --cached --quiet || git commit -m "chore(system): final cleanup after CQRS refactor"
```

---

## Done Criteria

- `git grep "ISysUserService"` → 0 hits
- `git grep "SysUserServiceImpl"` → 0 hits
- `mvn clean install -DskipTests -f com.scm.parent/pom.xml` → BUILD SUCCESS
- `mvn test -pl scm-system/service -f com.scm.parent/pom.xml` → all green
- Three new query/command services exist under `service/query/` and `service/command/`
- Old `ISysUserService.java` and `Impl/SysUserServiceImpl.java` are gone
- `UserCommandTemplate` exists and is used by `updateUser`, `deleteUser`, `grantRoles`
- `ResultCode.USER_PASSWORD_INCORRECT_OLD` and `ResultCode.USER_PASSWORD_SAME_AS_OLD` exist
- All `@CacheEvict` use `keyGenerator = "tenantAwareCacheKeyGenerator"` (except `userDetails` cache which keeps username-keyed auth-path lookup)
- `app.security.admin-user-id` overrides the hardcoded super-admin UUID

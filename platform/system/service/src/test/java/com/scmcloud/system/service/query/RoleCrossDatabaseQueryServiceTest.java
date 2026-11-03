package com.scmcloud.system.service.query;

import com.scmcloud.system.domain.entity.SysUser;
import com.scmcloud.system.mapper.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/**
 * RoleCrossDatabaseQueryService 单元测试
 *
 * @author Deng
 * @since 2025-01-16
 */
@ExtendWith(MockitoExtension.class)
class RoleCrossDatabaseQueryServiceTest {

    @Mock
    private SysRoleMapper roleMapper;

    @Mock
    private SysUserRoleMapper userRoleMapper;

    @Mock
    private SysRoleDeptMapper roleDeptMapper;

    @Mock
    private SysUserMapper userMapper;

    @Mock
    private SysDeptMapper deptMapper;

    @InjectMocks
    private RoleCrossDatabaseQueryService service;

    private UUID testRoleId;
    private UUID testUserId;
    private UUID testDeptId;
    private UUID testTenantId;
    private String testRoleCode;

    @BeforeEach
    void setUp() {
        testRoleId = UUID.randomUUID();
        testUserId = UUID.randomUUID();
        testDeptId = UUID.randomUUID();
        testTenantId = UUID.randomUUID();
        testRoleCode = "ROLE_ADMIN";
    }

    // ========================================
    // 角色基本信息查询测试
    // ========================================

    @Test
    void getRoleLevel_WithValidId_ReturnsLevel() {
        // Arrange
        when(roleMapper.getRoleLevel(testRoleId)).thenReturn(1);

        // Act
        Integer result = service.getRoleLevel(testRoleId);

        // Assert
        assertNotNull(result);
        assertEquals(1, result);
        verify(roleMapper).getRoleLevel(testRoleId);
    }

    @Test
    void getRoleLevel_WithNullId_ReturnsNull() {
        // Act
        Integer result = service.getRoleLevel(null);

        // Assert
        assertNull(result);
        verify(roleMapper, never()).getRoleLevel(any());
    }

    @Test
    void getRoleLevel_RoleNotFound_ReturnsNull() {
        // Arrange
        when(roleMapper.getRoleLevel(testRoleId)).thenReturn(null);

        // Act
        Integer result = service.getRoleLevel(testRoleId);

        // Assert
        assertNull(result);
        verify(roleMapper).getRoleLevel(testRoleId);
    }

    @Test
    void getRoleTenantId_WithValidId_ReturnsTenantId() {
        // Arrange
        when(roleMapper.getRoleTenantId(testRoleId)).thenReturn(testTenantId);

        // Act
        UUID result = service.getRoleTenantId(testRoleId);

        // Assert
        assertNotNull(result);
        assertEquals(testTenantId, result);
        verify(roleMapper).getRoleTenantId(testRoleId);
    }

    @Test
    void getRoleTenantId_WithNullId_ReturnsNull() {
        // Act
        UUID result = service.getRoleTenantId(null);

        // Assert
        assertNull(result);
        verify(roleMapper, never()).getRoleTenantId(any());
    }

    @Test
    void getRoleTenantId_PlatformRole_ReturnsNull() {
        // Arrange
        when(roleMapper.getRoleTenantId(testRoleId)).thenReturn(null);

        // Act
        UUID result = service.getRoleTenantId(testRoleId);

        // Assert
        assertNull(result);
        verify(roleMapper).getRoleTenantId(testRoleId);
    }

    // ========================================
    // 角色-用户关系查询测试
    // ========================================

    @Test
    void findFirstUserIdByRoleCode_WithValidCode_ReturnsUserId() {
        // Arrange
        UUID userId2 = UUID.randomUUID();
        List<UUID> userIds = Arrays.asList(testUserId, userId2);

        SysUser user1 = new SysUser();
        user1.setId(testUserId);
        user1.setStatus(1);
        user1.setCreateTime(LocalDateTime.now().minusDays(2));

        SysUser user2 = new SysUser();
        user2.setId(userId2);
        user2.setStatus(1);
        user2.setCreateTime(LocalDateTime.now());

        when(roleMapper.findIdByRoleCode(testRoleCode)).thenReturn(testRoleId);
        when(userRoleMapper.findUserIdsByRoleId(testRoleId)).thenReturn(userIds);
        when(userMapper.selectBasicInfoByIds(userIds)).thenReturn(Arrays.asList(user1, user2));

        // Act
        UUID result = service.findFirstUserIdByRoleCode(testRoleCode);

        // Assert
        assertNotNull(result);
        assertEquals(testUserId, result); // Should return earliest created user
        verify(roleMapper).findIdByRoleCode(testRoleCode);
        verify(userRoleMapper).findUserIdsByRoleId(testRoleId);
        verify(userMapper).selectBasicInfoByIds(userIds);
    }

    @Test
    void findFirstUserIdByRoleCode_WithNullCode_ReturnsNull() {
        // Act
        UUID result = service.findFirstUserIdByRoleCode(null);

        // Assert
        assertNull(result);
        verify(roleMapper, never()).findIdByRoleCode(any());
    }

    @Test
    void findFirstUserIdByRoleCode_WithEmptyCode_ReturnsNull() {
        // Act
        UUID result = service.findFirstUserIdByRoleCode("   ");

        // Assert
        assertNull(result);
        verify(roleMapper, never()).findIdByRoleCode(any());
    }

    @Test
    void findFirstUserIdByRoleCode_RoleNotFound_ReturnsNull() {
        // Arrange
        when(roleMapper.findIdByRoleCode(testRoleCode)).thenReturn(null);

        // Act
        UUID result = service.findFirstUserIdByRoleCode(testRoleCode);

        // Assert
        assertNull(result);
        verify(roleMapper).findIdByRoleCode(testRoleCode);
        verify(userRoleMapper, never()).findUserIdsByRoleId(any());
    }

    @Test
    void findFirstUserIdByRoleCode_NoUsers_ReturnsNull() {
        // Arrange
        when(roleMapper.findIdByRoleCode(testRoleCode)).thenReturn(testRoleId);
        when(userRoleMapper.findUserIdsByRoleId(testRoleId)).thenReturn(Collections.emptyList());

        // Act
        UUID result = service.findFirstUserIdByRoleCode(testRoleCode);

        // Assert
        assertNull(result);
        verify(roleMapper).findIdByRoleCode(testRoleCode);
        verify(userRoleMapper).findUserIdsByRoleId(testRoleId);
        verify(userMapper, never()).selectBasicInfoByIds(anyList());
    }

    @Test
    void findFirstUserIdByRoleCode_NoActiveUsers_ReturnsNull() {
        // Arrange
        List<UUID> userIds = Collections.singletonList(testUserId);

        SysUser inactiveUser = new SysUser();
        inactiveUser.setId(testUserId);
        inactiveUser.setStatus(0); // Inactive

        when(roleMapper.findIdByRoleCode(testRoleCode)).thenReturn(testRoleId);
        when(userRoleMapper.findUserIdsByRoleId(testRoleId)).thenReturn(userIds);
        when(userMapper.selectBasicInfoByIds(userIds)).thenReturn(Collections.singletonList(inactiveUser));

        // Act
        UUID result = service.findFirstUserIdByRoleCode(testRoleCode);

        // Assert
        assertNull(result);
        verify(userMapper).selectBasicInfoByIds(userIds);
    }

    // ========================================
    // 角色-部门关系查询测试
    // ========================================

    @Test
    void findAccessibleDeptIds_WithValidId_ReturnsDeptList() {
        // Arrange
        UUID deptId2 = UUID.randomUUID();
        UUID childDeptId = UUID.randomUUID();

        List<UUID> directDepts = Collections.singletonList(testDeptId);
        List<UUID> deptsWithChildren = Collections.singletonList(deptId2);
        List<UUID> allChildDepts = Arrays.asList(deptId2, childDeptId);

        when(roleDeptMapper.findDeptIdsWithoutChildren(testRoleId)).thenReturn(directDepts);
        when(roleDeptMapper.findDeptIdsWithChildren(testRoleId)).thenReturn(deptsWithChildren);
        when(deptMapper.selectDeptsAndChildren(deptsWithChildren)).thenReturn(allChildDepts);

        // Act
        List<UUID> result = service.findAccessibleDeptIds(testRoleId);

        // Assert
        assertNotNull(result);
        assertEquals(3, result.size());
        assertTrue(result.contains(testDeptId));
        assertTrue(result.contains(deptId2));
        assertTrue(result.contains(childDeptId));
        verify(roleDeptMapper).findDeptIdsWithoutChildren(testRoleId);
        verify(roleDeptMapper).findDeptIdsWithChildren(testRoleId);
        verify(deptMapper).selectDeptsAndChildren(deptsWithChildren);
    }

    @Test
    void findAccessibleDeptIds_WithNullId_ReturnsEmptyList() {
        // Act
        List<UUID> result = service.findAccessibleDeptIds(null);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(roleDeptMapper, never()).findDeptIdsWithoutChildren(any());
    }

    @Test
    void findAccessibleDeptIds_OnlyDirectDepts_ReturnsDeptList() {
        // Arrange
        List<UUID> directDepts = Collections.singletonList(testDeptId);

        when(roleDeptMapper.findDeptIdsWithoutChildren(testRoleId)).thenReturn(directDepts);
        when(roleDeptMapper.findDeptIdsWithChildren(testRoleId)).thenReturn(Collections.emptyList());

        // Act
        List<UUID> result = service.findAccessibleDeptIds(testRoleId);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.contains(testDeptId));
        verify(deptMapper, never()).selectDeptsAndChildren(anyList());
    }

    @Test
    void findAccessibleDeptIds_NoDirectDepts_ReturnsOnlyChildDepts() {
        // Arrange
        UUID childDeptId = UUID.randomUUID();
        List<UUID> deptsWithChildren = Collections.singletonList(testDeptId);
        List<UUID> allChildDepts = Arrays.asList(testDeptId, childDeptId);

        when(roleDeptMapper.findDeptIdsWithoutChildren(testRoleId)).thenReturn(null);
        when(roleDeptMapper.findDeptIdsWithChildren(testRoleId)).thenReturn(deptsWithChildren);
        when(deptMapper.selectDeptsAndChildren(deptsWithChildren)).thenReturn(allChildDepts);

        // Act
        List<UUID> result = service.findAccessibleDeptIds(testRoleId);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    void findAccessibleDeptIds_NoDepts_ReturnsEmptyList() {
        // Arrange
        when(roleDeptMapper.findDeptIdsWithoutChildren(testRoleId)).thenReturn(Collections.emptyList());
        when(roleDeptMapper.findDeptIdsWithChildren(testRoleId)).thenReturn(Collections.emptyList());

        // Act
        List<UUID> result = service.findAccessibleDeptIds(testRoleId);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ========================================
    // 角色过期查询测试
    // ========================================

    @Test
    void findExpiringRolesWithUserInfo_WithValidDays_ReturnsRoleList() {
        // Arrange
        Integer days = 7;

        Map<String, Object> role = new HashMap<>();
        role.put("user_id", testUserId);
        role.put("role_id", testRoleId);
        role.put("role_name", "Admin");
        role.put("expire_time", LocalDateTime.now().plusDays(5));

        List<Map<String, Object>> expiringRoles = Collections.singletonList(role);

        SysUser user = new SysUser();
        user.setId(testUserId);
        user.setUsername("testuser");
        user.setEmail("test@example.com");

        when(userRoleMapper.findExpiringRolesForNotification(days)).thenReturn(expiringRoles);
        when(userMapper.selectBasicInfoByIds(anyList())).thenReturn(Collections.singletonList(user));

        // Act
        List<Map<String, Object>> result = service.findExpiringRolesWithUserInfo(days);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("testuser", result.get(0).get("username"));
        assertEquals("test@example.com", result.get(0).get("email"));
        verify(userRoleMapper).findExpiringRolesForNotification(days);
        verify(userMapper).selectBasicInfoByIds(anyList());
    }

    @Test
    void findExpiringRolesWithUserInfo_WithNullDays_ReturnsEmptyList() {
        // Act
        List<Map<String, Object>> result = service.findExpiringRolesWithUserInfo(null);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(userRoleMapper, never()).findExpiringRolesForNotification(any());
    }

    @Test
    void findExpiringRolesWithUserInfo_WithZeroDays_ReturnsEmptyList() {
        // Act
        List<Map<String, Object>> result = service.findExpiringRolesWithUserInfo(0);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(userRoleMapper, never()).findExpiringRolesForNotification(any());
    }

    @Test
    void findExpiringRolesWithUserInfo_WithNegativeDays_ReturnsEmptyList() {
        // Act
        List<Map<String, Object>> result = service.findExpiringRolesWithUserInfo(-1);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(userRoleMapper, never()).findExpiringRolesForNotification(any());
    }

    @Test
    void findExpiringRolesWithUserInfo_NoExpiringRoles_ReturnsEmptyList() {
        // Arrange
        when(userRoleMapper.findExpiringRolesForNotification(7)).thenReturn(Collections.emptyList());

        // Act
        List<Map<String, Object>> result = service.findExpiringRolesWithUserInfo(7);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(userMapper, never()).selectBasicInfoByIds(anyList());
    }

    @Test
    void findExpiredRolesWithUserInfo_WithValidData_ReturnsRoleList() {
        // Arrange
        Map<String, Object> role = new HashMap<>();
        role.put("user_id", testUserId);
        role.put("role_id", testRoleId);
        role.put("role_name", "Admin");

        List<Map<String, Object>> expiredRoles = Collections.singletonList(role);

        SysUser user = new SysUser();
        user.setId(testUserId);
        user.setUsername("testuser");

        when(userRoleMapper.findExpiredRolesForCleanup()).thenReturn(expiredRoles);
        when(userMapper.selectBasicInfoByIds(anyList())).thenReturn(Collections.singletonList(user));

        // Act
        List<Map<String, Object>> result = service.findExpiredRolesWithUserInfo();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("testuser", result.get(0).get("username"));
        assertNull(result.get(0).get("email")); // Email not included for expired roles
        verify(userRoleMapper).findExpiredRolesForCleanup();
        verify(userMapper).selectBasicInfoByIds(anyList());
    }

    @Test
    void findExpiredRolesWithUserInfo_NoExpiredRoles_ReturnsEmptyList() {
        // Arrange
        when(userRoleMapper.findExpiredRolesForCleanup()).thenReturn(Collections.emptyList());

        // Act
        List<Map<String, Object>> result = service.findExpiredRolesWithUserInfo();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(userMapper, never()).selectBasicInfoByIds(anyList());
    }

    @Test
    void findExpiredRolesWithUserInfo_UserNotFound_ReturnsRoleWithoutUserInfo() {
        // Arrange
        Map<String, Object> role = new HashMap<>();
        role.put("user_id", testUserId);
        role.put("role_id", testRoleId);
        role.put("role_name", "Admin");

        List<Map<String, Object>> expiredRoles = Collections.singletonList(role);

        when(userRoleMapper.findExpiredRolesForCleanup()).thenReturn(expiredRoles);
        when(userMapper.selectBasicInfoByIds(anyList())).thenReturn(Collections.emptyList());

        // Act
        List<Map<String, Object>> result = service.findExpiredRolesWithUserInfo();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertNull(result.get(0).get("username"));
        verify(userMapper).selectBasicInfoByIds(anyList());
    }

    @Test
    void findExpiringRolesWithUserInfo_MultipleUsersMultipleRoles_ReturnsEnrichedList() {
        // Arrange
        UUID userId2 = UUID.randomUUID();
        Integer days = 7;

        Map<String, Object> role1 = new HashMap<>();
        role1.put("user_id", testUserId);
        role1.put("role_id", testRoleId);
        role1.put("role_name", "Admin");

        Map<String, Object> role2 = new HashMap<>();
        role2.put("user_id", userId2);
        role2.put("role_id", UUID.randomUUID());
        role2.put("role_name", "Manager");

        List<Map<String, Object>> expiringRoles = Arrays.asList(role1, role2);

        SysUser user1 = new SysUser();
        user1.setId(testUserId);
        user1.setUsername("user1");
        user1.setEmail("user1@example.com");

        SysUser user2 = new SysUser();
        user2.setId(userId2);
        user2.setUsername("user2");
        user2.setEmail("user2@example.com");

        when(userRoleMapper.findExpiringRolesForNotification(days)).thenReturn(expiringRoles);
        when(userMapper.selectBasicInfoByIds(anyList())).thenReturn(Arrays.asList(user1, user2));

        // Act
        List<Map<String, Object>> result = service.findExpiringRolesWithUserInfo(days);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("user1", result.get(0).get("username"));
        assertEquals("user2", result.get(1).get("username"));
    }
}
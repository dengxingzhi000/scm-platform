package com.scmcloud.system.service.query;

import com.scmcloud.system.domain.entity.SysUser;
import com.scmcloud.system.mapper.SysUserMapper;
import com.scmcloud.system.mapper.SysUserRoleMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/**
 * UserCrossDatabaseQueryService 鍗曞厓娴嬭瘯
 *
 * @author Deng
 * @since 2025-01-16
 */
@ExtendWith(MockitoExtension.class)
class UserCrossDatabaseQueryServiceTest {

    @Mock
    private SysUserMapper userMapper;

    @Mock
    private SysUserRoleMapper userRoleMapper;

    @InjectMocks
    private UserCrossDatabaseQueryService service;

    private UUID testUserId;
    private UUID testRoleId;
    private UUID testDeptId;
    private SysUser testUser;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testRoleId = UUID.randomUUID();
        testDeptId = UUID.randomUUID();

        testUser = new SysUser();
        testUser.setId(testUserId);
        testUser.setUsername("testuser");
        testUser.setRealName("Test User");
        testUser.setEmail("test@example.com");
        testUser.setStatus(1);
        testUser.setCreateTime(LocalDateTime.now());
    }

    // ========================================
    // 鐢ㄦ埛鍩烘湰淇℃伅鏌ヨ娴嬭瘯
    // ========================================

    @Test
    void getUserBasicInfo_WithValidId_ReturnsUser() {
        // Arrange
        when(userMapper.selectById(testUserId)).thenReturn(testUser);

        // Act
        SysUser result = service.getUserBasicInfo(testUserId);

        // Assert
        assertNotNull(result);
        assertEquals(testUserId, result.getId());
        assertEquals("testuser", result.getUsername());
        verify(userMapper).selectById(testUserId);
    }

    @Test
    void getUserBasicInfo_WithNullId_ReturnsNull() {
        // Act
        SysUser result = service.getUserBasicInfo(null);

        // Assert
        assertNull(result);
        verify(userMapper, never()).selectById(any());
    }

    @Test
    void getUserBasicInfo_UserNotFound_ReturnsNull() {
        // Arrange
        when(userMapper.selectById(testUserId)).thenReturn(null);

        // Act
        SysUser result = service.getUserBasicInfo(testUserId);

        // Assert
        assertNull(result);
        verify(userMapper).selectById(testUserId);
    }

    @Test
    void getUserBasicInfoBatch_WithValidIds_ReturnsUserList() {
        // Arrange
        UUID userId2 = UUID.randomUUID();
        List<UUID> userIds = Arrays.asList(testUserId, userId2);

        SysUser user2 = new SysUser();
        user2.setId(userId2);
        user2.setUsername("testuser2");

        List<SysUser> expectedUsers = Arrays.asList(testUser, user2);
        when(userMapper.selectBasicInfoByIds(userIds)).thenReturn(expectedUsers);

        // Act
        List<SysUser> result = service.getUserBasicInfoBatch(userIds);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(testUserId, result.get(0).getId());
        assertEquals(userId2, result.get(1).getId());
        verify(userMapper).selectBasicInfoByIds(userIds);
    }

    @Test
    void getUserBasicInfoBatch_WithNullIds_ReturnsEmptyList() {
        // Act
        List<SysUser> result = service.getUserBasicInfoBatch(null);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(userMapper, never()).selectBasicInfoByIds(anyList());
    }

    @Test
    void getUserBasicInfoBatch_WithEmptyIds_ReturnsEmptyList() {
        // Act
        List<SysUser> result = service.getUserBasicInfoBatch(Collections.emptyList());

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(userMapper, never()).selectBasicInfoByIds(anyList());
    }

    @Test
    void getUserBasicInfoMap_WithValidIds_ReturnsMap() {
        // Arrange
        UUID userId2 = UUID.randomUUID();
        List<UUID> userIds = Arrays.asList(testUserId, userId2);

        SysUser user2 = new SysUser();
        user2.setId(userId2);
        user2.setUsername("testuser2");

        List<SysUser> users = Arrays.asList(testUser, user2);
        when(userMapper.selectBasicInfoByIds(userIds)).thenReturn(users);

        // Act
        Map<UUID, SysUser> result = service.getUserBasicInfoMap(userIds);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.containsKey(testUserId));
        assertTrue(result.containsKey(userId2));
        assertEquals("testuser", result.get(testUserId).getUsername());
        verify(userMapper).selectBasicInfoByIds(userIds);
    }

    @Test
    void getUserBasicInfoMap_WithDuplicateIds_KeepsFirstUser() {
        // Arrange
        List<UUID> userIds = Arrays.asList(testUserId, testUserId);
        List<SysUser> users = Arrays.asList(testUser, testUser);
        when(userMapper.selectBasicInfoByIds(userIds)).thenReturn(users);

        // Act
        Map<UUID, SysUser> result = service.getUserBasicInfoMap(userIds);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.containsKey(testUserId));
    }

    // ========================================
    // 鐢ㄦ埛瑙掕壊鏌ヨ娴嬭瘯
    // ========================================

    @Test
    void findUserRolesWithNames_WithValidId_ReturnsRoleList() {
        // Arrange
        Map<String, Object> role1 = new HashMap<>();
        role1.put("id", testRoleId);
        role1.put("name", "Admin");
        List<Map<String, Object>> expectedRoles = Collections.singletonList(role1);

        when(userRoleMapper.findUserRolesWithNames(testUserId)).thenReturn(expectedRoles);

        // Act
        List<Map<String, Object>> result = service.findUserRolesWithNames(testUserId);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Admin", result.get(0).get("name"));
        verify(userRoleMapper).findUserRolesWithNames(testUserId);
    }

    @Test
    void findUserRolesWithNames_WithNullId_ReturnsEmptyList() {
        // Act
        List<Map<String, Object>> result = service.findUserRolesWithNames(null);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(userRoleMapper, never()).findUserRolesWithNames(any());
    }

    @Test
    void findRoleCodesByUserId_WithValidId_ReturnsRoleCodes() {
        // Arrange
        Set<String> expectedCodes = new HashSet<>(Arrays.asList("ROLE_ADMIN", "ROLE_USER"));
        when(userRoleMapper.findRoleCodesByUserId(testUserId)).thenReturn(expectedCodes);

        // Act
        Set<String> result = service.findRoleCodesByUserId(testUserId);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains("ROLE_ADMIN"));
        assertTrue(result.contains("ROLE_USER"));
        verify(userRoleMapper).findRoleCodesByUserId(testUserId);
    }

    @Test
    void findRoleCodesByUserId_WithNullId_ReturnsEmptySet() {
        // Act
        Set<String> result = service.findRoleCodesByUserId(null);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(userRoleMapper, never()).findRoleCodesByUserId(any());
    }

    @Test
    void getUserMaxRoleLevel_WithValidId_ReturnsLevel() {
        // Arrange
        when(userRoleMapper.getUserMaxRoleLevel(testUserId)).thenReturn(1);

        // Act
        Integer result = service.getUserMaxRoleLevel(testUserId);

        // Assert
        assertNotNull(result);
        assertEquals(1, result);
        verify(userRoleMapper).getUserMaxRoleLevel(testUserId);
    }

    @Test
    void getUserMaxRoleLevel_WithNullId_ReturnsNull() {
        // Act
        Integer result = service.getUserMaxRoleLevel(null);

        // Assert
        assertNull(result);
        verify(userRoleMapper, never()).getUserMaxRoleLevel(any());
    }

    @Test
    void getUserMaxRoleLevel_NoRoles_ReturnsNull() {
        // Arrange
        when(userRoleMapper.getUserMaxRoleLevel(testUserId)).thenReturn(null);

        // Act
        Integer result = service.getUserMaxRoleLevel(testUserId);

        // Assert
        assertNull(result);
        verify(userRoleMapper).getUserMaxRoleLevel(testUserId);
    }

    @Test
    void countUserRoles_WithValidId_ReturnsCount() {
        // Arrange
        when(userRoleMapper.countUserRoles(testUserId)).thenReturn(3);

        // Act
        Integer result = service.countUserRoles(testUserId);

        // Assert
        assertEquals(3, result);
        verify(userRoleMapper).countUserRoles(testUserId);
    }

    @Test
    void countUserRoles_WithNullId_ReturnsZero() {
        // Act
        Integer result = service.countUserRoles(null);

        // Assert
        assertEquals(0, result);
        verify(userRoleMapper, never()).countUserRoles(any());
    }

    @Test
    void countUserRoles_MapperReturnsNull_ReturnsZero() {
        // Arrange
        when(userRoleMapper.countUserRoles(testUserId)).thenReturn(null);

        // Act
        Integer result = service.countUserRoles(testUserId);

        // Assert
        assertEquals(0, result);
        verify(userRoleMapper).countUserRoles(testUserId);
    }

    // ========================================
    // 鐢ㄦ埛鏉冮檺鏌ヨ娴嬭瘯
    // ========================================

    @Test
    void findPermissionCodesByUserId_WithValidId_ReturnsPermissions() {
        // Arrange
        Set<String> expectedPermissions = new HashSet<>(Arrays.asList("user:add", "user:edit"));
        when(userRoleMapper.findPermissionCodesByUserId(testUserId)).thenReturn(expectedPermissions);

        // Act
        Set<String> result = service.findPermissionCodesByUserId(testUserId);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains("user:add"));
        assertTrue(result.contains("user:edit"));
        verify(userRoleMapper).findPermissionCodesByUserId(testUserId);
    }

    @Test
    void findPermissionCodesByUserId_WithNullId_ReturnsEmptySet() {
        // Act
        Set<String> result = service.findPermissionCodesByUserId(null);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(userRoleMapper, never()).findPermissionCodesByUserId(any());
    }

    @Test
    void getUserDataScope_WithValidId_ReturnsDataScope() {
        // Arrange
        when(userRoleMapper.getUserDataScope(testUserId)).thenReturn(3);

        // Act
        Integer result = service.getUserDataScope(testUserId);

        // Assert
        assertEquals(3, result);
        verify(userRoleMapper).getUserDataScope(testUserId);
    }

    @Test
    void getUserDataScope_WithNullId_ReturnsDefaultValue() {
        // Act
        Integer result = service.getUserDataScope(null);

        // Assert
        assertEquals(5, result); // Default: 浠呮湰锟?
        verify(userRoleMapper, never()).getUserDataScope(any());
    }

    @Test
    void getUserDataScope_MapperReturnsNull_ReturnsDefaultValue() {
        // Arrange
        when(userRoleMapper.getUserDataScope(testUserId)).thenReturn(null);

        // Act
        Integer result = service.getUserDataScope(testUserId);

        // Assert
        assertEquals(5, result); // Default: 浠呮湰锟?
        verify(userRoleMapper).getUserDataScope(testUserId);
    }

    @Test
    void getMaxApprovalAmount_WithValidId_ReturnsAmount() {
        // Arrange
        BigDecimal expectedAmount = new BigDecimal("10000.00");
        when(userRoleMapper.getMaxApprovalAmount(testUserId)).thenReturn(expectedAmount);

        // Act
        BigDecimal result = service.getMaxApprovalAmount(testUserId);

        // Assert
        assertNotNull(result);
        assertEquals(expectedAmount, result);
        verify(userRoleMapper).getMaxApprovalAmount(testUserId);
    }

    @Test
    void getMaxApprovalAmount_WithNullId_ReturnsZero() {
        // Act
        BigDecimal result = service.getMaxApprovalAmount(null);

        // Assert
        assertEquals(BigDecimal.ZERO, result);
        verify(userRoleMapper, never()).getMaxApprovalAmount(any());
    }

    // ========================================
    // 涓存椂瑙掕壊鏌ヨ娴嬭瘯
    // ========================================

    @Test
    void hasTemporaryRole_WithValidIds_ReturnsTrue() {
        // Arrange
        when(userRoleMapper.hasTemporaryRole(testUserId, testRoleId)).thenReturn(true);

        // Act
        boolean result = service.hasTemporaryRole(testUserId, testRoleId);

        // Assert
        assertTrue(result);
        verify(userRoleMapper).hasTemporaryRole(testUserId, testRoleId);
    }

    @Test
    void hasTemporaryRole_WithNullUserId_ReturnsFalse() {
        // Act
        boolean result = service.hasTemporaryRole(null, testRoleId);

        // Assert
        assertFalse(result);
        verify(userRoleMapper, never()).hasTemporaryRole(any(), any());
    }

    @Test
    void hasTemporaryRole_WithNullRoleId_ReturnsFalse() {
        // Act
        boolean result = service.hasTemporaryRole(testUserId, null);

        // Assert
        assertFalse(result);
        verify(userRoleMapper, never()).hasTemporaryRole(any(), any());
    }

    @Test
    void findTemporaryRolesByUserId_WithValidId_ReturnsRoles() {
        // Arrange
        Map<String, Object> tempRole = new HashMap<>();
        tempRole.put("role_id", testRoleId);
        tempRole.put("role_name", "Temporary Admin");
        tempRole.put("expire_time", LocalDateTime.now().plusDays(7));
        List<Map<String, Object>> expectedRoles = Collections.singletonList(tempRole);

        when(userRoleMapper.findTemporaryRolesByUserId(testUserId)).thenReturn(expectedRoles);

        // Act
        List<Map<String, Object>> result = service.findTemporaryRolesByUserId(testUserId);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Temporary Admin", result.get(0).get("role_name"));
        verify(userRoleMapper).findTemporaryRolesByUserId(testUserId);
    }

    @Test
    void findTemporaryRolesByUserId_WithNullId_ReturnsEmptyList() {
        // Act
        List<Map<String, Object>> result = service.findTemporaryRolesByUserId(null);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(userRoleMapper, never()).findTemporaryRolesByUserId(any());
    }

    @Test
    void countTemporaryRoles_WithValidId_ReturnsCount() {
        // Arrange
        when(userRoleMapper.countTemporaryRoles(testUserId)).thenReturn(2);

        // Act
        Integer result = service.countTemporaryRoles(testUserId);

        // Assert
        assertEquals(2, result);
        verify(userRoleMapper).countTemporaryRoles(testUserId);
    }

    @Test
    void countTemporaryRoles_WithNullId_ReturnsZero() {
        // Act
        Integer result = service.countTemporaryRoles(null);

        // Assert
        assertEquals(0, result);
        verify(userRoleMapper, never()).countTemporaryRoles(any());
    }

    @Test
    void countTemporaryRoles_MapperReturnsNull_ReturnsZero() {
        // Arrange
        when(userRoleMapper.countTemporaryRoles(testUserId)).thenReturn(null);

        // Act
        Integer result = service.countTemporaryRoles(testUserId);

        // Assert
        assertEquals(0, result);
        verify(userRoleMapper).countTemporaryRoles(testUserId);
    }

    @Test
    void countExpiringRoles_WithValidParameters_ReturnsCount() {
        // Arrange
        Integer days = 7;
        when(userRoleMapper.countExpiringRoles(testUserId, days)).thenReturn(3);

        // Act
        Integer result = service.countExpiringRoles(testUserId, days);

        // Assert
        assertEquals(3, result);
        verify(userRoleMapper).countExpiringRoles(testUserId, days);
    }

    @Test
    void countExpiringRoles_WithNullUserId_ReturnsZero() {
        // Act
        Integer result = service.countExpiringRoles(null, 7);

        // Assert
        assertEquals(0, result);
        verify(userRoleMapper, never()).countExpiringRoles(any(), any());
    }

    @Test
    void countExpiringRoles_MapperReturnsNull_ReturnsZero() {
        // Arrange
        when(userRoleMapper.countExpiringRoles(testUserId, 7)).thenReturn(null);

        // Act
        Integer result = service.countExpiringRoles(testUserId, 7);

        // Assert
        assertEquals(0, result);
        verify(userRoleMapper).countExpiringRoles(testUserId, 7);
    }

    // ========================================
    // 閮ㄩ棬鐢ㄦ埛缁熻娴嬭瘯
    // ========================================

    @Test
    void countUsersByDeptId_WithValidId_ReturnsCount() {
        // Arrange
        when(userMapper.countUsersByDeptId(testDeptId)).thenReturn(5);

        // Act
        int result = service.countUsersByDeptId(testDeptId);

        // Assert
        assertEquals(5, result);
        verify(userMapper).countUsersByDeptId(testDeptId);
    }

    @Test
    void countUsersByDeptId_WithNullId_ReturnsZero() {
        // Act
        int result = service.countUsersByDeptId(null);

        // Assert
        assertEquals(0, result);
        verify(userMapper, never()).countUsersByDeptId(any());
    }

    @Test
    void countUsersByDeptIds_WithValidIds_ReturnsMap() {
        // Arrange
        UUID deptId2 = UUID.randomUUID();
        List<UUID> deptIds = Arrays.asList(testDeptId, deptId2);

        Map<UUID, Map<String, Object>> mapperResult = new HashMap<>();
        Map<String, Object> row1 = new HashMap<>();
        row1.put("user_count", 5);
        Map<String, Object> row2 = new HashMap<>();
        row2.put("user_count", 3);

        mapperResult.put(testDeptId, row1);
        mapperResult.put(deptId2, row2);

        when(userMapper.countUsersByDeptIds(deptIds)).thenReturn(mapperResult);

        // Act
        Map<UUID, Integer> result = service.countUsersByDeptIds(deptIds);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(5, result.get(testDeptId));
        assertEquals(3, result.get(deptId2));
        verify(userMapper).countUsersByDeptIds(deptIds);
    }

    @Test
    void countUsersByDeptIds_WithNullIds_ReturnsEmptyMap() {
        // Act
        Map<UUID, Integer> result = service.countUsersByDeptIds(null);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(userMapper, never()).countUsersByDeptIds(anyList());
    }

    @Test
    void countUsersByDeptIds_WithEmptyIds_ReturnsEmptyMap() {
        // Act
        Map<UUID, Integer> result = service.countUsersByDeptIds(Collections.emptyList());

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(userMapper, never()).countUsersByDeptIds(anyList());
    }

    @Test
    void countUsersByDeptIds_MapperReturnsNull_ReturnsEmptyMap() {
        // Arrange
        List<UUID> deptIds = Collections.singletonList(testDeptId);
        when(userMapper.countUsersByDeptIds(deptIds)).thenReturn(null);

        // Act
        Map<UUID, Integer> result = service.countUsersByDeptIds(deptIds);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(userMapper).countUsersByDeptIds(deptIds);
    }

    @Test
    void countUsersByDeptIds_WithNullCount_ReturnsZero() {
        // Arrange
        List<UUID> deptIds = Collections.singletonList(testDeptId);

        Map<UUID, Map<String, Object>> mapperResult = new HashMap<>();
        Map<String, Object> row = new HashMap<>();
        row.put("user_count", null);
        mapperResult.put(testDeptId, row);

        when(userMapper.countUsersByDeptIds(deptIds)).thenReturn(mapperResult);

        // Act
        Map<UUID, Integer> result = service.countUsersByDeptIds(deptIds);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(0, result.get(testDeptId));
    }
}
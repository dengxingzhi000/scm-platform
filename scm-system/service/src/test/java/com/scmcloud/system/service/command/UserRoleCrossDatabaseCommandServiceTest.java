package com.scmcloud.system.service.command;

import com.scmcloud.system.mapper.SysUserRoleMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/**
 * UserRoleCrossDatabaseCommandService 鍗曞厓娴嬭瘯
 *
 * @author Deng
 * @since 2025-01-16
 */
@ExtendWith(MockitoExtension.class)
class UserRoleCrossDatabaseCommandServiceTest {

    @Mock
    private SysUserRoleMapper userRoleMapper;

    @InjectMocks
    private UserRoleCrossDatabaseCommandService service;

    private UUID testUserId;
    private UUID testRoleId;
    private UUID testCreateBy;
    private List<UUID> testRoleIds;
    private LocalDateTime testEffectiveTime;
    private LocalDateTime testExpireTime;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testRoleId = UUID.randomUUID();
        testCreateBy = UUID.randomUUID();
        testRoleIds = Arrays.asList(testRoleId, UUID.randomUUID(), UUID.randomUUID());
        testEffectiveTime = LocalDateTime.now();
        testExpireTime = LocalDateTime.now().plusDays(30);
    }

    // ========================================
    // 鐢ㄦ埛瑙掕壊鍏宠仈鍐欐搷浣滄祴锟?
    // ========================================

    @Test
    void batchInsertUserRoles_WithValidParameters_ReturnsInsertedCount() {
        // Arrange
        when(userRoleMapper.batchInsert(testUserId, testRoleIds, testCreateBy)).thenReturn(3);

        // Act
        int result = service.batchInsertUserRoles(testUserId, testRoleIds, testCreateBy);

        // Assert
        assertEquals(3, result);
        verify(userRoleMapper).batchInsert(testUserId, testRoleIds, testCreateBy);
    }

    @Test
    void batchInsertUserRoles_WithNullUserId_ReturnsZero() {
        // Act
        int result = service.batchInsertUserRoles(null, testRoleIds, testCreateBy);

        // Assert
        assertEquals(0, result);
        verify(userRoleMapper, never()).batchInsert(any(), anyList(), any());
    }

    @Test
    void batchInsertUserRoles_WithNullRoleIds_ReturnsZero() {
        // Act
        int result = service.batchInsertUserRoles(testUserId, null, testCreateBy);

        // Assert
        assertEquals(0, result);
        verify(userRoleMapper, never()).batchInsert(any(), anyList(), any());
    }

    @Test
    void batchInsertUserRoles_WithEmptyRoleIds_ReturnsZero() {
        // Act
        int result = service.batchInsertUserRoles(testUserId, Collections.emptyList(), testCreateBy);

        // Assert
        assertEquals(0, result);
        verify(userRoleMapper, never()).batchInsert(any(), anyList(), any());
    }

    @Test
    void batchInsertUserRoles_WithSingleRole_ReturnsOne() {
        // Arrange
        List<UUID> singleRole = Collections.singletonList(testRoleId);
        when(userRoleMapper.batchInsert(testUserId, singleRole, testCreateBy)).thenReturn(1);

        // Act
        int result = service.batchInsertUserRoles(testUserId, singleRole, testCreateBy);

        // Assert
        assertEquals(1, result);
        verify(userRoleMapper).batchInsert(testUserId, singleRole, testCreateBy);
    }

    @Test
    void batchInsertUserRoles_MapperReturnsZero_ReturnsZero() {
        // Arrange
        when(userRoleMapper.batchInsert(testUserId, testRoleIds, testCreateBy)).thenReturn(0);

        // Act
        int result = service.batchInsertUserRoles(testUserId, testRoleIds, testCreateBy);

        // Assert
        assertEquals(0, result);
        verify(userRoleMapper).batchInsert(testUserId, testRoleIds, testCreateBy);
    }

    @Test
    void batchInsertTemporaryUserRoles_WithValidParameters_ReturnsInsertedCount() {
        // Arrange
        when(userRoleMapper.batchInsertTemporary(
                testUserId, testRoleIds, testEffectiveTime, testExpireTime, testCreateBy
        )).thenReturn(3);

        // Act
        int result = service.batchInsertTemporaryUserRoles(
                testUserId, testRoleIds, testEffectiveTime, testExpireTime, testCreateBy
        );

        // Assert
        assertEquals(3, result);
        verify(userRoleMapper).batchInsertTemporary(
                testUserId, testRoleIds, testEffectiveTime, testExpireTime, testCreateBy
        );
    }

    @Test
    void batchInsertTemporaryUserRoles_WithNullUserId_ReturnsZero() {
        // Act
        int result = service.batchInsertTemporaryUserRoles(
                null, testRoleIds, testEffectiveTime, testExpireTime, testCreateBy
        );

        // Assert
        assertEquals(0, result);
        verify(userRoleMapper, never()).batchInsertTemporary(any(), anyList(), any(), any(), any());
    }

    @Test
    void batchInsertTemporaryUserRoles_WithNullRoleIds_ReturnsZero() {
        // Act
        int result = service.batchInsertTemporaryUserRoles(
                testUserId, null, testEffectiveTime, testExpireTime, testCreateBy
        );

        // Assert
        assertEquals(0, result);
        verify(userRoleMapper, never()).batchInsertTemporary(any(), anyList(), any(), any(), any());
    }

    @Test
    void batchInsertTemporaryUserRoles_WithEmptyRoleIds_ReturnsZero() {
        // Act
        int result = service.batchInsertTemporaryUserRoles(
                testUserId, Collections.emptyList(), testEffectiveTime, testExpireTime, testCreateBy
        );

        // Assert
        assertEquals(0, result);
        verify(userRoleMapper, never()).batchInsertTemporary(any(), anyList(), any(), any(), any());
    }

    @Test
    void batchInsertTemporaryUserRoles_WithNullEffectiveTime_CallsMapper() {
        // Arrange
        when(userRoleMapper.batchInsertTemporary(
                testUserId, testRoleIds, null, testExpireTime, testCreateBy
        )).thenReturn(3);

        // Act
        int result = service.batchInsertTemporaryUserRoles(
                testUserId, testRoleIds, null, testExpireTime, testCreateBy
        );

        // Assert
        assertEquals(3, result);
        verify(userRoleMapper).batchInsertTemporary(
                testUserId, testRoleIds, null, testExpireTime, testCreateBy
        );
    }

    @Test
    void batchInsertTemporaryUserRoles_WithFutureExpireTime_Success() {
        // Arrange
        LocalDateTime futureTime = LocalDateTime.now().plusYears(1);
        when(userRoleMapper.batchInsertTemporary(
                testUserId, testRoleIds, testEffectiveTime, futureTime, testCreateBy
        )).thenReturn(3);

        // Act
        int result = service.batchInsertTemporaryUserRoles(
                testUserId, testRoleIds, testEffectiveTime, futureTime, testCreateBy
        );

        // Assert
        assertEquals(3, result);
        verify(userRoleMapper).batchInsertTemporary(
                testUserId, testRoleIds, testEffectiveTime, futureTime, testCreateBy
        );
    }

    @Test
    void deleteUserRoles_WithValidUserId_ReturnsDeletedCount() {
        // Arrange
        when(userRoleMapper.deleteByUserId(testUserId)).thenReturn(5);

        // Act
        int result = service.deleteUserRoles(testUserId);

        // Assert
        assertEquals(5, result);
        verify(userRoleMapper).deleteByUserId(testUserId);
    }

    @Test
    void deleteUserRoles_WithNullUserId_ReturnsZero() {
        // Act
        int result = service.deleteUserRoles(null);

        // Assert
        assertEquals(0, result);
        verify(userRoleMapper, never()).deleteByUserId(any());
    }

    @Test
    void deleteUserRoles_NoRolesToDelete_ReturnsZero() {
        // Arrange
        when(userRoleMapper.deleteByUserId(testUserId)).thenReturn(0);

        // Act
        int result = service.deleteUserRoles(testUserId);

        // Assert
        assertEquals(0, result);
        verify(userRoleMapper).deleteByUserId(testUserId);
    }

    // ========================================
    // 涓存椂瑙掕壊绠$悊鍐欐搷浣滄祴锟?
    // ========================================

    @Test
    void extendTemporaryRole_WithValidParameters_ReturnsUpdatedCount() {
        // Arrange
        LocalDateTime newExpireTime = LocalDateTime.now().plusDays(60);
        when(userRoleMapper.extendTemporaryRole(testUserId, testRoleId, newExpireTime)).thenReturn(1);

        // Act
        int result = service.extendTemporaryRole(testUserId, testRoleId, newExpireTime);

        // Assert
        assertEquals(1, result);
        verify(userRoleMapper).extendTemporaryRole(testUserId, testRoleId, newExpireTime);
    }

    @Test
    void extendTemporaryRole_WithNullUserId_ReturnsZero() {
        // Act
        int result = service.extendTemporaryRole(null, testRoleId, testExpireTime);

        // Assert
        assertEquals(0, result);
        verify(userRoleMapper, never()).extendTemporaryRole(any(), any(), any());
    }

    @Test
    void extendTemporaryRole_WithNullRoleId_ReturnsZero() {
        // Act
        int result = service.extendTemporaryRole(testUserId, null, testExpireTime);

        // Assert
        assertEquals(0, result);
        verify(userRoleMapper, never()).extendTemporaryRole(any(), any(), any());
    }

    @Test
    void extendTemporaryRole_WithNullExpireTime_ReturnsZero() {
        // Act
        int result = service.extendTemporaryRole(testUserId, testRoleId, null);

        // Assert
        assertEquals(0, result);
        verify(userRoleMapper, never()).extendTemporaryRole(any(), any(), any());
    }

    @Test
    void extendTemporaryRole_RoleNotFound_ReturnsZero() {
        // Arrange
        when(userRoleMapper.extendTemporaryRole(testUserId, testRoleId, testExpireTime)).thenReturn(0);

        // Act
        int result = service.extendTemporaryRole(testUserId, testRoleId, testExpireTime);

        // Assert
        assertEquals(0, result);
        verify(userRoleMapper).extendTemporaryRole(testUserId, testRoleId, testExpireTime);
    }

    @Test
    void extendTemporaryRole_WithFarFutureDate_Success() {
        // Arrange
        LocalDateTime farFuture = LocalDateTime.now().plusYears(10);
        when(userRoleMapper.extendTemporaryRole(testUserId, testRoleId, farFuture)).thenReturn(1);

        // Act
        int result = service.extendTemporaryRole(testUserId, testRoleId, farFuture);

        // Assert
        assertEquals(1, result);
        verify(userRoleMapper).extendTemporaryRole(testUserId, testRoleId, farFuture);
    }

    @Test
    void terminateTemporaryRole_WithValidParameters_ReturnsUpdatedCount() {
        // Arrange
        when(userRoleMapper.terminateTemporaryRole(testUserId, testRoleId)).thenReturn(1);

        // Act
        int result = service.terminateTemporaryRole(testUserId, testRoleId);

        // Assert
        assertEquals(1, result);
        verify(userRoleMapper).terminateTemporaryRole(testUserId, testRoleId);
    }

    @Test
    void terminateTemporaryRole_WithNullUserId_ReturnsZero() {
        // Act
        int result = service.terminateTemporaryRole(null, testRoleId);

        // Assert
        assertEquals(0, result);
        verify(userRoleMapper, never()).terminateTemporaryRole(any(), any());
    }

    @Test
    void terminateTemporaryRole_WithNullRoleId_ReturnsZero() {
        // Act
        int result = service.terminateTemporaryRole(testUserId, null);

        // Assert
        assertEquals(0, result);
        verify(userRoleMapper, never()).terminateTemporaryRole(any(), any());
    }

    @Test
    void terminateTemporaryRole_RoleNotFound_ReturnsZero() {
        // Arrange
        when(userRoleMapper.terminateTemporaryRole(testUserId, testRoleId)).thenReturn(0);

        // Act
        int result = service.terminateTemporaryRole(testUserId, testRoleId);

        // Assert
        assertEquals(0, result);
        verify(userRoleMapper).terminateTemporaryRole(testUserId, testRoleId);
    }

    @Test
    void terminateTemporaryRole_AlreadyTerminated_ReturnsZero() {
        // Arrange
        when(userRoleMapper.terminateTemporaryRole(testUserId, testRoleId)).thenReturn(0);

        // Act
        int result = service.terminateTemporaryRole(testUserId, testRoleId);

        // Assert
        assertEquals(0, result);
        verify(userRoleMapper).terminateTemporaryRole(testUserId, testRoleId);
    }

    // ========================================
    // 浜嬪姟鍥炴粴娴嬭瘯
    // ========================================

    @Test
    void batchInsertUserRoles_ThrowsException_ShouldRollback() {
        // Arrange
        when(userRoleMapper.batchInsert(testUserId, testRoleIds, testCreateBy))
                .thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        assertThrows(RuntimeException.class, () ->
                service.batchInsertUserRoles(testUserId, testRoleIds, testCreateBy)
        );

        verify(userRoleMapper).batchInsert(testUserId, testRoleIds, testCreateBy);
    }

    @Test
    void deleteUserRoles_ThrowsException_ShouldRollback() {
        // Arrange
        when(userRoleMapper.deleteByUserId(testUserId))
                .thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        assertThrows(RuntimeException.class, () ->
                service.deleteUserRoles(testUserId)
        );

        verify(userRoleMapper).deleteByUserId(testUserId);
    }

    // ========================================
    // 杈圭晫鏉′欢娴嬭瘯
    // ========================================

    @Test
    void batchInsertUserRoles_WithLargeNumberOfRoles_Success() {
        // Arrange
        List<UUID> manyRoles = Arrays.asList(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID() // 10 roles
        );
        when(userRoleMapper.batchInsert(testUserId, manyRoles, testCreateBy)).thenReturn(10);

        // Act
        int result = service.batchInsertUserRoles(testUserId, manyRoles, testCreateBy);

        // Assert
        assertEquals(10, result);
        verify(userRoleMapper).batchInsert(testUserId, manyRoles, testCreateBy);
    }

    @Test
    void batchInsertTemporaryUserRoles_WithSameEffectiveAndExpireTime_Success() {
        // Arrange
        LocalDateTime sameTime = LocalDateTime.now().plusDays(1);
        when(userRoleMapper.batchInsertTemporary(
                testUserId, testRoleIds, sameTime, sameTime, testCreateBy
        )).thenReturn(3);

        // Act
        int result = service.batchInsertTemporaryUserRoles(
                testUserId, testRoleIds, sameTime, sameTime, testCreateBy
        );

        // Assert
        assertEquals(3, result);
        verify(userRoleMapper).batchInsertTemporary(
                testUserId, testRoleIds, sameTime, sameTime, testCreateBy
        );
    }

    @Test
    void extendTemporaryRole_ExtendingByOneDay_Success() {
        // Arrange
        LocalDateTime tomorrow = LocalDateTime.now().plusDays(1);
        when(userRoleMapper.extendTemporaryRole(testUserId, testRoleId, tomorrow)).thenReturn(1);

        // Act
        int result = service.extendTemporaryRole(testUserId, testRoleId, tomorrow);

        // Assert
        assertEquals(1, result);
        verify(userRoleMapper).extendTemporaryRole(testUserId, testRoleId, tomorrow);
    }
}
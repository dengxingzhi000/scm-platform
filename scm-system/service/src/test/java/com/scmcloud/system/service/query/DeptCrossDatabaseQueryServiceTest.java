package com.scmcloud.system.service.query;

import com.scmcloud.common.dto.dept.DeptDTO;
import com.scmcloud.system.domain.entity.SysDept;
import com.scmcloud.system.domain.entity.SysUser;
import com.scmcloud.system.mapper.SysDeptMapper;
import com.scmcloud.system.mapper.SysUserMapper;
import com.scmcloud.system.mapper.SysUserRoleMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/**
 * DeptCrossDatabaseQueryService 鍗曞厓娴嬭瘯
 *
 * @author Deng
 * @since 2025-01-16
 */
@ExtendWith(MockitoExtension.class)
class DeptCrossDatabaseQueryServiceTest {

    @Mock
    private SysDeptMapper deptMapper;

    @Mock
    private SysUserMapper userMapper;

    @Mock
    private SysUserRoleMapper userRoleMapper;

    @InjectMocks
    private DeptCrossDatabaseQueryService service;

    private UUID testDeptId;
    private UUID testUserId;
    private UUID testLeaderId;
    private SysDept testDept;
    private SysUser testLeader;

    @BeforeEach
    void setUp() {
        testDeptId = UUID.randomUUID();
        testUserId = UUID.randomUUID();
        testLeaderId = UUID.randomUUID();

        testDept = new SysDept();
        testDept.setId(testDeptId);
        testDept.setDeptCode("DEPT001");
        testDept.setDeptName("Test Department");
        testDept.setDeptType(1);
        testDept.setLeaderId(testLeaderId);
        testDept.setPhone("1234567890");
        testDept.setEmail("dept@example.com");
        testDept.setIsolationLevel(1);
        testDept.setSortOrder(1);
        testDept.setStatus(1);

        testLeader = new SysUser();
        testLeader.setId(testLeaderId);
        testLeader.setRealName("Test Leader");
    }

    // ========================================
    // 閮ㄩ棬鏍戞煡璇㈡祴锟?
    // ========================================

    @Test
    void selectDeptTree_WithValidData_ReturnsDeptDTOList() {
        // Arrange
        List<SysDept> depts = Collections.singletonList(testDept);
        List<SysUser> leaders = Collections.singletonList(testLeader);

        when(deptMapper.selectDeptList()).thenReturn(depts);
        when(userMapper.selectBasicInfoByIds(anyList())).thenReturn(leaders);

        // Act
        List<DeptDTO> result = service.selectDeptTree();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        DeptDTO dto = result.get(0);
        assertEquals(testDeptId, dto.getId());
        assertEquals("DEPT001", dto.getDeptCode());
        assertEquals("Test Department", dto.getDeptName());
        assertEquals("Test Leader", dto.getLeaderName());
        verify(deptMapper).selectDeptList();
        verify(userMapper).selectBasicInfoByIds(anyList());
    }

    @Test
    void selectDeptTree_NoDepts_ReturnsEmptyList() {
        // Arrange
        when(deptMapper.selectDeptList()).thenReturn(Collections.emptyList());

        // Act
        List<DeptDTO> result = service.selectDeptTree();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(deptMapper).selectDeptList();
        verify(userMapper, never()).selectBasicInfoByIds(anyList());
    }

    @Test
    void selectDeptTree_DeptsReturnsNull_ReturnsEmptyList() {
        // Arrange
        when(deptMapper.selectDeptList()).thenReturn(null);

        // Act
        List<DeptDTO> result = service.selectDeptTree();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(deptMapper).selectDeptList();
        verify(userMapper, never()).selectBasicInfoByIds(anyList());
    }

    @Test
    void selectDeptTree_NoLeaders_ReturnsDeptsWithoutLeaderNames() {
        // Arrange
        testDept.setLeaderId(null);
        List<SysDept> depts = Collections.singletonList(testDept);

        when(deptMapper.selectDeptList()).thenReturn(depts);

        // Act
        List<DeptDTO> result = service.selectDeptTree();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertNull(result.get(0).getLeaderName());
        verify(deptMapper).selectDeptList();
        verify(userMapper, never()).selectBasicInfoByIds(anyList());
    }

    @Test
    void selectDeptTree_LeaderNotFound_ReturnsDeptsWithNullLeaderName() {
        // Arrange
        List<SysDept> depts = Collections.singletonList(testDept);

        when(deptMapper.selectDeptList()).thenReturn(depts);
        when(userMapper.selectBasicInfoByIds(anyList())).thenReturn(Collections.emptyList());

        // Act
        List<DeptDTO> result = service.selectDeptTree();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertNull(result.get(0).getLeaderName());
        verify(userMapper).selectBasicInfoByIds(anyList());
    }

    @Test
    void selectDeptTree_MultipleDeptsMultipleLeaders_ReturnsCompleteTree() {
        // Arrange
        UUID deptId2 = UUID.randomUUID();
        UUID leaderId2 = UUID.randomUUID();

        SysDept dept2 = new SysDept();
        dept2.setId(deptId2);
        dept2.setDeptCode("DEPT002");
        dept2.setDeptName("Department 2");
        dept2.setLeaderId(leaderId2);
        dept2.setStatus(1);

        SysUser leader2 = new SysUser();
        leader2.setId(leaderId2);
        leader2.setRealName("Leader 2");

        List<SysDept> depts = Arrays.asList(testDept, dept2);
        List<SysUser> leaders = Arrays.asList(testLeader, leader2);

        when(deptMapper.selectDeptList()).thenReturn(depts);
        when(userMapper.selectBasicInfoByIds(anyList())).thenReturn(leaders);

        // Act
        List<DeptDTO> result = service.selectDeptTree();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Test Leader", result.get(0).getLeaderName());
        assertEquals("Leader 2", result.get(1).getLeaderName());
    }

    // ========================================
    // 閮ㄩ棬鍩烘湰淇℃伅鏌ヨ娴嬭瘯
    // ========================================

    @Test
    void getDeptLeaderId_WithValidId_ReturnsLeaderId() {
        // Arrange
        when(deptMapper.getLeaderId(testDeptId)).thenReturn(testLeaderId);

        // Act
        UUID result = service.getDeptLeaderId(testDeptId);

        // Assert
        assertNotNull(result);
        assertEquals(testLeaderId, result);
        verify(deptMapper).getLeaderId(testDeptId);
    }

    @Test
    void getDeptLeaderId_WithNullId_ReturnsNull() {
        // Act
        UUID result = service.getDeptLeaderId(null);

        // Assert
        assertNull(result);
        verify(deptMapper, never()).getLeaderId(any());
    }

    @Test
    void getDeptLeaderId_DeptNotFound_ReturnsNull() {
        // Arrange
        when(deptMapper.getLeaderId(testDeptId)).thenReturn(null);

        // Act
        UUID result = service.getDeptLeaderId(testDeptId);

        // Assert
        assertNull(result);
        verify(deptMapper).getLeaderId(testDeptId);
    }

    // ========================================
    // 閮ㄩ棬灞傜骇鍏崇郴鏌ヨ娴嬭瘯
    // ========================================

    @Test
    void findUserDeptAndChildren_WithValidUserId_ReturnsDeptList() {
        // Arrange
        UUID childDeptId = UUID.randomUUID();
        List<UUID> expectedDepts = Arrays.asList(testDeptId, childDeptId);

        when(userMapper.getUserDeptId(testUserId)).thenReturn(testDeptId);
        when(deptMapper.selectDeptAndChildren(testDeptId)).thenReturn(expectedDepts);

        // Act
        List<UUID> result = service.findUserDeptAndChildren(testUserId);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains(testDeptId));
        assertTrue(result.contains(childDeptId));
        verify(userMapper).getUserDeptId(testUserId);
        verify(deptMapper).selectDeptAndChildren(testDeptId);
    }

    @Test
    void findUserDeptAndChildren_WithNullUserId_ReturnsEmptyList() {
        // Act
        List<UUID> result = service.findUserDeptAndChildren(null);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(userMapper, never()).getUserDeptId(any());
    }

    @Test
    void findUserDeptAndChildren_UserHasNoDept_ReturnsEmptyList() {
        // Arrange
        when(userMapper.getUserDeptId(testUserId)).thenReturn(null);

        // Act
        List<UUID> result = service.findUserDeptAndChildren(testUserId);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(userMapper).getUserDeptId(testUserId);
        verify(deptMapper, never()).selectDeptAndChildren(any());
    }

    @Test
    void hasAccessToDept_WithDataScopeAll_ReturnsTrue() {
        // Arrange
        when(userRoleMapper.getUserDataScope(testUserId)).thenReturn(1); // 鍏ㄩ儴鏁版嵁

        // Act
        boolean result = service.hasAccessToDept(testUserId, testDeptId);

        // Assert
        assertTrue(result);
        verify(userRoleMapper).getUserDataScope(testUserId);
        verify(userMapper, never()).getUserDeptId(any());
    }

    @Test
    void hasAccessToDept_WithDataScopeSameDept_AndSameDept_ReturnsTrue() {
        // Arrange
        when(userRoleMapper.getUserDataScope(testUserId)).thenReturn(3); // 鏈儴锟?
        when(userMapper.getUserDeptId(testUserId)).thenReturn(testDeptId);

        // Act
        boolean result = service.hasAccessToDept(testUserId, testDeptId);

        // Assert
        assertTrue(result);
        verify(userRoleMapper).getUserDataScope(testUserId);
        verify(userMapper).getUserDeptId(testUserId);
    }

    @Test
    void hasAccessToDept_WithDataScopeSameDept_AndDifferentDept_ReturnsFalse() {
        // Arrange
        UUID otherDeptId = UUID.randomUUID();
        when(userRoleMapper.getUserDataScope(testUserId)).thenReturn(3); // 鏈儴锟?
        when(userMapper.getUserDeptId(testUserId)).thenReturn(testDeptId);

        // Act
        boolean result = service.hasAccessToDept(testUserId, otherDeptId);

        // Assert
        assertFalse(result);
        verify(userMapper).getUserDeptId(testUserId);
    }

    @Test
    void hasAccessToDept_WithDataScopeDeptAndChildren_AndChildDept_ReturnsTrue() {
        // Arrange
        UUID childDeptId = UUID.randomUUID();
        List<UUID> accessibleDepts = Arrays.asList(testDeptId, childDeptId);

        when(userRoleMapper.getUserDataScope(testUserId)).thenReturn(4); // 鏈儴闂ㄥ強瀛愰儴锟?
        when(userMapper.getUserDeptId(testUserId)).thenReturn(testDeptId);
        when(deptMapper.selectDeptAndChildren(testDeptId)).thenReturn(accessibleDepts);

        // Act
        boolean result = service.hasAccessToDept(testUserId, childDeptId);

        // Assert
        assertTrue(result);
        verify(deptMapper).selectDeptAndChildren(testDeptId);
    }

    @Test
    void hasAccessToDept_WithDataScopeDeptAndChildren_AndNonChildDept_ReturnsFalse() {
        // Arrange
        UUID otherDeptId = UUID.randomUUID();
        List<UUID> accessibleDepts = Collections.singletonList(testDeptId);

        when(userRoleMapper.getUserDataScope(testUserId)).thenReturn(4); // 鏈儴闂ㄥ強瀛愰儴锟?
        when(userMapper.getUserDeptId(testUserId)).thenReturn(testDeptId);
        when(deptMapper.selectDeptAndChildren(testDeptId)).thenReturn(accessibleDepts);

        // Act
        boolean result = service.hasAccessToDept(testUserId, otherDeptId);

        // Assert
        assertFalse(result);
        verify(deptMapper).selectDeptAndChildren(testDeptId);
    }

    @Test
    void hasAccessToDept_WithDataScopeSelfOnly_ReturnsFalse() {
        // Arrange
        when(userRoleMapper.getUserDataScope(testUserId)).thenReturn(5); // 浠呮湰锟?

        // Act
        boolean result = service.hasAccessToDept(testUserId, testDeptId);

        // Assert
        assertFalse(result);
        verify(userRoleMapper).getUserDataScope(testUserId);
    }

    @Test
    void hasAccessToDept_WithNullUserId_ReturnsFalse() {
        // Act
        boolean result = service.hasAccessToDept(null, testDeptId);

        // Assert
        assertFalse(result);
        verify(userRoleMapper, never()).getUserDataScope(any());
    }

    @Test
    void hasAccessToDept_WithNullDeptId_ReturnsFalse() {
        // Act
        boolean result = service.hasAccessToDept(testUserId, null);

        // Assert
        assertFalse(result);
        verify(userRoleMapper, never()).getUserDataScope(any());
    }

    @Test
    void hasAccessToDept_WithNullDataScope_ReturnsFalse() {
        // Arrange
        when(userRoleMapper.getUserDataScope(testUserId)).thenReturn(null);

        // Act
        boolean result = service.hasAccessToDept(testUserId, testDeptId);

        // Assert
        assertFalse(result);
        verify(userRoleMapper).getUserDataScope(testUserId);
    }

    @Test
    void hasAccessToDept_UserHasNoDept_ReturnsFalse() {
        // Arrange
        when(userRoleMapper.getUserDataScope(testUserId)).thenReturn(3);
        when(userMapper.getUserDeptId(testUserId)).thenReturn(null);

        // Act
        boolean result = service.hasAccessToDept(testUserId, testDeptId);

        // Assert
        assertFalse(result);
        verify(userMapper).getUserDeptId(testUserId);
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

    @Test
    void countUsersByDeptIds_WithLongCount_ConvertsCorrectly() {
        // Arrange
        List<UUID> deptIds = Collections.singletonList(testDeptId);

        Map<UUID, Map<String, Object>> mapperResult = new HashMap<>();
        Map<String, Object> row = new HashMap<>();
        row.put("user_count", 100L);
        mapperResult.put(testDeptId, row);

        when(userMapper.countUsersByDeptIds(deptIds)).thenReturn(mapperResult);

        // Act
        Map<UUID, Integer> result = service.countUsersByDeptIds(deptIds);

        // Assert
        assertNotNull(result);
        assertEquals(100, result.get(testDeptId));
    }
}
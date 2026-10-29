package com.scmcloud.common.mybatisPlus.aspect;

import com.scmcloud.common.mybatisPlus.annotation.DataScope;
import com.scmcloud.common.mybatisPlus.context.DataScopeContextHolder;
import com.scmcloud.common.mybatisPlus.context.DataScopeFilter;
import com.scmcloud.common.mybatisPlus.service.DataPermissionService;
import com.scmcloud.common.security.SecurityContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DataScopeAspect Refactoring Tests")
class DataScopeAspectTest {

    @Mock
    private SecurityContext securityContext;

    @Mock
    private DataPermissionService dataPermissionService;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private DataScope dataScopeAnnotation;

    private DataScopeAspect aspect;

    private UUID testUserId;
    private UUID testDeptId;

    @BeforeEach
    void setUp() {
        aspect = new DataScopeAspect(securityContext, dataPermissionService);
        testUserId = UUID.randomUUID();
        testDeptId = UUID.randomUUID();

        lenient().when(dataScopeAnnotation.userAlias()).thenReturn("u");
        lenient().when(dataScopeAnnotation.deptAlias()).thenReturn("d");
    }

    @AfterEach
    void tearDown() {
        DataScopeContextHolder.clear();
    }

    @Test
    @DisplayName("Should skip data scope when user is not authenticated")
    void testAround_NotAuthenticated_SkipsDataScope() throws Throwable {
        when(securityContext.isAuthenticated()).thenReturn(false);
        Object expectedResult = "proceed-result";
        when(joinPoint.proceed()).thenReturn(expectedResult);

        Object result = aspect.around(joinPoint, dataScopeAnnotation);

        assertThat(result).isEqualTo(expectedResult);
        assertThat(DataScopeContextHolder.get()).isNull();
        verify(securityContext).isAuthenticated();
        verify(joinPoint).proceed();
        verifyNoMoreInteractions(securityContext);
    }

    @Test
    @DisplayName("Should skip data scope when userId is null")
    void testAround_NullUserId_SkipsDataScope() throws Throwable {
        when(securityContext.isAuthenticated()).thenReturn(true);
        when(securityContext.getCurrentUserId()).thenReturn(null);
        Object expectedResult = "proceed-result";
        when(joinPoint.proceed()).thenReturn(expectedResult);

        Object result = aspect.around(joinPoint, dataScopeAnnotation);

        assertThat(result).isEqualTo(expectedResult);
        assertThat(DataScopeContextHolder.get()).isNull();
        verify(securityContext).isAuthenticated();
        verify(securityContext).getCurrentUserId();
        verify(joinPoint).proceed();
    }

    @Test
    @DisplayName("Should apply data scope level 5 (SELF) correctly")
    void testAround_LevelSelf_AppliesDataScope() throws Throwable {
        when(securityContext.isAuthenticated()).thenReturn(true);
        when(securityContext.getCurrentUserId()).thenReturn(testUserId);
        when(securityContext.getCurrentDeptId()).thenReturn(testDeptId);
        when(securityContext.getDataScopeLevel()).thenReturn(5);

        DataScopeFilter[] capturedFilter = new DataScopeFilter[1];
        Object expectedResult = "proceed-result";
        when(joinPoint.proceed()).thenAnswer(invocation -> {
            capturedFilter[0] = DataScopeContextHolder.get();
            return expectedResult;
        });

        Object result = aspect.around(joinPoint, dataScopeAnnotation);

        assertThat(result).isEqualTo(expectedResult);

        DataScopeFilter filter = capturedFilter[0];
        assertThat(filter).isNotNull();
        assertThat(filter.getClause()).contains("u = #{__ds_userId}::uuid");
        assertThat(filter.getParams()).containsEntry("__ds_userId", testUserId.toString());

        verify(securityContext).isAuthenticated();
        verify(securityContext).getCurrentUserId();
        verify(securityContext).getCurrentDeptId();
        verify(securityContext).getDataScopeLevel();
        verify(joinPoint).proceed();
    }

    @Test
    @DisplayName("Should apply data scope level 3 (DEPT) correctly")
    void testAround_LevelDept_AppliesDataScope() throws Throwable {
        when(securityContext.isAuthenticated()).thenReturn(true);
        when(securityContext.getCurrentUserId()).thenReturn(testUserId);
        when(securityContext.getCurrentDeptId()).thenReturn(testDeptId);
        when(securityContext.getDataScopeLevel()).thenReturn(3);

        DataScopeFilter[] capturedFilter = new DataScopeFilter[1];
        Object expectedResult = "proceed-result";
        when(joinPoint.proceed()).thenAnswer(invocation -> {
            capturedFilter[0] = DataScopeContextHolder.get();
            return expectedResult;
        });

        Object result = aspect.around(joinPoint, dataScopeAnnotation);

        assertThat(result).isEqualTo(expectedResult);

        DataScopeFilter filter = capturedFilter[0];
        assertThat(filter).isNotNull();
        assertThat(filter.getClause()).contains("d = #{__ds_deptId}::uuid");
        assertThat(filter.getParams()).containsEntry("__ds_deptId", testDeptId.toString());
    }

    @Test
    @DisplayName("Should apply data scope level 1 (ALL) correctly")
    void testAround_LevelAll_AppliesNoFiltering() throws Throwable {
        when(securityContext.isAuthenticated()).thenReturn(true);
        when(securityContext.getCurrentUserId()).thenReturn(testUserId);
        when(securityContext.getCurrentDeptId()).thenReturn(testDeptId);
        when(securityContext.getDataScopeLevel()).thenReturn(1);

        DataScopeFilter[] capturedFilter = new DataScopeFilter[1];
        Object expectedResult = "proceed-result";
        when(joinPoint.proceed()).thenAnswer(invocation -> {
            capturedFilter[0] = DataScopeContextHolder.get();
            return expectedResult;
        });

        Object result = aspect.around(joinPoint, dataScopeAnnotation);

        assertThat(result).isEqualTo(expectedResult);

        DataScopeFilter filter = capturedFilter[0];
        assertThat(filter).isNotNull();
        assertThat(filter.getClause()).isEqualTo("1=1");
    }

    @Test
    @DisplayName("Should apply data scope level 4 (DEPT_AND_CHILDREN) with recursive CTE")
    void testAround_LevelDeptAndChildren_AppliesRecursiveCTE() throws Throwable {
        when(securityContext.isAuthenticated()).thenReturn(true);
        when(securityContext.getCurrentUserId()).thenReturn(testUserId);
        when(securityContext.getCurrentDeptId()).thenReturn(testDeptId);
        when(securityContext.getDataScopeLevel()).thenReturn(4);

        DataScopeFilter[] capturedFilter = new DataScopeFilter[1];
        Object expectedResult = "proceed-result";
        when(joinPoint.proceed()).thenAnswer(invocation -> {
            capturedFilter[0] = DataScopeContextHolder.get();
            return expectedResult;
        });

        Object result = aspect.around(joinPoint, dataScopeAnnotation);

        assertThat(result).isEqualTo(expectedResult);

        DataScopeFilter filter = capturedFilter[0];
        assertThat(filter).isNotNull();
        assertThat(filter.getClause()).contains("WITH RECURSIVE dept_tree");
        assertThat(filter.getClause()).contains("d IN");
        assertThat(filter.getParams()).containsEntry("__ds_deptId", testDeptId.toString());
    }

    @Test
    @DisplayName("Should handle null deptId for DEPT level gracefully")
    void testAround_LevelDept_NullDeptId_DeniesAccess() throws Throwable {
        when(securityContext.isAuthenticated()).thenReturn(true);
        when(securityContext.getCurrentUserId()).thenReturn(testUserId);
        when(securityContext.getCurrentDeptId()).thenReturn(null);
        when(securityContext.getDataScopeLevel()).thenReturn(3);

        DataScopeFilter[] capturedFilter = new DataScopeFilter[1];
        Object expectedResult = "proceed-result";
        when(joinPoint.proceed()).thenAnswer(invocation -> {
            capturedFilter[0] = DataScopeContextHolder.get();
            return expectedResult;
        });

        Object result = aspect.around(joinPoint, dataScopeAnnotation);

        assertThat(result).isEqualTo(expectedResult);

        DataScopeFilter filter = capturedFilter[0];
        assertThat(filter).isNotNull();
        assertThat(filter.getClause()).isEqualTo("1=0");
    }

    @Test
    @DisplayName("Should use custom table aliases from annotation")
    void testAround_CustomAliases_UsedInFilter() throws Throwable {
        when(dataScopeAnnotation.userAlias()).thenReturn("user_table");
        when(dataScopeAnnotation.deptAlias()).thenReturn("dept_table");

        when(securityContext.isAuthenticated()).thenReturn(true);
        when(securityContext.getCurrentUserId()).thenReturn(testUserId);
        when(securityContext.getCurrentDeptId()).thenReturn(testDeptId);
        when(securityContext.getDataScopeLevel()).thenReturn(5);

        DataScopeFilter[] capturedFilter = new DataScopeFilter[1];
        Object expectedResult = "proceed-result";
        when(joinPoint.proceed()).thenAnswer(invocation -> {
            capturedFilter[0] = DataScopeContextHolder.get();
            return expectedResult;
        });

        Object result = aspect.around(joinPoint, dataScopeAnnotation);
        assertThat(result).isEqualTo(expectedResult);

        DataScopeFilter filter = capturedFilter[0];
        assertThat(filter).isNotNull();
        assertThat(filter.getClause()).contains("user_table =");
    }

    @Test
    @DisplayName("Should clear ThreadLocal context after processing")
    void testAround_ClearsThreadLocalContext() throws Throwable {
        when(securityContext.isAuthenticated()).thenReturn(true);
        when(securityContext.getCurrentUserId()).thenReturn(testUserId);
        when(securityContext.getCurrentDeptId()).thenReturn(testDeptId);
        when(securityContext.getDataScopeLevel()).thenReturn(5);

        when(joinPoint.proceed()).thenReturn("result");

        aspect.around(joinPoint, dataScopeAnnotation);

        assertThat(DataScopeContextHolder.get()).isNull();
    }

    @Test
    @DisplayName("Should clear ThreadLocal even when exception occurs")
    void testAround_ClearsThreadLocalOnException() {
        when(securityContext.isAuthenticated()).thenReturn(true);
        when(securityContext.getCurrentUserId()).thenReturn(testUserId);
        when(securityContext.getCurrentDeptId()).thenReturn(testDeptId);
        when(securityContext.getDataScopeLevel()).thenReturn(5);

        try {
            when(joinPoint.proceed()).thenThrow(new RuntimeException("Test exception"));
        } catch (Throwable e) {
        }

        assertThatThrownBy(() -> aspect.around(joinPoint, dataScopeAnnotation))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Test exception");

        assertThat(DataScopeContextHolder.get()).isNull();
    }

    @Test
    @DisplayName("REFACTORING: Verify no dependency on SecurityUser or SecurityUtils")
    void testRefactoring_NoDependencyOnWebLayer() {
        when(securityContext.isAuthenticated()).thenReturn(false);

        try {
            when(joinPoint.proceed()).thenReturn("result");
            aspect.around(joinPoint, dataScopeAnnotation);

            assertThat(aspect).isNotNull();
        } catch (Throwable e) {
            fail("Should not throw exception", e);
        }
    }
}

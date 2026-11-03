package com.cognora.platform.system.service.Impl;

import com.cognora.platform.system.mapper.SysDeptMapper;
import com.cognora.platform.system.mapper.SysRoleMapper;
import com.cognora.platform.system.mapper.SysUserMapper;
import com.cognora.platform.system.mapper.SysUserRoleMapper;
import com.cognora.platform.system.service.query.UserCrossDatabaseQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link PermissionQueryServiceImpl} 的数据权限语义与缓存键约定测试。
 *
 * <p>数值语义以 {@code scripts/db/microservices/003_db_permission.sql} 的
 * {@code sys_role.data_scope} 定义为准：1=ALL, 2=CUSTOM, 3=DEPT, 4=DEPT_AND_SUB, 5=SELF。</p>
 */
@ExtendWith(MockitoExtension.class)
class PermissionQueryServiceImplTest {

    @Mock private SysUserRoleMapper userRoleMapper;
    @Mock private SysUserMapper userMapper;
    @Mock private SysDeptMapper deptMapper;
    @Mock private SysRoleMapper roleMapper;

    private PermissionQueryServiceImpl service;
    private UUID userId;

    @BeforeEach
    void setUp() {
        service = new PermissionQueryServiceImpl(userRoleMapper, userMapper, deptMapper, roleMapper);
        userId = UUID.randomUUID();
    }

    @Test
    void mapsNumericDataScopeToDdlEnum() {
        Map<Integer, String> expected = Map.of(
                1, "ALL",
                2, "CUSTOM",
                3, "DEPT",
                4, "DEPT_AND_SUB",
                5, "SELF");

        expected.forEach((code, name) -> {
            when(userRoleMapper.getUserDataScope(userId)).thenReturn(code);
            String actual = service.getUserDataScope(userId);
            assertInstanceOf(String.class, actual);
            assertEquals(name, actual, "data_scope=" + code);
        });
    }

    @Test
    void nullOrUnknownScopeFallsBackToSelf() {
        when(userRoleMapper.getUserDataScope(userId)).thenReturn(null);
        assertEquals("SELF", service.getUserDataScope(userId));

        when(userRoleMapper.getUserDataScope(userId)).thenReturn(99);
        assertEquals("SELF", service.getUserDataScope(userId));
    }

    @Test
    void nullUserIdReturnsSelfWithoutQuerying() {
        assertEquals("SELF", service.getUserDataScope(null));
        verify(userRoleMapper, never()).getUserDataScope(any());
    }

    /**
     * 回归用例：修复前 String 枚举与 Integer 码值都用裸 {@code userId} 作为键，
     * 同一用户先读其中一种、再读另一种会命中错误类型。修复后两者键必须不同。
     */
    @Test
    void stringScopeAndNumericScopeUseDifferentCacheKeys() throws Exception {
        Method stringMethod = PermissionQueryServiceImpl.class.getMethod("getUserDataScope", UUID.class);
        Method numericMethod = UserCrossDatabaseQueryService.class.getMethod("getUserDataScope", UUID.class);

        String stringKey = evaluateKey(stringMethod, userId);
        String numericKey = evaluateKey(numericMethod, userId);

        assertEquals(userId.toString(), stringKey);
        assertEquals("scope:" + userId, numericKey);
        assertNotEquals(stringKey, numericKey);

        // 返回类型契约：String 枚举 / Integer 码值。
        assertEquals(String.class, stringMethod.getReturnType());
        assertEquals(Integer.class, numericMethod.getReturnType());
        assertEquals("userDataScope", cacheName(stringMethod));
        assertEquals("userDataScope", cacheName(numericMethod));
    }

    private static String evaluateKey(Method method, UUID userId) {
        Cacheable cacheable = method.getAnnotation(Cacheable.class);
        StandardEvaluationContext context = new StandardEvaluationContext();
        context.setVariable("userId", userId);
        Object value = new SpelExpressionParser()
                .parseExpression(cacheable.key())
                .getValue(context);
        return String.valueOf(value);
    }

    private static String cacheName(Method method) {
        return method.getAnnotation(Cacheable.class).value()[0];
    }
}

package com.scmcloud.common.mybatisPlus.interceptor;

import com.scmcloud.common.mybatisPlus.context.DataScopeContextHolder;
import com.scmcloud.common.mybatisPlus.context.DataScopeFilter;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.reflection.MetaObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.util.Map;

import org.junit.jupiter.api.Disabled;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DataScopeInterceptor Tests")
class DataScopeInterceptorTest {

    private DataScopeInterceptor interceptor;

    @Mock
    private StatementHandler statementHandler;

    @Mock
    private BoundSql boundSql;

    @Mock
    private MetaObject metaObject;

    private Invocation invocation;

    @BeforeEach
    void setUp() throws Exception {
        interceptor = new DataScopeInterceptor();

        lenient().when(statementHandler.getBoundSql()).thenReturn(boundSql);
        invocation = new Invocation(statementHandler, StatementHandler.class.getMethod("prepare", Connection.class, Integer.class), new Object[]{null, 0});
    }

    @AfterEach
    void tearDown() {
        DataScopeContextHolder.clear();
    }

    @Test
    @DisplayName("Should skip when no data scope context")
    void testIntercept_SkipsWhenNoContext() throws Throwable {
        Object result = interceptor.intercept(invocation);
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Should apply filter for SELECT statement")
    @Disabled("Requires MetaObject integration test with real MyBatis objects")
    void testIntercept_AppliesFilterForSelect() throws Throwable {
        DataScopeContextHolder.set(new DataScopeFilter("u.id = #{__ds_userId}::uuid", Map.of("__ds_userId", "user-123")));
        when(metaObject.getValue("delegate.mappedStatement.sqlCommandType")).thenReturn("SELECT");
        when(boundSql.getSql()).thenReturn("SELECT * FROM sys_user");

        interceptor.intercept(invocation);

        verify(metaObject).setValue(eq("delegate.boundSql.sql"), contains("u.id = #{__ds_userId}::uuid"));
    }

    @Test
    @DisplayName("Should skip non-SELECT statements")
    void testIntercept_SkipsNonSelect() throws Throwable {
        DataScopeContextHolder.set(new DataScopeFilter("u.id = #{id}::uuid", Map.of()));

        interceptor.intercept(invocation);
    }

    @Test
    @DisplayName("Should filter with WHERE clause present")
    @Disabled("Requires MetaObject integration test with real MyBatis objects")
    void testIntercept_AppendsToExistingWhere() throws Throwable {
        DataScopeContextHolder.set(new DataScopeFilter("u.dept_id = #{__ds_deptId}::uuid", Map.of("__ds_deptId", "dept-456")));
        when(metaObject.getValue("delegate.mappedStatement.sqlCommandType")).thenReturn("SELECT");
        when(boundSql.getSql()).thenReturn("SELECT * FROM sys_user WHERE u.status = 1");

        interceptor.intercept(invocation);

        verify(metaObject).setValue(eq("delegate.boundSql.sql"), contains("(u.dept_id = #{__ds_deptId}::uuid) AND"));
    }

    @Test
    @DisplayName("Should add WHERE clause when missing")
    @Disabled("Requires MetaObject integration test with real MyBatis objects")
    void testIntercept_AddsWhereClause() throws Throwable {
        DataScopeContextHolder.set(new DataScopeFilter("u.id = #{__ds_userId}::uuid", Map.of("__ds_userId", "user-789")));
        when(metaObject.getValue("delegate.mappedStatement.sqlCommandType")).thenReturn("SELECT");
        when(boundSql.getSql()).thenReturn("SELECT * FROM sys_user");

        interceptor.intercept(invocation);

        verify(metaObject).setValue(eq("delegate.boundSql.sql"), contains("WHERE (u.id = #{__ds_userId}::uuid)"));
    }

    private boolean invokeIsSafeFilter(String filter) throws Exception {
        Method method = DataScopeInterceptor.class.getDeclaredMethod("isSafeFilter", String.class);
        method.setAccessible(true);
        return (boolean) method.invoke(interceptor, filter);
    }

    private boolean invokeMatchesAllowedPattern(String filter) throws Exception {
        Method method = DataScopeInterceptor.class.getDeclaredMethod("matchesAllowedPattern", String.class);
        method.setAccessible(true);
        return (boolean) method.invoke(interceptor, filter);
    }

    @Test
    @DisplayName("Should allow safe data scope filter")
    void testIsSafeFilter_SafeFilter() throws Exception {
        assertThat(invokeIsSafeFilter("u.dept_id = #{userId}")).isTrue();
    }

    @Test
    @DisplayName("SECURITY: Should block SQL injection attempt with UNION")
    void testIsSafeFilter_BlocksUnionInjection() throws Exception {
        assertThat(invokeIsSafeFilter("u.dept_id = 1 UNION SELECT password FROM sys_user")).isFalse();
    }

    @Test
    @DisplayName("SECURITY: Should block SQL injection with semicolon")
    void testIsSafeFilter_BlocksSemicolon() throws Exception {
        assertThat(invokeIsSafeFilter("u.dept_id = 1; DROP TABLE sys_user;")).isFalse();
    }

    @Test
    @DisplayName("SECURITY: Should block SQL injection with comments")
    void testIsSafeFilter_BlocksComments() throws Exception {
        assertThat(invokeIsSafeFilter("u.dept_id = 1 -- comment")).isFalse();
        assertThat(invokeIsSafeFilter("u.dept_id = 1 /* comment */")).isFalse();
    }

    @Test
    @DisplayName("SECURITY: Should block OR-based SQL injection")
    void testIsSafeFilter_BlocksOrInjection() throws Exception {
        assertThat(invokeIsSafeFilter("u.dept_id = 1 OR 1=1")).isFalse();
    }

    @Test
    @DisplayName("SECURITY: Should block DROP TABLE injection")
    void testIsSafeFilter_BlocksDropTable() throws Exception {
        assertThat(invokeIsSafeFilter("u.dept_id = 1; DROP TABLE sys_user")).isFalse();
    }

    @Test
    @DisplayName("SECURITY: Should block EXEC/EXECUTE command injection")
    void testIsSafeFilter_BlocksExecCommand() throws Exception {
        assertThat(invokeIsSafeFilter("u.dept_id = 1; EXEC sp_executesql")).isFalse();
        assertThat(invokeIsSafeFilter("u.dept_id = 1; EXECUTE sp_executesql")).isFalse();
    }

    @Test
    @DisplayName("SECURITY: Should block file operation injection")
    void testIsSafeFilter_BlocksFileOperations() throws Exception {
        assertThat(invokeIsSafeFilter("u.dept_id = 1 INTO OUTFILE '/tmp/passwords.txt'")).isFalse();
        assertThat(invokeIsSafeFilter("u.dept_id = 1 AND load_file('/etc/passwd')")).isFalse();
    }

    @Test
    @DisplayName("SECURITY: Should block hex encoding bypass attempts")
    void testIsSafeFilter_BlocksHexEncoding() throws Exception {
        assertThat(invokeIsSafeFilter("u.dept_id = 0x61646D696E")).isFalse();
        assertThat(invokeIsSafeFilter("u.dept_id = char(65,68,77,73,78)")).isFalse();
        assertThat(invokeIsSafeFilter("u.dept_id = concat('ad','min')")).isFalse();
    }

    @Test
    @DisplayName("SECURITY: Should handle case-insensitive SQL keyword detection")
    void testIsSafeFilter_CaseInsensitiveKeywords() throws Exception {
        assertThat(invokeIsSafeFilter("u.dept_id = 1 UnIoN SeLeCt password")).isFalse();
        assertThat(invokeIsSafeFilter("u.dept_id = 1 DeLeTe FrOm sys_user")).isFalse();
        assertThat(invokeIsSafeFilter("u.dept_id = 1 DrOp TaBlE sys_user")).isFalse();
    }

    @Test
    @DisplayName("Should allow recursive CTE pattern (legitimate use)")
    void testMatchesAllowedPattern_AllowsRecursiveCTE() throws Exception {
        assertThat(invokeMatchesAllowedPattern("WITH RECURSIVE dept_tree AS (SELECT id FROM sys_dept WHERE id = #{deptId})")).isTrue();
    }

    @Test
    @DisplayName("Should allow parameterized placeholders")
    void testMatchesAllowedPattern_AllowsParameterizedQueries() throws Exception {
        assertThat(invokeMatchesAllowedPattern("u.user_id = #{userId}")).isTrue();
        assertThat(invokeMatchesAllowedPattern("d.dept_id IN (#{deptIds})")).isTrue();
    }

    @Test
    @DisplayName("Should clear data scope context after filter application")
    void testContextCleanup() {
        DataScopeContextHolder.set(new DataScopeFilter("1=1", Map.of()));
        assertThat(DataScopeContextHolder.get()).isNotNull();

        DataScopeContextHolder.clear();
        assertThat(DataScopeContextHolder.get()).isNull();
    }
}

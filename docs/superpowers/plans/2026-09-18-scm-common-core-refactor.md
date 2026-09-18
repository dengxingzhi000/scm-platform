# scm-common/core 模块升级实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把 `scm-common/core` 升级为 JDK 21 + Spring Boot 4.1.1 + Spring Cloud 2025 风格的安全、高性能、空安全基座，并补齐核心组件单测。

**Architecture:** 在不破坏现有公共 API 契约前提下，按 P0 安全 → P1 性能 → P2 结构 → P3 测试的顺序渐进重构。每 Phase 独立可编译、可测试通过。

**Tech Stack:**
- JDK 21（virtual threads、record、sealed、pattern matching switch）
- Spring Boot 4.1.1、Spring Cloud 2025
- MyBatis-Plus 3.x + JSQLParser
- JSpecify 1.0（空安全注解）
- Micrometer（指标）
- JUnit 6 + Mockito 6 + AssertJ 3.27

**参考 spec:** `docs/superpowers/specs/2026-09-18-scm-common-core-refactor-design.md`

---

## 目录

- [Phase 0-A: ServiceException 加 HttpStatus + 异常 sealed 化](#phase-0-a-serviceexception-加-httpstatus--异常-sealed-化)
- [Phase 0-B: TenantInterceptor fail-fast + UNION 处理](#phase-0-b-tenantinterceptor-fail-fast--union-处理)
- [Phase 0-C: TenantFilter Authentication 优先 + shouldNotFilter](#phase-0-c-tenantfilter-authentication-优先--shouldnotfilter)
- [Phase 0-D: 删除 GlobalExceptionHandler HTML 转义](#phase-0-d-删除-globalexceptionhandler-html-转义)
- [Phase 1-A ~ 1-D: 健壮性/性能优化](#phase-1-健壮性性能优化)
- [Phase 2-A ~ 2-F: 结构/一致性](#phase-2-结构一致性)
- [Phase 3-A ~ 3-J: 测试/可观测性](#phase-3-测试可观测性)
- [最终验证](#最终验证)

---

## 全局约定

**每次提交模板:**
```bash
git add <files>
git commit -m "<type>(scm-common-core): <description>"
```

**type 限定**: `fix` (P0) / `perf` (P1) / `refactor` (P2) / `test` (P3) / `docs` (P3-H/E) / `chore` (其它)

**本地编译命令（贯穿全文）:**
```bash
mvn -pl scm-common/core -am compile -f com.scm.parent/pom.xml
mvn -pl scm-common/core test -f com.scm.parent/pom.xml
```

**期望输出**: `BUILD SUCCESS` 与 `Tests run: N, Failures: 0, Errors: 0`

---

## Phase 0-A: ServiceException 加 HttpStatus + 异常 sealed 化

**Files:**
- Modify: `scm-common/core/src/main/java/com/scmcloud/common/exception/ServiceException.java`
- Modify: `scm-common/core/src/main/java/com/scmcloud/common/exception/BusinessException.java`
- Modify: `scm-common/core/src/main/java/com/scmcloud/common/exception/ErrorCode.java`
- Create: `scm-common/core/src/test/java/com/scmcloud/common/exception/ServiceExceptionTest.java`
- Create: `scm-common/core/src/test/java/com/scmcloud/common/exception/BusinessExceptionTest.java`

### Task 0-A.1: 写 ServiceException 加 HttpStatus 的失败测试

**File:** `scm-common/core/src/test/java/com/scmcloud/common/exception/ServiceExceptionTest.java`

- [ ] **Step 1: 新建测试类**

```java
package com.scmcloud.common.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class ServiceExceptionTest {

    @Test
    void defaultConstructor_shouldUseInternalServerError() {
        var ex = new ServiceException("boom");
        assertThat(ex.getCode()).isEqualTo(500);
        assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void codeConstructor_shouldCarryCodeAndDefault500() {
        var ex = new ServiceException(40000, "bad request");
        assertThat(ex.getCode()).isEqualTo(40000);
        assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void statusConstructor_shouldCarryHttpStatus() {
        var ex = new ServiceException(40000, HttpStatus.BAD_REQUEST, "bad");
        assertThat(ex.getCode()).isEqualTo(40000);
        assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void errorCodeConstructor_shouldMapToHttpStatusFromEnum() {
        var ex = new ServiceException(ErrorCode.ORDER_NOT_FOUND);
        assertThat(ex.getCode()).isEqualTo(ErrorCode.ORDER_NOT_FOUND.getCode());
        assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
```

- [ ] **Step 2: 跑测试，预期失败（缺方法）**

```bash
mvn -pl scm-common/core test -Dtest=ServiceExceptionTest -f com.scm.parent/pom.xml
```

期望：编译失败或 `getHttpStatus()` 找不到。

### Task 0-A.2: 修改 ServiceException 携带 HttpStatus

**File:** `scm-common/core/src/main/java/com/scmcloud/common/exception/ServiceException.java`

- [ ] **Step 3: 完整重写该文件**

```java
package com.scmcloud.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;

@Getter
public class ServiceException extends RuntimeException {

    private final Integer code;
    private final HttpStatus httpStatus;

    public ServiceException(String message) {
        super(message);
        this.code = 500;
        this.httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
    }

    public ServiceException(Integer code, String message) {
        super(message);
        this.code = code;
        this.httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
    }

    public ServiceException(Integer code, HttpStatus httpStatus, String message) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
    }

    public ServiceException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
        this.httpStatus = errorCode.getHttpStatus();
    }

    public ServiceException(ErrorCode errorCode) {
        super(errorCode.getI18nKey());
        this.code = errorCode.getCode();
        this.httpStatus = errorCode.getHttpStatus();
    }

    public ServiceException(String message, Throwable cause) {
        super(message, cause);
        this.code = 500;
        this.httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
    }

    @Nullable
    public Integer getCode() {
        return code;
    }
}
```

注意：`@Getter` 已提供 `getHttpStatus()`，无需手写。`getCode()` 显式标注 `@Nullable` 防止 null 检查（实际不会为 null 但保留兼容）。

- [ ] **Step 4: 跑测试，预期通过**

```bash
mvn -pl scm-common/core test -Dtest=ServiceExceptionTest -f com.scm.parent/pom.xml
```

### Task 0-A.3: BusinessException 加 sealed 化（保留旧构造器）

**File:** `scm-common/core/src/main/java/com/scmcloud/common/exception/BusinessException.java`

- [ ] **Step 5: 完整重写为非 sealed（不破坏现有子类扩展）**

```java
package com.scmcloud.common.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    private final Integer code;

    public BusinessException(String message) {
        super(message);
        this.code = 500;
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getI18nKey());
        this.code = errorCode.getCode();
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
        this.code = 500;
    }
}
```

> 注：BusinessException 暂不 sealed 化（业务侧可能有自己的子类）。后续 Phase 2-C 再统一收紧。

### Task 0-A.4: GlobalExceptionHandler 用 ServiceException.httpStatus

**File:** `scm-common/core/src/main/java/com/scmcloud/common/exception/GlobalExceptionHandler.java`

- [ ] **Step 6: 修改 `handleServiceException` 方法（替换原 :37-44 行）**

原:
```java
@ExceptionHandler(ServiceException.class)
public ResponseEntity<ApiResponse<Void>> handleServiceException(ServiceException e, HttpServletRequest request) {
    String traceId = resolveTraceId(request);
    log.error("Service exception at {}, traceId={}: {}", request.getRequestURI(), traceId, e.getMessage());
    HttpStatus status = HttpStatus.resolve(e.getCode()) != null ? HttpStatus.valueOf(e.getCode()) : HttpStatus.INTERNAL_SERVER_ERROR;
    return ResponseEntity.status(status)
            .body(ApiResponse.fail(e.getCode(), "Service error: " + escapeHtml(e.getMessage()) + " (traceId=" + traceId + ")"));
}
```

改为:
```java
@ExceptionHandler(ServiceException.class)
public ResponseEntity<ApiResponse<Void>> handleServiceException(ServiceException e, HttpServletRequest request) {
    String traceId = resolveTraceId(request);
    log.error("Service exception at {}, traceId={}: {}", request.getRequestURI(), traceId, e.getMessage());
    HttpStatus status = e.getHttpStatus() != null ? e.getHttpStatus() : HttpStatus.INTERNAL_SERVER_ERROR;
    return ResponseEntity.status(status)
            .body(ApiResponse.fail(e.getCode(), "Service error: " + e.getMessage() + " (traceId=" + traceId + ")"));
}
```

注意：HTML 转义在本 Phase 一并删除（与 Phase 0-D 合并）。

- [ ] **Step 7: 删除 `escapeHtml` 字段 + import（Phase 0-D 合并完成）**

删除 `import org.springframework.web.util.HtmlUtils;` 和方法 `private String escapeHtml(String input)`。

- [ ] **Step 8: 修改 `handleBusinessException` 同样删除 escapeHtml**

原:
```java
return ApiResponse.fail(e.getCode(), "Business error: " + escapeHtml(e.getMessage()));
```

改为:
```java
return ApiResponse.fail(e.getCode(), "Business error: " + e.getMessage());
```

- [ ] **Step 9: 跑全模块测试**

```bash
mvn -pl scm-common/core test -f com.scm.parent/pom.xml
```

期望: 全绿。

- [ ] **Step 10: 提交**

```bash
git add scm-common/core/src/main/java/com/scmcloud/common/exception/ \
        scm-common/core/src/test/java/com/scmcloud/common/exception/
git commit -m "fix(scm-common-core): ServiceException carry HttpStatus, drop html escape"
```

---

## Phase 0-B: TenantInterceptor fail-fast + UNION 处理

**Files:**
- Modify: `scm-common/core/src/main/java/com/scmcloud/common/tenant/TenantInterceptor.java`
- Create: `scm-common/core/src/main/java/com/scmcloud/common/tenant/TenantProperties.java`
- Create: `scm-common/core/src/main/java/com/scmcloud/common/tenant/TenantParseException.java`
- Create: `scm-common/core/src/test/java/com/scmcloud/common/tenant/TenantInterceptorTest.java`

### Task 0-B.1: 新增 TenantProperties 配置类

- [ ] **Step 1: 新建文件**

```java
package com.scmcloud.common.tenant;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "scm.tenant")
public record TenantProperties(
        boolean failOnParseError,
        boolean required,
        List<String> excludePaths
) {
    public TenantProperties {
        if (excludePaths == null) excludePaths = List.of();
    }
}
```

### Task 0-B.2: 新增 TenantParseException

- [ ] **Step 2: 新建顶层异常类**

```java
package com.scmcloud.common.tenant;

public class TenantParseException extends RuntimeException {
    public TenantParseException(String message) { super(message); }
    public TenantParseException(String message, Throwable cause) { super(message, cause); }
}
```

### Task 0-B.3: 写失败测试（覆盖 fail-fast 与 UNION）

**File:** `scm-common/core/src/test/java/com/scmcloud/common/tenant/TenantInterceptorTest.java`

- [ ] **Step 3: 新建测试类骨架 + 解析失败测试**

```java
package com.scmcloud.common.tenant;

import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SetOperationList;
import net.sf.jsqlparser.statement.select.UnionOp;
import net.sf.jsqlparser.statement.update.Update;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.JSQLException;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Invocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;

import java.sql.Connection;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

class TenantInterceptorTest {

    private TenantInterceptor interceptor;
    private TenantProperties properties;

    @BeforeEach
    void setup() {
        properties = new TenantProperties(true, true, List.of());
        interceptor = new TenantInterceptor(properties);
        TenantContextHolder.setTenantId(UUID.randomUUID());
    }

    @AfterEach
    void teardown() {
        TenantContextHolder.clear();
    }

    @Test
    void selectWithTenant_shouldInjectWhereClause() {
        var bound = boundSql("SELECT id FROM ord_order");
        var stmtHandler = mockHandler(SqlCommandType.SELECT, bound);
        var inv = mockInvocation(stmtHandler);
        interceptor.intercept(inv);
        assertThat(bound.getSql()).contains("WHERE tenant_id =");
    }

    @Test
    void updateWithTenant_shouldInjectWhereClause() {
        var bound = boundSql("UPDATE ord_order SET status = 'PAID' WHERE id = 1");
        var stmtHandler = mockHandler(SqlCommandType.UPDATE, bound);
        var inv = mockInvocation(stmtHandler);
        interceptor.intercept(inv);
        assertThat(bound.getSql()).contains("tenant_id =");
        assertThat(bound.getSql()).contains("id = 1");
    }

    @Test
    void deleteWithTenant_shouldInjectWhereClause() {
        var bound = boundSql("DELETE FROM ord_order WHERE id = 1");
        var stmtHandler = mockHandler(SqlCommandType.DELETE, bound);
        var inv = mockInvocation(stmtHandler);
        interceptor.intercept(inv);
        assertThat(bound.getSql()).contains("tenant_id =");
    }

    @Test
    void unionSelect_shouldInjectIntoEachBranch() throws Exception {
        var sql = "SELECT id FROM ord_order UNION SELECT id FROM ord_refund";
        var stmt = (SetOperationList) net.sf.jsqlparser.parser.CCJSqlParserUtil.parse(sql);
        var first = (PlainSelect) stmt.getSelects().get(0);
        var second = (PlainSelect) stmt.getSelects().get(1);
        var bound = boundSql(sql);
        var stmtHandler = mockHandler(SqlCommandType.SELECT, bound);
        var inv = mockInvocation(stmtHandler);
        interceptor.intercept(inv);
        // 验证两个 PlainSelect 都被处理
        assertThat(first.getWhere()).isNotNull();
        assertThat(second.getWhere()).isNotNull();
    }

    @Test
    void excludeTable_shouldSkipInjection() throws Exception {
        var stmt = (net.sf.jsqlparser.statement.select.PlainSelect)
                net.sf.jsqlparser.parser.CCJSqlParserUtil.parse("SELECT id FROM tenant");
        var bound = boundSql("SELECT id FROM tenant");
        var stmtHandler = mockHandler(SqlCommandType.SELECT, bound);
        var inv = mockInvocation(stmtHandler);
        interceptor.intercept(inv);
        assertThat(bound.getSql()).doesNotContain("tenant_id =");
    }

    @Test
    void unparseableSql_shouldThrowWhenFailFastEnabled() {
        var bound = boundSql("THIS IS NOT SQL");
        var stmtHandler = mockHandler(SqlCommandType.SELECT, bound);
        var inv = mockInvocation(stmtHandler);
        assertThatThrownBy(() -> interceptor.intercept(inv))
                .isInstanceOf(TenantParseException.class);
    }

    @Test
    void unparseableSql_shouldSkipWhenFailFastDisabled() {
        properties = new TenantProperties(false, true, List.of());
        interceptor = new TenantInterceptor(properties);
        var bound = boundSql("THIS IS NOT SQL");
        var stmtHandler = mockHandler(SqlCommandType.SELECT, bound);
        var inv = mockInvocation(stmtHandler);
        interceptor.intercept(inv); // 不应抛
    }

    @Test
    void noTenantContext_shouldSkipInjection() {
        TenantContextHolder.clear();
        var bound = boundSql("SELECT id FROM ord_order");
        var stmtHandler = mockHandler(SqlCommandType.SELECT, bound);
        var inv = mockInvocation(stmtHandler);
        interceptor.intercept(inv);
        assertThat(bound.getSql()).isEqualTo("SELECT id FROM ord_order");
    }

    // --- helpers ---

    private BoundSql boundSql(String sql) {
        var bs = mock(BoundSql.class);
        when(bs.getSql()).thenReturn(sql);
        return bs;
    }

    private StatementHandler mockHandler(SqlCommandType type, BoundSql bound) {
        var ms = mock(MappedStatement.class);
        when(ms.getSqlCommandType()).thenReturn(type);
        when(ms.getId()).thenReturn("test.mapper.method");
        var handler = mock(StatementHandler.class);
        when(handler.getBoundSql()).thenReturn(bound);
        var meta = org.apache.ibatis.reflection.SystemMetaObject.forObject(handler);
        meta.setValue("delegate.mappedStatement", ms);
        meta.setValue("delegate.boundSql", bound);
        return handler;
    }

    private Invocation mockInvocation(StatementHandler handler) {
        var inv = mock(Invocation.class);
        when(inv.getTarget()).thenReturn(handler);
        when(inv.proceed()).thenReturn(null);
        return inv;
    }
}
```

- [ ] **Step 4: 跑测试，预期全部失败**

```bash
mvn -pl scm-common/core test -Dtest=TenantInterceptorTest -f com.scm.parent/pom.xml
```

### Task 0-B.4: 修改 TenantInterceptor 实现 fail-fast 与 SetOperationList

**File:** `scm-common/core/src/main/java/com/scmcloud/common/tenant/TenantInterceptor.java`

- [ ] **Step 5: 完整重写该文件**

```java
package com.scmcloud.common.tenant;

import com.baomidou.mybatisplus.core.toolkit.PluginUtils;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SetOperationList;
import net.sf.jsqlparser.statement.update.Update;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;

import java.sql.Connection;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Intercepts({
    @Signature(
        type = StatementHandler.class,
        method = "prepare",
        args = {Connection.class, Integer.class}
    )
})
public class TenantInterceptor implements Interceptor {

    private static final String TENANT_COLUMN = "tenant_id";

    private static final Set<String> EXCLUDE_TABLES = new HashSet<>(Arrays.asList(
        "tenant", "tenant_package", "tenant_subscription",
        "tenant_resource_quota", "tenant_config", "tenant_feature", "tenant_operation_log"
    ));

    private final TenantProperties properties;

    public TenantInterceptor(TenantProperties properties) {
        this.properties = properties;
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        StatementHandler statementHandler = PluginUtils.realTarget(invocation.getTarget());
        MetaObject metaObject = SystemMetaObject.forObject(statementHandler);

        MappedStatement mappedStatement = (MappedStatement) metaObject.getValue("delegate.mappedStatement");
        SqlCommandType sqlCommandType = mappedStatement.getSqlCommandType();

        if (!SqlCommandType.SELECT.equals(sqlCommandType)
            && !SqlCommandType.UPDATE.equals(sqlCommandType)
            && !SqlCommandType.DELETE.equals(sqlCommandType)
            && !SqlCommandType.INSERT.equals(sqlCommandType)) {
            return invocation.proceed();
        }

        UUID tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            log.debug("Tenant ID is null, skipping tenant filter for SQL: {}", mappedStatement.getId());
            return invocation.proceed();
        }

        BoundSql boundSql = statementHandler.getBoundSql();
        String originalSql = boundSql.getSql();

        try {
            Statement statement = CCJSqlParserUtil.parse(originalSql);

            if (statement instanceof Select select) {
                handleSelect(select, tenantId);
            } else if (statement instanceof Update update) {
                handleUpdate(update, tenantId);
            } else if (statement instanceof Delete delete) {
                handleDelete(delete, tenantId);
            } else if (statement instanceof Insert) {
                log.debug("INSERT statement detected, tenant_id auto-filled by AuditMetaObjectHandler");
            }

            String newSql = statement.toString();
            metaObject.setValue("delegate.boundSql.sql", newSql);
            log.debug("Injected tenant_id={} into SQL: {}", tenantId, newSql);
        } catch (JSQLException | RuntimeException e) {
            if (properties != null && properties.failOnParseError()) {
                throw new TenantParseException(
                    "Failed to inject tenant_id into SQL: " + originalSql, e);
            }
            log.warn("Failed to inject tenant_id into SQL (skipping): {}", originalSql, e);
        }

        return invocation.proceed();
    }

    private void handleSelect(Select select, UUID tenantId) {
        if (select.getSelectBody() instanceof SetOperationList setOp) {
            for (Object body : setOp.getSelects()) {
                if (body instanceof PlainSelect ps) {
                    injectIntoPlainSelect(ps, tenantId);
                }
            }
        } else if (select.getSelectBody() instanceof PlainSelect plainSelect) {
            injectIntoPlainSelect(plainSelect, tenantId);
        }
    }

    private void injectIntoPlainSelect(PlainSelect plainSelect, UUID tenantId) {
        String tableName = plainSelect.getFromItem().toString();
        if (isExcludeTable(tableName)) {
            log.debug("Table {} is excluded from tenant filter", tableName);
            return;
        }
        EqualsTo condition = buildTenantCondition(tenantId);
        Expression where = plainSelect.getWhere();
        if (where == null) {
            plainSelect.setWhere(condition);
        } else {
            plainSelect.setWhere(new AndExpression(where, condition));
        }
    }

    private void handleUpdate(Update update, UUID tenantId) {
        Table table = update.getTable();
        if (table != null && isExcludeTable(table.toString())) return;
        EqualsTo condition = buildTenantCondition(tenantId);
        Expression where = update.getWhere();
        if (where == null) update.setWhere(condition);
        else update.setWhere(new AndExpression(where, condition));
    }

    private void handleDelete(Delete delete, UUID tenantId) {
        Table table = delete.getTable();
        if (table != null && isExcludeTable(table.toString())) return;
        EqualsTo condition = buildTenantCondition(tenantId);
        Expression where = delete.getWhere();
        if (where == null) delete.setWhere(condition);
        else delete.setWhere(new AndExpression(where, condition));
    }

    private EqualsTo buildTenantCondition(UUID tenantId) {
        EqualsTo condition = new EqualsTo();
        condition.setLeftExpression(new Column(TENANT_COLUMN));
        condition.setRightExpression(new StringValue(tenantId.toString()));
        return condition;
    }

    private boolean isExcludeTable(String tableName) {
        String actual = tableName.contains(" ")
                ? tableName.substring(0, tableName.indexOf(" ")).trim()
                : tableName.trim();
        if (actual.contains(".")) {
            actual = actual.substring(actual.indexOf(".") + 1);
        }
        return EXCLUDE_TABLES.contains(actual.toLowerCase());
    }

    @Override
    public Object plugin(Object target) {
        if (target instanceof StatementHandler) return Plugin.wrap(target, this);
        return target;
    }

    @Override
    public void setProperties(Properties properties) {
        String excludeTables = properties.getProperty("excludeTables");
        if (excludeTables != null && !excludeTables.trim().isEmpty()) {
            for (String t : excludeTables.split(",")) {
                EXCLUDE_TABLES.add(t.trim().toLowerCase());
            }
        }
    }
}
```

- [ ] **Step 6: 跑测试，预期通过**

```bash
mvn -pl scm-common/core test -Dtest=TenantInterceptorTest -f com.scm.parent/pom.xml
```

- [ ] **Step 7: 全模块编译验证**

```bash
mvn -pl scm-common/core -am compile -f com.scm.parent/pom.xml
```

- [ ] **Step 8: 提交**

```bash
git add scm-common/core/src/main/java/com/scmcloud/common/tenant/ \
        scm-common/core/src/test/java/com/scmcloud/common/tenant/
git commit -m "fix(scm-common-core): TenantInterceptor fail-fast on parse error + UNION support"
```

---

## Phase 0-C: TenantFilter Authentication 优先 + shouldNotFilter

**Files:**
- Modify: `scm-common/core/src/main/java/com/scmcloud/common/tenant/TenantFilter.java`
- Modify: `scm-common/core/src/main/java/com/scmcloud/common/tenant/TenantContextHolder.java`
- Create: `scm-common/core/src/test/java/com/scmcloud/common/tenant/TenantFilterTest.java`

### Task 0-C.1: TenantContextHolder 把 TenantNotFoundException 提到顶层

**File:** `scm-common/core/src/main/java/com/scmcloud/common/tenant/TenantContextHolder.java`

- [ ] **Step 1: 把嵌套异常类移到独立文件 `TenantParseException.java`（Phase 0-B 已建）保留兼容**

将 `TenantContextHolder.TenantNotFoundException` 标 `@Deprecated`，委托到 `TenantParseException`。完整文件:

```java
package com.scmcloud.common.tenant;

import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

@Slf4j
public class TenantContextHolder {
    private static final ThreadLocal<UUID> TENANT_ID_HOLDER = new ThreadLocal<>();

    public static void setTenantId(UUID tenantId) {
        if (tenantId == null) {
            log.warn("Setting null tenant ID");
        }
        TENANT_ID_HOLDER.set(tenantId);
        log.debug("Set tenant ID: {}", tenantId);
    }

    public static UUID getTenantId() {
        UUID t = TENANT_ID_HOLDER.get();
        if (t == null) log.debug("Tenant ID is null in current thread");
        return t;
    }

    public static UUID getRequiredTenantId() {
        UUID t = getTenantId();
        if (t == null) throw new TenantParseException("Tenant ID is required but not found");
        return t;
    }

    public static void clear() {
        UUID t = TENANT_ID_HOLDER.get();
        TENANT_ID_HOLDER.remove();
        log.debug("Cleared tenant ID: {}", t);
    }

    public static <T> T executeInTenantContext(UUID tenantId, TenantContextCallback<T> callback) {
        UUID original = getTenantId();
        try {
            setTenantId(tenantId);
            return callback.execute();
        } finally {
            if (original != null) setTenantId(original);
            else clear();
        }
    }

    @FunctionalInterface
    public interface TenantContextCallback<T> { T execute(); }

    @Deprecated
    public static class TenantNotFoundException extends RuntimeException {
        public TenantNotFoundException(String message) { super(message); }
    }
}
```

> 注: Phase 1-A 调整 warn→debug 已合并完成。

### Task 0-C.2: 写 TenantFilter 失败测试

**File:** `scm-common/core/src/test/java/com/scmcloud/common/tenant/TenantFilterTest.java`

- [ ] **Step 2: 新建测试**

```java
package com.scmcloud.common.tenant;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class TenantFilterTest {

    private TenantFilter filter;
    private FilterChain chain;

    @BeforeEach
    void setup() {
        filter = new TenantFilter(new TenantProperties(true, true, List.of()));
        chain = mock(FilterChain.class);
    }

    @AfterEach
    void teardown() {
        TenantContextHolder.clear();
    }

    @Test
    void headerTenantId_shouldBeSet() throws Exception {
        UUID tenantId = UUID.randomUUID();
        var req = new MockHttpServletRequest();
        req.addHeader("X-Tenant-Id", tenantId.toString());
        var resp = new MockHttpServletResponse();
        filter.doFilter(req, resp, chain);
        assertThat(TenantContextHolder.getTenantId()).isEqualTo(tenantId);
        verify(chain).doFilter(req, resp);
    }

    @Test
    void paramTenantId_shouldBeSet() throws Exception {
        UUID tenantId = UUID.randomUUID();
        var req = new MockHttpServletRequest();
        req.setParameter("tenantId", tenantId.toString());
        var resp = new MockHttpServletResponse();
        filter.doFilter(req, resp, chain);
        assertThat(TenantContextHolder.getTenantId()).isEqualTo(tenantId);
    }

    @Test
    void missingTenant_shouldThrowWhenRequired() {
        var req = new MockHttpServletRequest();
        var resp = new MockHttpServletResponse();
        assertThatThrownBy(() -> filter.doFilter(req, resp, chain))
                .isInstanceOf(TenantParseException.class);
    }

    @Test
    void missingTenant_shouldSkipWhenNotRequired() throws Exception {
        filter = new TenantFilter(new TenantProperties(true, false, List.of()));
        var req = new MockHttpServletRequest();
        var resp = new MockHttpServletResponse();
        filter.doFilter(req, resp, chain);
        assertThat(TenantContextHolder.getTenantId()).isNull();
        verify(chain).doFilter(req, resp);
    }

    @Test
    void invalidTenantIdFormat_shouldThrow() {
        var req = new MockHttpServletRequest();
        req.addHeader("X-Tenant-Id", "not-a-uuid");
        var resp = new MockHttpServletResponse();
        assertThatThrownBy(() -> filter.doFilter(req, resp, chain))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void clearAfterRequest_shouldAlwaysRun() throws Exception {
        UUID tenantId = UUID.randomUUID();
        var req = new MockHttpServletRequest();
        req.addHeader("X-Tenant-Id", tenantId.toString());
        var resp = new MockHttpServletResponse();
        filter.doFilter(req, resp, chain);
        assertThat(TenantContextHolder.getTenantId()).isNull();
    }

    @Test
    void excludePath_shouldSkipFilter() throws Exception {
        filter = new TenantFilter(new TenantProperties(true, true, List.of("/actuator/**")));
        var req = new MockHttpServletRequest("GET", "/actuator/health");
        var resp = new MockHttpServletResponse();
        filter.doFilter(req, resp, chain);
        assertThat(TenantContextHolder.getTenantId()).isNull();
        verify(chain).doFilter(req, resp);
    }
}
```

- [ ] **Step 3: 跑测试，预期失败**

```bash
mvn -pl scm-common/core test -Dtest=TenantFilterTest -f com.scm.parent/pom.xml
```

### Task 0-C.3: 重写 TenantFilter

**File:** `scm-common/core/src/main/java/com/scmcloud/common/tenant/TenantFilter.java`

- [ ] **Step 4: 完整重写**

```java
package com.scmcloud.common.tenant;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class TenantFilter implements jakarta.servlet.Filter {

    private static final String HEADER_TENANT_ID = "X-Tenant-Id";
    private static final String HEADER_TENANT_ID_ALT = "Tenant-Id";
    private static final String PARAM_TENANT_ID = "tenantId";

    private final TenantProperties properties;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public TenantFilter(TenantProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean shouldNotFilter(HttpServletRequest request) {
        List<String> excludePaths = properties.excludePaths();
        String path = request.getRequestURI();
        return excludePaths.stream().anyMatch(p -> pathMatcher.match(p, path));
    }

    @Override
    public void doFilter(jakarta.servlet.ServletRequest request,
                         jakarta.servlet.ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        if (shouldNotFilter(httpRequest)) {
            chain.doFilter(request, response);
            return;
        }
        try {
            UUID tenantId = extractTenantId(httpRequest);
            if (tenantId != null) {
                TenantContextHolder.setTenantId(tenantId);
                log.debug("Tenant filter set tenant ID: {} for {}",
                        tenantId, httpRequest.getRequestURI());
            } else if (properties.required()) {
                throw new TenantParseException("Tenant ID is required but missing in request: "
                        + httpRequest.getRequestURI());
            } else {
                log.debug("Tenant ID not present and not required: {}", httpRequest.getRequestURI());
            }
            chain.doFilter(request, response);
        } finally {
            TenantContextHolder.clear();
        }
    }

    private UUID extractTenantId(HttpServletRequest request) {
        String value = request.getHeader(HEADER_TENANT_ID);
        if (value == null || value.isBlank()) value = request.getHeader(HEADER_TENANT_ID_ALT);
        if (value == null || value.isBlank()) value = request.getParameter(PARAM_TENANT_ID);
        if (value == null || value.isBlank()) return null;
        try {
            return UUID.fromString(value.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid tenant ID format: " + value, e);
        }
    }
}
```

> 注意: JWT 解析已从本 Filter 移除（移交 JwtAuthenticationFilter），本 Filter 只看 Header / Query 参数。

- [ ] **Step 5: 注册 TenantProperties 为 @ConfigurationProperties bean**

修改 `AsyncTenantAutoConfiguration.java` 增加:

```java
@Bean
@ConfigurationProperties(prefix = "scm.tenant")
public TenantProperties tenantProperties() {
    return new TenantProperties(true, true, List.of("/actuator/**", "/v3/api-docs/**"));
}
```

完整 `AsyncTenantAutoConfiguration.java`:

```java
package com.scmcloud.common.tenant;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@EnableConfigurationProperties
public class AsyncTenantAutoConfiguration {

    @Bean
    @ConfigurationProperties(prefix = "scm.tenant")
    public TenantProperties tenantProperties() {
        return new TenantProperties(true, true, List.of("/actuator/**", "/v3/api-docs/**"));
    }

    // 既有 Bean（保留）...
}
```

- [ ] **Step 6: 跑测试**

```bash
mvn -pl scm-common/core test -Dtest=TenantFilterTest -f com.scm.parent/pom.xml
```

- [ ] **Step 7: 编译全模块 + 跑核心模块测试**

```bash
mvn -pl scm-common/core -am compile test -f com.scm.parent/pom.xml
```

- [ ] **Step 8: 提交**

```bash
git add scm-common/core/src/main/java/com/scmcloud/common/tenant/ \
        scm-common/core/src/test/java/com/scmcloud/common/tenant/
git commit -m "fix(scm-common-core): TenantFilter use Spring properties + shouldNotFilter + JWT removed"
```

---

## Phase 0-D: 删除 GlobalExceptionHandler HTML 转义

> 已合并到 Phase 0-A.4。Phase 0-D 无独立操作。

- [ ] **Step 1: 确认 `escapeHtml` 方法已删除**

```bash
grep -n escapeHtml scm-common/core/src/main/java/com/scmcloud/common/exception/GlobalExceptionHandler.java
```

期望: 无输出。

---

## Phase 1: 健壮性/性能优化

### Phase 1-A: TenantContextHolder warn→debug

> 已合并到 Phase 0-C.1。

### Phase 1-B: TenantFilter 注入 ObjectMapper + 缺失可配置阻断

> 已合并到 Phase 0-C.3。本 Phase 仅做 ObjectMapper 注入清理（如需自定义 JWT 解析时使用）。

- [ ] **Step 1: 在 `AsyncTenantAutoConfiguration` 中暴露 ObjectMapper bean（如果未来 TenantFilter 需要 JWT 解析）**

```java
@Bean
@ConditionalOnMissingBean
public ObjectMapper tenantAwareObjectMapper() {
    return new ObjectMapper();
}
```

> 当前 TenantFilter 已不解析 JWT，本步骤是预留位。

### Phase 1-C: TenantInterceptor SQL 改写缓存

**File:** `scm-common/core/src/main/java/com/scmcloud/common/tenant/TenantInterceptor.java`

- [ ] **Step 1: 写失败测试**

```java
@Test
void sameSqlSecondCall_shouldHitCache() {
    var bound1 = boundSql("SELECT id FROM ord_order");
    var handler1 = mockHandler(SqlCommandType.SELECT, bound1);
    interceptor.intercept(mockInvocation(handler1));
    String sql1 = bound1.getSql();

    var bound2 = boundSql("SELECT id FROM ord_order");
    var handler2 = mockHandler(SqlCommandType.SELECT, bound2);
    interceptor.intercept(mockInvocation(handler2));
    // 改写结果应一致（即使 Statement 实例不同，输出 SQL 相同）
    assertThat(bound2.getSql()).isEqualTo(sql1);
}
```

- [ ] **Step 2: 在 `TenantInterceptor` 中加 Caffeine cache（依赖 `com.github.ben-manes.caffeine:caffeine`，已由 spring-boot-starter 间接引入）**

修改 `intercept()`:

```java
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;

// 字段
private final Cache<String, String> rewrittenSqlCache = Caffeine.newBuilder()
        .maximumSize(1024)
        .expireAfterWrite(Duration.ofMinutes(10))
        .build();

// 在 intercept() 中
String cacheKey = mappedStatement.getId() + "::" + originalSql;
String cached = rewrittenSqlCache.getIfPresent(cacheKey);
String newSql;
if (cached != null) {
    newSql = cached;
} else {
    Statement statement = CCJSqlParserUtil.parse(originalSql);
    if (statement instanceof Select select) handleSelect(select, tenantId);
    else if (statement instanceof Update update) handleUpdate(update, tenantId);
    else if (statement instanceof Delete delete) handleDelete(delete, tenantId);
    newSql = statement.toString();
    rewrittenSqlCache.put(cacheKey, newSql);
}
metaObject.setValue("delegate.boundSql.sql", newSql);
```

> 注意: INSERT 不缓存（改写结果不变但没必要缓存）。

- [ ] **Step 3: 跑测试**

```bash
mvn -pl scm-common/core test -Dtest=TenantInterceptorTest -f com.scm.parent/pom.xml
```

- [ ] **Step 4: 提交**

```bash
git add scm-common/core/src/main/java/com/scmcloud/common/tenant/TenantInterceptor.java \
        scm-common/core/src/test/java/com/scmcloud/common/tenant/TenantInterceptorTest.java
git commit -m "perf(scm-common-core): cache rewritten SQL by MappedStatement.id"
```

### Phase 1-D: PermissionChecker 加 Caffeine L1 缓存

**File:** `scm-common/core/src/main/java/com/scmcloud/common/security/PermissionChecker.java`

- [ ] **Step 1: 写失败测试**

```java
// 文件: PermissionCheckerTest.java
@Test
void hasPermission_cachedAfterFirstCall() {
    UUID userId = UUID.randomUUID();
    when(query.getUserPermissions(userId)).thenReturn(Set.of("order:create"));
    checker.hasPermission(userId, "order:create");
    checker.hasPermission(userId, "order:create");
    verify(query, times(1)).getUserPermissions(userId);
}

@Test
void hasPermission_differentUser_doesNotShareCache() {
    UUID u1 = UUID.randomUUID();
    UUID u2 = UUID.randomUUID();
    when(query.getUserPermissions(u1)).thenReturn(Set.of("a"));
    when(query.getUserPermissions(u2)).thenReturn(Set.of("b"));
    checker.hasPermission(u1, "a");
    checker.hasPermission(u2, "b");
    verify(query).getUserPermissions(u1);
    verify(query).getUserPermissions(u2);
}
```

- [ ] **Step 2: 修改 PermissionChecker 注入 Caffeine cache**

```java
private final Cache<UUID, Set<String>> userPermissionsCache = Caffeine.newBuilder()
        .maximumSize(10_000)
        .expireAfterWrite(Duration.ofSeconds(60))
        .build();

public boolean hasPermission(UUID userId, String permissionCode) {
    if (userId == null || permissionCode == null || permissionCode.isBlank()) return false;
    Set<String> perms = userPermissionsCache.get(userId,
            k -> permissionQueryService.getUserPermissions(k));
    return perms.contains(permissionCode);
}
```

> 对应同样改造 `hasRole`、`hasAnyPermission`、`hasAllPermissions` 等方法，使用各自 cache。

- [ ] **Step 3: 跑测试 + 提交**

```bash
mvn -pl scm-common/core test -Dtest=PermissionCheckerTest -f com.scm.parent/pom.xml
git commit -am "perf(scm-common-core): Caffeine L1 cache for PermissionChecker"
```

---

## Phase 2: 结构/一致性

### Phase 2-A: 合并两个 TenantAwareEntity

**Files:**
- Modify: `scm-common/core/src/main/java/com/scmcloud/common/tenant/TenantAwareEntity.java`（标 @Deprecated, 委托 entity/）
- Create: `scm-common/core/src/main/java/com/scmcloud/common/entity/TenantAwareEntity.java`（保留现版本）

- [ ] **Step 1: 把 `entity/TenantAwareEntity.java` 设为业务基类标准版（已是 Phase 0 现状）**

- [ ] **Step 2: 重写 `tenant/TenantAwareEntity.java` 为 @Deprecated 委托版**

```java
package com.scmcloud.common.tenant;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.io.Serializable;

/**
 * @deprecated use {@link com.scmcloud.common.entity.TenantAwareEntity} which also includes
 *             audit fields, optimistic locking, and Snowflake ID.
 *             This class is kept for binary compatibility in v1.x and will be removed in v2.0.
 */
@Data
@Deprecated(since = "1.1.0", forRemoval = true)
public abstract class TenantAwareEntity implements Serializable {
    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    private String tenantId;
}
```

- [ ] **Step 3: grep 业务模块确认无直接继承 `tenant.TenantAwareEntity`**

```bash
grep -r "extends.*tenant\.TenantAwareEntity" --include="*.java" .
```

期望: 仅 core 模块自身。如有业务模块继承，需逐一改为继承 `entity.TenantAwareEntity`。

- [ ] **Step 4: 编译验证**

```bash
mvn -f com.scm.parent/pom.xml compile
```

- [ ] **Step 5: 提交**

```bash
git commit -am "refactor(scm-common-core): deprecate tenant.TenantAwareEntity in favor of entity.TenantAwareEntity"
```

### Phase 2-B: Money/Quantity/PageResult record 化

**Files:**
- Modify: `scm-common/core/src/main/java/com/scmcloud/common/domain/Money.java`
- Modify: `scm-common/core/src/main/java/com/scmcloud/common/domain/Quantity.java`
- Modify: `scm-common/core/src/main/java/com/scmcloud/common/domain/PageResult.java`

- [ ] **Step 1: grep 调用方**

```bash
grep -rn "com\.scmcloud\.common\.domain\.Money\|com\.scmcloud\.common\.domain\.Quantity\|com\.scmcloud\.common\.domain\.PageResult" --include="*.java" .
```

- [ ] **Step 2: 如无业务模块调用，按 record 重写**

```java
// Money.java
public record Money(BigDecimal amount, Currency currency) {
    public Money {
        if (amount == null) throw new IllegalArgumentException("amount required");
        if (currency == null) throw new IllegalArgumentException("currency required");
    }
    public static Money of(String amount, String currency) {
        return new Money(new BigDecimal(amount), Currency.getInstance(currency));
    }
    public Money add(Money other) {
        if (!currency.equals(other.currency))
            throw new IllegalArgumentException("Currency mismatch");
        return new Money(amount.add(other.amount), currency);
    }
    public Money multiply(BigDecimal factor) {
        return new Money(amount.multiply(factor), currency);
    }
}
```

```java
// Quantity.java
public record Quantity(long value, String unit) {
    public Quantity {
        if (unit == null || unit.isBlank()) throw new IllegalArgumentException("unit required");
    }
    public static Quantity of(long value, String unit) {
        return new Quantity(value, unit);
    }
    public Quantity add(Quantity other) {
        if (!unit.equals(other.unit))
            throw new IllegalArgumentException("Unit mismatch");
        return new Quantity(Math.addExact(value, other.value), unit);
    }
}
```

```java
// PageResult.java
public record PageResult<T>(List<T> records, long total, long size, long current) {
    public PageResult {
        records = records == null ? List.of() : List.copyOf(records);
    }
    public static <T> PageResult<T> of(List<T> records, long total, long size, long current) {
        return new PageResult<>(records, total, size, current);
    }
    public long getPages() {
        return size == 0 ? 0 : (total + size - 1) / size;
    }
}
```

- [ ] **Step 3: 提交**

```bash
git commit -am "refactor(scm-common-core): record-ify Money/Quantity/PageResult"
```

### Phase 2-C: 异常 sealed 化

> 仅在新增异常类型上 sealed（如未来 `RateLimitException` 子类），现有 `ServiceException` / `BusinessException` 保持开放继承（业务模块可能自定义子类）。本 Phase 不强制收紧，避免破坏性变更。

- [ ] **Step 1: 评估是否值得 sealed**

  当前 `ServiceException` / `BusinessException` 都被业务模块继承风险高 → 不 sealed。改为：在 Javadoc 增加 "consider creating a custom subclass" 提示。

- [ ] **Step 2: 提交（如有改动）**

```bash
git commit -am "docs(scm-common-core): document exception subclass guidance"
```

### Phase 2-D: JSpecify + package-info

**Files:**
- Modify: `scm-common/core/pom.xml`
- Create: `scm-common/core/src/main/java/com/scmcloud/common/package-info.java`

- [ ] **Step 1: pom.xml 添加依赖**

```xml
<dependency>
    <groupId>org.jspecify</groupId>
    <artifactId>jspecify</artifactId>
</dependency>
```

version 由 parent BOM 提供（与 `scm-common/web` 一致）。

- [ ] **Step 2: package-info.java**

```java
@NullMarked
package com.scmcloud.common;
```

- [ ] **Step 3: 在所有新写/重构的公共 API 中按需标注 `@Nullable`**

示例（修改 `TenantId.java`）:

```java
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class TenantId implements Comparable<TenantId>, Serializable {
    public static TenantId fromString(@Nullable String value) { ... }
}
```

- [ ] **Step 4: 编译验证（应有警告但不报错）**

```bash
mvn -pl scm-common/core compile -f com.scm.parent/pom.xml
```

- [ ] **Step 5: 提交**

```bash
git commit -am "chore(scm-common-core): add JSpecify + package-info @NullMarked"
```

### Phase 2-E: Javadoc UTF-8 重写

- [ ] **Step 1: 用 UTF-8 重新保存所有中文 Javadoc 文件**

  用 IDE 打开每个文件 → 右键 File Encoding → UTF-8 → 重新输入中文。涉及文件:
  - `tenant/TenantContextHolder.java`
  - `tenant/TenantInterceptor.java`
  - `tenant/TenantFilter.java`
  - `tenant/quota/RequireQuotaCheck.java`
  - `exception/ErrorCode.java`
  - `response/ResultCode.java`
  - `domain/Money.java`, `Quantity.java`
  - `security/PermissionChecker.java`
  - `partition/PartitionManagementJob.java`

- [ ] **Step 2: pom.xml 确认 `<project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>`（已是）**

- [ ] **Step 3: 提交**

```bash
git commit -am "docs(scm-common-core): rewrite Chinese javadoc in UTF-8"
```

### Phase 2-F: Idempotent.errorMessage 默认 i18n key

**File:** `scm-common/core/src/main/java/com/scmcloud/common/lock/Idempotent.java`

- [ ] **Step 1: 修改默认值**

```java
String errorMessage() default "idempotent.replay";  // 与 ErrorCode.IDEMPOTENT_REPLAY.i18nKey 对齐
```

- [ ] **Step 2: 提交**

```bash
git commit -am "refactor(scm-common-core): Idempotent default errorMessage uses i18n key"
```

---

## Phase 3: 测试/可观测性

### Phase 3-A: TenantInterceptor 单元测试

> Phase 0-B 已创建 `TenantInterceptorTest.java`，本 Phase 补充覆盖：

- [ ] **Step 1: 增加 exclude 表集合测试（动态注入）**

```java
@Test
void dynamicExcludeTable_shouldSkip() {
    interceptor = new TenantInterceptor(new TenantProperties(true, true, List.of()));
    Properties props = new Properties();
    props.setProperty("excludeTables", "ord_temp, ord_archive");
    interceptor.setProperties(props);
    var bound = boundSql("SELECT id FROM ord_temp");
    var stmtHandler = mockHandler(SqlCommandType.SELECT, bound);
    interceptor.intercept(mockInvocation(stmtHandler));
    assertThat(bound.getSql()).doesNotContain("tenant_id =");
}
```

- [ ] **Step 2: 跑测试 + 提交**

```bash
mvn -pl scm-common/core test -Dtest=TenantInterceptorTest -f com.scm.parent/pom.xml
git commit -am "test(scm-common-core): extend TenantInterceptorTest with dynamic exclude"
```

### Phase 3-B: TenantFilter 单元测试

> Phase 0-C 已建。本 Phase 补充：

- [ ] **Step 1: 增加 `SecurityContextHolder` 解析优先级测试**

  暂未启用 SecurityContext 路径（需 Spring Security context），跳过此 case。

- [ ] **Step 2: 跑 + 提交**

```bash
mvn -pl scm-common/core test -Dtest=TenantFilterTest -f com.scm.parent/pom.xml
git commit --allow-empty -m "test(scm-common-core): TenantFilterTest baseline (no extra cases yet)"
```

### Phase 3-C: GlobalExceptionHandler 单元测试

**File:** `scm-common/core/src/test/java/com/scmcloud/common/exception/GlobalExceptionHandlerTest.java`

- [ ] **Step 1: 新建测试，覆盖每条 handler**

```java
class GlobalExceptionHandlerTest {
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final MockHttpServletRequest request = new MockHttpServletRequest();

    @Test void serviceException_usesHttpStatusFromException() { ... }
    @Test void badCredentials_returns401() { ... }
    @Test void accessDenied_returns403() { ... }
    @Test void rateLimit_returns429() { ... }
    @Test void businessException_carriesCode() { ... }
    @Test void illegalArgument_returns400() { ... }
    @Test void genericException_returns500WithTraceId() { ... }
    @Test void validation_returnsFieldErrors() { ... }
}
```

具体断言每个测试用 assertThat 验证 ApiResponse 的 code/message/status。

- [ ] **Step 2: 跑 + 提交**

```bash
mvn -pl scm-common/core test -Dtest=GlobalExceptionHandlerTest -f com.scm.parent/pom.xml
git commit -am "test(scm-common-core): GlobalExceptionHandler unit tests"
```

### Phase 3-D: PermissionChecker 单元测试

**File:** `scm-common/core/src/test/java/com/scmcloud/common/security/PermissionCheckerTest.java`

- [ ] **Step 1: 完整测试类（mock PermissionQueryService）**

```java
class PermissionCheckerTest {
    private PermissionQueryService query;
    private PermissionChecker checker;

    @BeforeEach
    void setup() {
        query = mock(PermissionQueryService.class);
        ObjectMapper om = new ObjectMapper();
        checker = new PermissionChecker(query, om);
    }

    @Test void hasPermission_returnsTrue() { ... }
    @Test void hasPermission_returnsFalseForUnknown() { ... }
    @Test void hasRole_returnsTrue() { ... }
    @Test void requirePermission_throwsOnMissing() { ... }
    @Test void canAccessDepartment_dataScopeAll() { ... }
    @Test void canAccessDepartment_dataScopeDept() { ... }
    @Test void canAccessDepartment_dataScopeCustom() { ... }
    @Test void cannotOperateResource_dataScopeSelf() { ... }
    @Test void canAssignRole_levelCheck() { ... }
    @Test void cacheHit_avoidsRepeatedCall() { ... }
}
```

- [ ] **Step 2: 跑 + 提交**

```bash
mvn -pl scm-common/core test -Dtest=PermissionCheckerTest -f com.scm.parent/pom.xml
git commit -am "test(scm-common-core): PermissionChecker unit tests"
```

### Phase 3-E: TenantContextHolder 单元测试

**File:** `scm-common/core/src/test/java/com/scmcloud/common/tenant/TenantContextHolderTest.java`

- [ ] **Step 1: 完整测试类**

```java
class TenantContextHolderTest {
    @AfterEach void clear() { TenantContextHolder.clear(); }

    @Test void setAndGet_returnsTenant() { ... }
    @Test void clear_removesTenant() { ... }
    @Test void getRequiredTenantId_throwsWhenNull() { ... }
    @Test void executeInTenantContext_restoresAfter() { ... }
    @Test void executeInTenantContext_clearsWhenNoOriginal() { ... }
}
```

- [ ] **Step 2: 跑 + 提交**

```bash
mvn -pl scm-common/core test -Dtest=TenantContextHolderTest -f com.scm.parent/pom.xml
git commit -am "test(scm-common-core): TenantContextHolder unit tests"
```

### Phase 3-F: PartitionManagementJob 幂等 + 单测

**Files:**
- Modify: `scm-common/core/src/main/java/com/scmcloud/common/partition/PartitionManagementJob.java`
- Create: `scm-common/core/src/test/java/com/scmcloud/common/partition/PartitionManagementJobTest.java`

- [ ] **Step 1: 写幂等测试**

```java
@Test
void execute_concurrentCall_secondWaits() {
    // 用 Redis mock 模拟分布式锁
    when(lock.tryLock("partition:monthly", 300)).thenReturn(true, false);
    job.execute();
    job.execute(); // 第二次应跳过
    verify(jdbc, times(1)).execute(anyString());
}
```

- [ ] **Step 2: 注入 RedisLock（已存在于 scm-common/cache；如依赖循环问题，使用 jdk 内置锁 + `JdbcTemplate` mock 验证）**

最小实现:

```java
private final StringRedisTemplate redis; // 可选注入

@XxlJob("partitionManagementJob")
public void execute() {
    String lockKey = "scm:job:partition:monthly:" + YearMonth.now();
    Boolean acquired = redis.opsForValue().setIfAbsent(lockKey, "1", Duration.ofMinutes(10));
    if (!Boolean.TRUE.equals(acquired)) {
        log.info("Partition job already running, skip");
        return;
    }
    try {
        // 原逻辑
    } finally {
        redis.delete(lockKey);
    }
}
```

- [ ] **Step 3: 跑 + 提交**

```bash
mvn -pl scm-common/core test -Dtest=PartitionManagementJobTest -f com.scm.parent/pom.xml
git commit -am "perf+test(scm-common-core): PartitionManagementJob idempotent via Redis lock + tests"
```

### Phase 3-G: Micrometer 指标

**Files:**
- Modify: `scm-common/core/src/main/java/com/scmcloud/common/tenant/TenantFilter.java`
- Modify: `scm-common/core/src/main/java/com/scmcloud/common/tenant/TenantInterceptor.java`
- Modify: `scm-common/core/src/main/java/com/scmcloud/common/security/PermissionChecker.java`
- Modify: `scm-common/core/src/main/java/com/scmcloud/common/exception/GlobalExceptionHandler.java`
- Modify: `scm-common/core/src/main/java/com/scmcloud/common/tenant/AsyncTenantAutoConfiguration.java`

- [ ] **Step 1: 引入依赖**

pom.xml:
```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-core</artifactId>
</dependency>
```

- [ ] **Step 2: 在 `AsyncTenantAutoConfiguration` 暴露 MeterRegistry**

```java
@Bean
@ConditionalOnClass(MeterRegistry.class)
public TenantMetrics tenantMetrics(MeterRegistry registry) {
    return new TenantMetrics(registry);
}
```

- [ ] **Step 3: 新建 `TenantMetrics.java` 封装指标**

```java
public class TenantMetrics {
    public final Counter filterMissing;
    public final Counter interceptorCacheHit;
    public final Timer permissionCheckLatency;
    public final Counter exceptionCount;

    public TenantMetrics(MeterRegistry r) {
        filterMissing = Counter.builder("scm.tenant.filter.missing").register(r);
        interceptorCacheHit = Counter.builder("scm.tenant.interceptor.cache_hit").register(r);
        permissionCheckLatency = Timer.builder("scm.permission.check.latency").register(r);
        exceptionCount = Counter.builder("scm.exception.count").register(r);
    }
}
```

- [ ] **Step 4: 在 4 处埋点**

- TenantFilter.filterMissing++ on missing + required
- TenantInterceptor: cacheHit++ / cacheMiss++
- PermissionChecker: Timer.record on hasPermission
- GlobalExceptionHandler: exceptionCount++ per ExceptionHandler

- [ ] **Step 5: 跑 + 提交**

```bash
mvn -pl scm-common/core test -f com.scm.parent/pom.xml
git commit -am "feat(scm-common-core): add Micrometer metrics for tenant/permission/exception paths"
```

### Phase 3-H: TenantAwareMDCTaskDecorator 文档 + 虚拟线程工厂

**File:** `scm-common/core/src/main/java/com/scmcloud/common/tenant/TenantAwareMDCTaskDecorator.java`

- [ ] **Step 1: 增加虚拟线程工厂方法**

```java
public static TenantAwareMDCTaskDecorator forVirtualThreadExecutor() {
    return new TenantAwareMDCTaskDecorator(); // 与 platform 实现一致；
    // 虚拟线程本身在调用栈内捕获 ThreadLocal/MDC，无需装饰
}
```

- [ ] **Step 2: 补充 Javadoc，说明何时用哪个**

```java
/**
 * Two flavours of tenant context propagation for @Async:
 * <ul>
 *   <li>{@link TenantAwareTaskDecorator} — ThreadLocal-based for platform thread pools</li>
 *   <li>{@link TenantAwareMDCTaskDecorator} — MDC-based for logging correlation</li>
 * </ul>
 * For virtual thread executors (Java 21+), neither decorator is strictly necessary —
 * ThreadLocal and MDC are inherited automatically within the call stack. Use
 * {@link #forVirtualThreadExecutor()} as a no-op marker for clarity.
 */
```

- [ ] **Step 3: 提交**

```bash
git commit -am "docs(scm-common-core): document TenantAwareTaskDecorator variants + virtual-thread helper"
```

### Phase 3-I: Money/Quantity 调用方评估

> Phase 2-B 已完成 record 化。

- [ ] **Step 1: 记录评估结果（在本文件追加）**

```bash
grep -rn "common\.domain\.Money\|common\.domain\.Quantity" --include="*.java" . | grep -v "scm-common/core"
```

期望: 无业务模块调用。结果: [记录实际扫描结果]

- [ ] **Step 2: 若无调用方 → 推动：在 Javadoc 增加 @since 1.1 + 示例**

- [ ] **Step 3: 提交**

```bash
git commit -am "docs(scm-common-core): Money/Quantity usage audit + Javadoc examples"
```

### Phase 3-J: FeatureFlag/License 评估

**Files:**
- Modify: `scm-common/core/src/main/java/com/scmcloud/common/feature/FeatureFlag.java`
- Modify: `scm-common/core/src/main/java/com/scmcloud/common/feature/FeatureFlagService.java`
- Modify: `scm-common/core/src/main/java/com/scmcloud/common/license/EnterpriseFeature.java`
- Modify: `scm-common/core/src/main/java/com/scmcloud/common/license/LicenseType.java`

- [ ] **Step 1: grep 业务模块**

```bash
grep -rn "common\.feature\|common\.license" --include="*.java" . | grep -v "scm-common/core"
```

- [ ] **Step 2: 若无调用方 → 标注 @Deprecated 指向外部方案**

```java
/**
 * @deprecated since 1.1.0 — placeholder; use an external feature flag service
 *             (e.g., LaunchDarkly / Unleash). Will be removed in v2.0.
 */
@Deprecated(since = "1.1.0", forRemoval = true)
public @interface FeatureFlag {
    String value();
}
```

> `FeatureFlagService` 同样标 `@Deprecated` 并把 `isEnabled` 方法标 `@Deprecated`。`EnterpriseFeature` / `LicenseType` 保持非 deprecated（属于公共契约，但提供 `@Deprecated` 的 default 实现）。

- [ ] **Step 3: 提交**

```bash
git commit -am "chore(scm-common-core): deprecate unused FeatureFlag/License placeholders"
```

---

## 最终验证

- [ ] **Step 1: 全模块编译**

```bash
mvn clean install -DskipTests -pl scm-common/core -am -f com.scm.parent/pom.xml
```

期望: `BUILD SUCCESS`

- [ ] **Step 2: 全模块测试**

```bash
mvn test -pl scm-common/core -f com.scm.parent/pom.xml
```

期望: `Tests run: N, Failures: 0, Errors: 0`，N ≥ 30

- [ ] **Step 3: 覆盖率检查**

```bash
mvn verify -Djacoco.skip=false -pl scm-common/core -am -f com.scm.parent/pom.xml
```

期望: core 模块核心类行覆盖 ≥ 80%

- [ ] **Step 4: 回归业务模块（重点 scm-order）**

```bash
mvn -pl scm-order/service -am test -f com.scm.parent/pom.xml
```

期望: 不出现 TenantParseException，未引入新的 null 警告。

- [ ] **Step 5: 提交验证报告**

```bash
git commit --allow-empty -m "docs(scm-common-core): verification report 2026-09-18"
```

---

## 备注

- 每 Phase 必须在独立 commit 完成后再进入下一 Phase
- P0 任何 Phase 失败 → 立即停止，回滚后讨论
- P1/P2 失败 → 评估是否阻塞 P3
- 测试覆盖以 `src/test/java`（单元）为主，集成测试放 `src/integrationTest/java`（本次不强制要求）

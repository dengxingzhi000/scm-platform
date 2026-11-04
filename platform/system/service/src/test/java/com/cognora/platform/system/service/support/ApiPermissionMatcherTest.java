package com.cognora.platform.system.service.support;

import com.cognora.platform.system.domain.entity.SysPermission;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiPermissionMatcherTest {

    @Test
    void literalPathMatchesExactlyAndNothingElse() {
        assertTrue(ApiPermissionMatcher.matches("/api/system/users", "/api/system/users"));
        assertFalse(ApiPermissionMatcher.matches("/api/system/users", "/api/system/users/1"));
        assertFalse(ApiPermissionMatcher.matches("/api/system/users", "/api/system/user"));
        assertFalse(ApiPermissionMatcher.matches("/api/system/users", "/api/system/users/"));
        assertFalse(ApiPermissionMatcher.matches("/api/system/users", null));
    }

    @Test
    void placeholderTemplateMatchesSingleSegment() {
        assertTrue(ApiPermissionMatcher.matches("/api/v1/orders/{id}", "/api/v1/orders/123"));
        assertTrue(ApiPermissionMatcher.matches(
                "/api/v1/orders/{id}", "/api/v1/orders/11111111-1111-1111-1111-111111111111"));
        assertFalse(ApiPermissionMatcher.matches("/api/v1/orders/{id}", "/api/v1/orders/123/items"));
        assertFalse(ApiPermissionMatcher.matches("/api/v1/orders/{id}", "/api/v1/orders/"));
        assertFalse(ApiPermissionMatcher.matches("/api/v1/orders/{id}", "/api/v1/orders"));
    }

    @Test
    void multiPlaceholderTemplateMatchesAllSegments() {
        assertTrue(ApiPermissionMatcher.matches(
                "/api/v1/analytics/datasets/{datasetId}/fields/{id}",
                "/api/v1/analytics/datasets/ds-1/fields/f-2"));
        assertFalse(ApiPermissionMatcher.matches(
                "/api/v1/analytics/datasets/{datasetId}/fields/{id}",
                "/api/v1/analytics/datasets/ds-1/fields"));
    }

    @Test
    void antWildcardsMatch() {
        assertTrue(ApiPermissionMatcher.matches("/api/orders/*", "/api/orders/1"));
        assertFalse(ApiPermissionMatcher.matches("/api/orders/*", "/api/orders/1/items"));
        assertTrue(ApiPermissionMatcher.matches("/api/orders/**", "/api/orders/1/items"));
        assertTrue(ApiPermissionMatcher.matches("/api/orders/**", "/api/orders"));
    }

    @Test
    void resolveKeepsOnlyMethodCompatibleRowsAndDeduplicates() {
        SysPermission getRow = row("order:order:view", "/api/v1/orders/{id}", "GET");
        SysPermission wildcardRow = row("order:order:any", "/api/v1/orders/{id}", "*");
        SysPermission postRow = row("order:order:update", "/api/v1/orders/{id}", "POST");
        SysPermission duplicate = row("order:order:view", "/api/v1/orders/{id}", "GET");

        List<String> codes = ApiPermissionMatcher.resolve(
                List.of(getRow, wildcardRow, postRow, duplicate), "/api/v1/orders/9", "GET");

        assertEquals(List.of("order:order:view", "order:order:any"), codes);
    }

    @Test
    void resolveReturnsEmptyWhenNothingMatches() {
        SysPermission row = row("order:order:view", "/api/v1/orders/{id}", "GET");

        assertEquals(List.of(), ApiPermissionMatcher.resolve(List.of(row), "/api/v1/products", "GET"));
        assertEquals(List.of(), ApiPermissionMatcher.resolve(List.of(row), "/api/v1/orders/9", "DELETE"));
        assertEquals(List.of(), ApiPermissionMatcher.resolve(null, "/api/v1/orders/9", "GET"));
    }

    @Test
    void resolveSkipsRowsWithoutHttpMethodLikeTheOldExactQueryDid() {
        SysPermission row = row("order:order:view", "/api/v1/orders/{id}", null);

        assertEquals(List.of(), ApiPermissionMatcher.resolve(List.of(row), "/api/v1/orders/9", "GET"));
    }

    @Test
    void brandIdRoutesResolveByHttpMethodWithoutBypassingWriteCodes() {
        SysPermission view = row("product:brand:view", "/api/v1/prod-brand/{id}", "GET");
        SysPermission update = row("product:brand:update", "/api/v1/prod-brand/{id}", "PUT");
        SysPermission delete = row("product:brand:delete", "/api/v1/prod-brand/{id}", "DELETE");
        SysPermission list = row("product:brand:list", "/api/v1/prod-brand", "GET");
        List<SysPermission> rows = List.of(view, update, delete, list);

        assertEquals(List.of("product:brand:view"),
                ApiPermissionMatcher.resolve(rows, "/api/v1/prod-brand/b-1", "GET"));
        assertEquals(List.of("product:brand:update"),
                ApiPermissionMatcher.resolve(rows, "/api/v1/prod-brand/b-1", "PUT"));
        assertEquals(List.of("product:brand:delete"),
                ApiPermissionMatcher.resolve(rows, "/api/v1/prod-brand/b-1", "DELETE"));
        // 集合路由不能被 {id} 模板吞掉
        assertEquals(List.of("product:brand:list"),
                ApiPermissionMatcher.resolve(rows, "/api/v1/prod-brand", "GET"));
        assertEquals(List.of(),
                ApiPermissionMatcher.resolve(rows, "/api/v1/prod-brand", "PUT"));
    }

    @Test
    void literalBrandSubRoutesStillResolveNextToTheIdTemplate() {
        SysPermission featured = row("product:brand:list-featured", "/api/v1/prod-brand/featured", "GET");
        SysPermission search = row("product:brand:search", "/api/v1/prod-brand/search", "GET");
        SysPermission view = row("product:brand:view", "/api/v1/prod-brand/{id}", "GET");
        List<SysPermission> rows = List.of(featured, search, view);

        // 模板会同时命中 featured/search（都是只读），字面权限码仍然独立存在；
        // ApiAccessControlFilter 是“任一命中即通过”，因此不会误拒。
        assertTrue(ApiPermissionMatcher.resolve(rows, "/api/v1/prod-brand/featured", "GET")
                .contains("product:brand:list-featured"));
        assertTrue(ApiPermissionMatcher.resolve(rows, "/api/v1/prod-brand/search", "GET")
                .contains("product:brand:search"));
        assertEquals(List.of("product:brand:view"),
                ApiPermissionMatcher.resolve(rows, "/api/v1/prod-brand/b-1", "GET"));
    }

    private static SysPermission row(String code, String apiPath, String method) {
        SysPermission permission = new SysPermission();
        permission.setPermissionCode(code);
        permission.setApiPath(apiPath);
        permission.setHttpMethod(method);
        return permission;
    }
}

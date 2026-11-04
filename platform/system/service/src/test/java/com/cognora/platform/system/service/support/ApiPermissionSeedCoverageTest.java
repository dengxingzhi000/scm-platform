package com.cognora.platform.system.service.support;

import com.cognora.platform.system.domain.entity.SysPermission;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 用 seed 里的权限定义做参数化回归：019 的 350 条 {var} 模板必须全部能命中，
 * 字面量路径（019 段 1 + 002）必须仍然精确命中。
 *
 * <p>数据集直接从仓库里的 SQL 读取，避免测试副本与 seed 漂移。</p>
 */
class ApiPermissionSeedCoverageTest {

    private static final Pattern VALUE_ROW = Pattern.compile(
            "\\('([^']+)',\\s*'[^']*',\\s*'[^']*',\\s*(NULL|'([^']*)'),\\s*(NULL|'([^']*)')");

    private static final String SAMPLE_SEGMENT = "11111111-1111-1111-1111-111111111111";

    private static final Pattern HTTP_METHOD = Pattern.compile("GET|POST|PUT|DELETE|PATCH|\\*");

    @Test
    void templatedSeedPathsAllResolve() throws IOException {
        List<Row> rows = seedRows();
        List<Row> templated = rows.stream().filter(row -> isTemplate(row.apiPath)).toList();

        assertEquals(350, templated.size(), "seed 019 段 2 的模板端点数变了，先确认 seed 再改断言");

        List<SysPermission> candidates = toCandidates(rows);
        for (Row row : templated) {
            String requestPath = row.apiPath.replaceAll("\\{[^/}]+}", SAMPLE_SEGMENT);
            List<String> codes = ApiPermissionMatcher.resolve(candidates, requestPath, row.method);
            assertTrue(codes.contains(row.code),
                    () -> "模板未命中: " + row.method + " " + row.apiPath + " -> " + requestPath);
        }
    }

    @Test
    void literalSeedPathsAllStillResolve() throws IOException {
        List<Row> rows = seedRows();
        List<Row> literal = rows.stream().filter(row -> !isTemplate(row.apiPath)).toList();

        assertEquals(214, literal.size(), "字面量端点数变了（019 段 1 203 + 002 11），先确认 seed 再改断言");

        List<SysPermission> candidates = toCandidates(rows);
        for (Row row : literal) {
            List<String> codes = ApiPermissionMatcher.resolve(candidates, row.apiPath, row.method);
            assertTrue(codes.contains(row.code),
                    () -> "字面量未命中: " + row.method + " " + row.apiPath);
        }
    }

    @Test
    void unknownRequestPathStillResolvesToNothing() throws IOException {
        List<SysPermission> candidates = toCandidates(seedRows());

        assertEquals(List.of(),
                ApiPermissionMatcher.resolve(candidates, "/api/v1/not-registered/abc", "GET"));
    }

    private static boolean isTemplate(String apiPath) {
        return apiPath.indexOf('{') >= 0 || apiPath.indexOf('*') >= 0 || apiPath.indexOf('?') >= 0;
    }

    private static List<SysPermission> toCandidates(List<Row> rows) {
        List<SysPermission> candidates = new ArrayList<>();
        for (Row row : rows) {
            SysPermission permission = new SysPermission();
            permission.setPermissionCode(row.code);
            permission.setApiPath(row.apiPath);
            permission.setHttpMethod(row.method);
            candidates.add(permission);
        }
        return candidates;
    }

    private static List<Row> seedRows() throws IOException {
        List<Row> rows = new ArrayList<>();
        for (String file : List.of("019_api_permission_coverage.sql", "002_rbac_permission.sql")) {
            for (String line : Files.readAllLines(seedFile(file), StandardCharsets.UTF_8)) {
                Matcher matcher = VALUE_ROW.matcher(line);
                if (matcher.find() && !"NULL".equals(matcher.group(2)) && !"NULL".equals(matcher.group(4))
                        && matcher.group(3).startsWith("/")
                        && HTTP_METHOD.matcher(matcher.group(5)).matches()) {
                    rows.add(new Row(matcher.group(1), matcher.group(3), matcher.group(5)));
                }
            }
        }
        return rows;
    }

    private static Path seedFile(String fileName) {
        List<Path> candidates = List.of(
                Path.of("../../../scripts/db/seed", fileName),
                Path.of("scripts/db/seed", fileName),
                Path.of("../../../../scripts/db/seed", fileName));
        for (Path candidate : candidates) {
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("找不到 seed 文件，尝试过: " + candidates);
    }

    private record Row(String code, String apiPath, String method) {
    }
}

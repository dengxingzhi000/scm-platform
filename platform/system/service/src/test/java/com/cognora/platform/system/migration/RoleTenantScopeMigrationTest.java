package com.cognora.platform.system.migration;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 静态防回归：ROLE_TENANT_ADMIN 不得再被授予 tenant:* 平台权限。
 *
 * <p>007 迁移负责回收历史越权关系行，002 种子负责不再复发。CI 的 push 任务
 * 用真实 PostgreSQL 跑夹具 + 迁移 + 断言；本测试补一道不依赖数据库的快检，
 * 防止有人把两个脚本里的过滤条件删掉。</p>
 */
class RoleTenantScopeMigrationTest {

    private static final String ROLE_TENANT_ADMIN = "'ROLE_TENANT_ADMIN'";

    @Test
    void seedDoesNotRegrantTenantScopedPermissionsToTenantAdmin() throws IOException {
        String seed = read("scripts/db/seed/002_rbac_permission.sql");

        String tenantAdminBlock = blockAfter(seed, "r.role_code = 'ROLE_TENANT_ADMIN'");
        assertTrue(tenantAdminBlock.contains("p.permission_code NOT LIKE 'tenant:%'"),
                "002 种子给 ROLE_TENANT_ADMIN 的授权必须排除 tenant:*，否则重跑种子会复发越权");

        String superAdminBlock = blockAfter(seed, "r.role_code = 'ROLE_SUPER_ADMIN'");
        assertFalse(superAdminBlock.contains("NOT LIKE 'tenant:%'"),
                "ROLE_SUPER_ADMIN 应保留 tenant:* 平台权限");
    }

    @Test
    void migrationOnlyRemovesTenantScopedRoleBindings() throws IOException {
        String migration = read("scripts/db/migrations/007_strip_tenant_scope_from_tenant_admin.sql");

        assertTrue(migration.contains(ROLE_TENANT_ADMIN), "迁移必须只针对 ROLE_TENANT_ADMIN");
        assertTrue(migration.contains("p.permission_code LIKE 'tenant:%'"),
                "迁移只能删除 tenant:* 权限的关系行");
        assertTrue(migration.contains("DELETE FROM sys_role_permission"),
                "迁移应删除角色-权限关系行");
        assertFalse(migration.contains("DELETE FROM sys_permission"),
                "迁移不得删除权限定义本身（sys_permission）");
    }

    @Test
    void validationScriptFailsWhenTenantScopedBindingRemains() throws IOException {
        String validation = read("scripts/db/ci_validate_tenant_admin_scope.sql");

        assertTrue(validation.contains("p.permission_code LIKE 'tenant:%'"),
                "校验脚本必须按 tenant:* 前缀判定");
        assertTrue(validation.contains("RAISE EXCEPTION"),
                "校验脚本必须用 RAISE EXCEPTION 让 CI 失败，而不是只打印日志");
    }

    private static String blockAfter(String content, String marker) {
        int index = content.indexOf(marker);
        assertTrue(index >= 0, "未找到片段标记: " + marker);
        return content.substring(index, Math.min(content.length(), index + 400));
    }

    private static String read(String relativePath) throws IOException {
        Path root = repoRoot();
        Path file = root.resolve(relativePath);
        assertTrue(Files.exists(file), "文件不存在: " + file);
        return Files.readString(file);
    }

    /** Maven surefire 的工作目录是模块目录，向上找到含 scripts/db 的仓库根。 */
    private static Path repoRoot() {
        Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (current != null) {
            if (Files.exists(current.resolve("scripts/db/seed/002_rbac_permission.sql"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("未能定位仓库根目录（从 " + System.getProperty("user.dir") + " 向上查找）");
    }
}

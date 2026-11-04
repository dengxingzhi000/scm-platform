-- ======================================================================
-- 007 — 从 ROLE_TENANT_ADMIN 剥离 tenant:* 平台管理权限
-- 目标库：db_permission
-- 类型：一次性数据迁移（非 schema 变更），幂等，可重复执行
--
-- 背景：
--   ROLE_TENANT_ADMIN 是租户内的最高权限角色，只能管理本租户资源，
--   不能创建/停用/删除“租户”这一平台级资源，否则就是跨租户越权面。
--   历史上有两处会把 tenant:* 授予该角色：
--     * 002_rbac_permission.sql 曾对 ROLE_TENANT_ADMIN 写入“全部权限”；
--     * 019_api_permission_coverage.sql 虽然排除了 tenant:*，但它只做增量
--       授予，不回收已存在的历史关系行。
--   所以“先跑 019 再跑 002”的环境里，租户管理员会拿到 tenant:* 授权。
--
-- 口径：
--   * 只删除 sys_role_permission 关系行，不删除 sys_permission 权限定义本身；
--   * 按 role_code 匹配，覆盖所有租户的 ROLE_TENANT_ADMIN，不写死默认租户；
--   * 不触碰 ROLE_SUPER_ADMIN，也不触碰 role_type=PLATFORM_ROLE 的平台角色。
--
-- 执行（现网）：
--   psql -h <host> -U <user> -d db_permission -v ON_ERROR_STOP=1 \
--        -f scripts/db/migrations/007_strip_tenant_scope_from_tenant_admin.sql
--
-- 执行后必须让权限缓存失效（scm-system 的 CacheManager 是 RedisCacheManager，
-- 默认 TTL 1 小时，重启进程不会清掉）：
--   redis-cli --scan --pattern 'userPermissions::*' | xargs -r redis-cli del
--   redis-cli --scan --pattern 'userPermissionCodes::*' | xargs -r redis-cli del
--   redis-cli --scan --pattern 'userMenuTree::*' | xargs -r redis-cli del
--   redis-cli --scan --pattern 'userDetails::*' | xargs -r redis-cli del
-- 或调用任意会触发 UserAuthorizationCacheEvictor#evictRolePermissionGrant 的角色写接口。
-- ======================================================================

BEGIN;

DO $$
DECLARE
    before_cnt INT;
    after_cnt INT;
    removed_cnt INT;
BEGIN
    SELECT count(*)
      INTO before_cnt
      FROM sys_role_permission rp
      JOIN sys_role r ON r.id = rp.role_id
      JOIN sys_permission p ON p.id = rp.permission_id
     WHERE r.role_code = 'ROLE_TENANT_ADMIN';

    DELETE FROM sys_role_permission rp
     USING sys_role r, sys_permission p
     WHERE r.id = rp.role_id
       AND p.id = rp.permission_id
       AND r.role_code = 'ROLE_TENANT_ADMIN'
       AND p.permission_code LIKE 'tenant:%';

    GET DIAGNOSTICS removed_cnt = ROW_COUNT;

    SELECT count(*)
      INTO after_cnt
      FROM sys_role_permission rp
      JOIN sys_role r ON r.id = rp.role_id
      JOIN sys_permission p ON p.id = rp.permission_id
     WHERE r.role_code = 'ROLE_TENANT_ADMIN';

    RAISE NOTICE 'ROLE_TENANT_ADMIN 权限条数：迁移前 %，删除 tenant:* % 条，迁移后 %',
        before_cnt, removed_cnt, after_cnt;

    IF after_cnt <> before_cnt - removed_cnt THEN
        RAISE EXCEPTION '权限条数校验失败：before=%, removed=%, after=%',
            before_cnt, removed_cnt, after_cnt;
    END IF;
END $$;

COMMIT;

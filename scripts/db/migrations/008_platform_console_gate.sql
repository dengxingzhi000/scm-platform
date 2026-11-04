-- ======================================================================
-- 008 — 平台控制台门禁：回收非平台角色的 menu:tenant% 菜单权限
-- 目标库：db_permission
-- 类型：一次性数据迁移（非 schema 变更），幂等，可重复执行
--
-- 背景：
--   平台控制台（租户管理）的 API 权限是 tenant:*，已经由 007 从
--   ROLE_TENANT_ADMIN 回收；但控制台菜单权限 menu:tenant% 仍会被
--   002 种子的“全部菜单”规则授予 ROLE_DEPARTMENT_MANAGER，并在
--   历史环境里残留给 ROLE_TENANT_ADMIN。结果是租户管理员虽然调不通
--   接口，却能在前端看到平台控制台入口。
--
-- 口径：
--   * 以“是否持有 tenant:% API 权限”判定平台角色：持有者保留控制台菜单，
--     不持有者一律回收 menu:tenant%；不写死角色名，覆盖面更稳。
--   * 只删除 sys_role_permission 关系行，不删除 sys_permission 定义。
--   * 幂等：第二次执行删除 0 条，前后条数校验仍成立。
--
-- 执行（现网）：
--   psql -h <host> -U <user> -d db_permission -v ON_ERROR_STOP=1 \
--        -f scripts/db/migrations/008_platform_console_gate.sql
--
-- 执行后必须让菜单/权限缓存失效（RedisCacheManager 默认 TTL 1 小时，
-- 重启进程不会清掉）：
--   redis-cli --scan --pattern 'userMenuTree::*' | xargs -r redis-cli del
--   redis-cli --scan --pattern 'userPermissionCodes::*' | xargs -r redis-cli del
--   redis-cli --scan --pattern 'userPermissions::*' | xargs -r redis-cli del
--   redis-cli --scan --pattern 'permissionTree::*' | xargs -r redis-cli del
-- 或调用任意会触发 UserAuthorizationCacheEvictor#evictRolePermissionGrant
-- 的角色写接口。
-- ======================================================================

BEGIN;

DO $$
DECLARE
    before_cnt INT;
    after_cnt INT;
    removed_cnt INT;
    platform_role_cnt INT;
    platform_menu_cnt INT;
BEGIN
    SELECT count(*)
      INTO before_cnt
      FROM sys_role_permission rp
      JOIN sys_permission p ON p.id = rp.permission_id
     WHERE p.permission_code LIKE 'menu:tenant%';

    DELETE FROM sys_role_permission rp
     USING sys_role r, sys_permission p
     WHERE r.id = rp.role_id
       AND p.id = rp.permission_id
       AND p.permission_code LIKE 'menu:tenant%'
       AND NOT EXISTS (
           SELECT 1
             FROM sys_role_permission xrp
             JOIN sys_permission xp ON xp.id = xrp.permission_id
            WHERE xrp.role_id = r.id
              AND xp.permission_code LIKE 'tenant:%'
       );

    GET DIAGNOSTICS removed_cnt = ROW_COUNT;

    SELECT count(*)
      INTO after_cnt
      FROM sys_role_permission rp
      JOIN sys_permission p ON p.id = rp.permission_id
     WHERE p.permission_code LIKE 'menu:tenant%';

    SELECT count(DISTINCT rp.role_id)
      INTO platform_role_cnt
      FROM sys_role_permission rp
      JOIN sys_permission p ON p.id = rp.permission_id
     WHERE p.permission_code LIKE 'tenant:%';

    SELECT count(DISTINCT rp.role_id)
      INTO platform_menu_cnt
      FROM sys_role_permission rp
      JOIN sys_permission p ON p.id = rp.permission_id
     WHERE p.permission_code LIKE 'menu:tenant%';

    RAISE NOTICE 'menu:tenant%% 绑定数：迁移前 %，删除 % 条，迁移后 %；持有 tenant:* 的角色 % 个，保留控制台菜单的角色 % 个',
        before_cnt, removed_cnt, after_cnt, platform_role_cnt, platform_menu_cnt;

    IF after_cnt <> before_cnt - removed_cnt THEN
        RAISE EXCEPTION '控制台菜单条数校验失败：before=%, removed=%, after=%',
            before_cnt, removed_cnt, after_cnt;
    END IF;

    -- 控制组：存在平台角色时，至少应有一个平台角色仍保留控制台菜单。
    IF platform_role_cnt > 0 AND platform_menu_cnt = 0 THEN
        RAISE EXCEPTION '平台角色 % 个但已无任何 menu:tenant%% 绑定，疑似误删平台控制台菜单',
            platform_role_cnt;
    END IF;
END $$;

COMMIT;

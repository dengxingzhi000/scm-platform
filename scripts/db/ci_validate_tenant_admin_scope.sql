-- ======================================================================
-- CI 断言：平台控制台门禁
--   1. ROLE_TENANT_ADMIN 最终权限集合不得含任何 tenant:* 权限（API 门禁）；
--   2. ROLE_TENANT_ADMIN 不得含任何 menu:tenant% 菜单权限（控制台入口）；
--   3. 任何持有 tenant:* 的平台角色必须至少保留一条 menu:tenant% 菜单。
--
-- 使用：
--   psql -h localhost -U admin -d db_permission -v ON_ERROR_STOP=1 \
--        -f scripts/db/ci_validate_tenant_admin_scope.sql
--
-- 有越权行时 RAISE EXCEPTION，配合 -v ON_ERROR_STOP=1 让 CI 步骤失败。
-- 无越权行时无输出、退出码 0。
-- ======================================================================

DO $$
DECLARE
    tenant_violation_cnt INT;
    menu_violation_cnt INT;
    platform_role_cnt INT;
    platform_menu_cnt INT;
BEGIN
    SELECT count(*)
      INTO tenant_violation_cnt
      FROM sys_role_permission rp
      JOIN sys_role r ON r.id = rp.role_id
      JOIN sys_permission p ON p.id = rp.permission_id
     WHERE r.role_code = 'ROLE_TENANT_ADMIN'
       AND p.permission_code LIKE 'tenant:%';

    IF tenant_violation_cnt <> 0 THEN
        RAISE EXCEPTION 'ROLE_TENANT_ADMIN 仍持有 % 条 tenant:* 权限（越权面未收口）', tenant_violation_cnt;
    END IF;

    SELECT count(*)
      INTO menu_violation_cnt
      FROM sys_role_permission rp
      JOIN sys_role r ON r.id = rp.role_id
      JOIN sys_permission p ON p.id = rp.permission_id
     WHERE r.role_code = 'ROLE_TENANT_ADMIN'
       AND p.permission_code LIKE 'menu:tenant%';

    IF menu_violation_cnt <> 0 THEN
        RAISE EXCEPTION 'ROLE_TENANT_ADMIN 仍持有 % 条 menu:tenant%% 菜单权限（平台控制台入口未收口）',
            menu_violation_cnt;
    END IF;

    -- 非平台角色（不持有 tenant:*）一律不得残留控制台菜单。
    SELECT count(*)
      INTO menu_violation_cnt
      FROM sys_role_permission rp
      JOIN sys_role r ON r.id = rp.role_id
      JOIN sys_permission p ON p.id = rp.permission_id
     WHERE p.permission_code LIKE 'menu:tenant%'
       AND NOT EXISTS (
           SELECT 1
             FROM sys_role_permission xrp
             JOIN sys_permission xp ON xp.id = xrp.permission_id
            WHERE xrp.role_id = r.id
              AND xp.permission_code LIKE 'tenant:%'
       );

    IF menu_violation_cnt <> 0 THEN
        RAISE EXCEPTION '仍有 % 条 menu:tenant%% 绑定落在非平台角色上', menu_violation_cnt;
    END IF;

    -- 控制组：平台角色必须保留控制台菜单。
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

    IF platform_role_cnt > 0 AND platform_menu_cnt = 0 THEN
        RAISE EXCEPTION '平台角色 % 个但已无任何 menu:tenant%% 绑定（平台控制台菜单被误删）',
            platform_role_cnt;
    END IF;
END $$;

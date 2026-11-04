-- ======================================================================
-- CI 断言：ROLE_TENANT_ADMIN 最终权限集合不得含任何 tenant:* 权限
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
    violation_cnt INT;
BEGIN
    SELECT count(*)
      INTO violation_cnt
      FROM sys_role_permission rp
      JOIN sys_role r ON r.id = rp.role_id
      JOIN sys_permission p ON p.id = rp.permission_id
     WHERE r.role_code = 'ROLE_TENANT_ADMIN'
       AND p.permission_code LIKE 'tenant:%';

    IF violation_cnt <> 0 THEN
        RAISE EXCEPTION 'ROLE_TENANT_ADMIN 仍持有 % 条 tenant:* 权限（越权面未收口）', violation_cnt;
    END IF;
END $$;

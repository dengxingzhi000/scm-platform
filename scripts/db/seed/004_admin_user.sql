-- ======================================================================
-- 本地开发种子数据 004 — admin 管理员属性
-- 目标库：db_user
--
-- 将 admin 标记为租户管理员并归入信息中心，与 003_org_dept.sql 保持一致。
-- 幂等：只更新目标行的属性字段。
-- ======================================================================
BEGIN;

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

UPDATE sys_user
SET user_type = 'TENANT_ADMIN',
    data_scope = 'ALL',
    dept_id = uuid_generate_v5(uuid_ns_url(), 'scm:dept:IT'),
    update_time = NOW()
WHERE tenant_id = '019a0aee-0000-7000-8000-000000000002'
  AND username = 'admin'
  AND NOT deleted;

COMMIT;

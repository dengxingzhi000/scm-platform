-- ======================================================================
-- CI 夹具：制造“ROLE_TENANT_ADMIN 持有 tenant:*”的历史越权状态
-- 目标库：db_permission（init-all-databases.sh 刚初始化）
--
-- 说明：
--   CI 不会执行 seed-all（它依赖完整 RBAC schema 与默认租户数据），
--   这里只造 007 迁移与校验断言所需的最小数据：
--     1 个 ROLE_TENANT_ADMIN 角色 + 1 条 tenant:* 权限 + 1 条对照业务权限，
--   并把两条权限绑定到该角色（模拟历史越权 + 对照组）。
--   随后由 007 迁移回收 tenant:*，校验脚本断言回收干净且对照组保留。
--
-- 幂等：INSERT ... ON CONFLICT DO NOTHING，可重复执行。
-- ======================================================================

BEGIN;

-- 004 迁移才会补的两列；这里最小补齐，使 007 与校验脚本可直接运行
ALTER TABLE sys_permission ADD COLUMN IF NOT EXISTS tenant_id UUID;
ALTER TABLE sys_role_permission ADD COLUMN IF NOT EXISTS tenant_id UUID;

INSERT INTO sys_role (id, tenant_id, role_code, role_name, role_level, data_scope, role_type, status)
VALUES (uuid_generate_v5(uuid_ns_url(), 'scm:role:tenant-admin:ci'),
        '019a0aee-0000-7000-8000-000000000002', 'ROLE_TENANT_ADMIN', '租户管理员',
        7, 1, 'TENANT_ROLE', 1)
ON CONFLICT (tenant_id, role_code) DO NOTHING;

INSERT INTO sys_permission (id, permission_code, permission_name, tenant_id)
VALUES (uuid_generate_v5(uuid_ns_url(), 'scm:perm:tenant:tenant:page'),
        'tenant:tenant:page', '租户-分页', '019a0aee-0000-7000-8000-000000000002'),
       (uuid_generate_v5(uuid_ns_url(), 'scm:perm:product:brand:view'),
        'product:brand:view', '品牌-查看', '019a0aee-0000-7000-8000-000000000002')
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO sys_role_permission (id, role_id, permission_id, tenant_id, create_time)
SELECT uuid_generate_v5(uuid_ns_url(), 'scm:rp:ci:' || r.role_code || ':' || p.permission_code),
       r.id, p.id, r.tenant_id, NOW()
  FROM sys_role r
  JOIN sys_permission p
    ON p.permission_code IN ('tenant:tenant:page', 'product:brand:view')
 WHERE r.role_code = 'ROLE_TENANT_ADMIN'
ON CONFLICT (role_id, permission_id) DO NOTHING;

COMMIT;

-- ======================================================================
-- 用户管理企业级增强 — 批次 4（导入导出）权限登记
-- 目标库：db_permission
-- 幂等：固定 UUID v5 + ON CONFLICT 更新；角色绑定 ON CONFLICT DO NOTHING。
-- ======================================================================

BEGIN;

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

INSERT INTO sys_permission (
    id, parent_id, permission_code, permission_name, permission_type,
    api_path, http_method, sort_order, visible, status,
    tenant_id, permission_scope, permission_level, risk_level,
    need_approval, need_two_factor, create_time, update_time, deleted
)
SELECT uuid_generate_v5(uuid_ns_url(), 'scm:perm:' || v.code),
       (SELECT p.id FROM sys_permission p WHERE p.permission_code = 'menu:system:user' LIMIT 1),
       v.code, v.name, 3, v.api, v.method, v.sort_order,
       TRUE, 1,
       '019a0aee-0000-7000-8000-000000000002', 'TENANT', 1, v.risk,
       FALSE, FALSE, NOW(), NOW(), FALSE
FROM (VALUES
    ('system:user:import',           '导入用户',       '/api/system/users/import', 'POST', 41, 3),
    ('system:user:import:template',  '下载导入模板',   '/api/system/users/import/template', 'GET', 42, 1),
    ('system:user:export',           '导出用户',       '/api/system/users/export', 'GET', 43, 2),
    ('system:user:export:sensitive', '导出敏感字段',   NULL, NULL, 44, 3)
) AS v(code, name, api, method, sort_order, risk)
ON CONFLICT (permission_code) DO UPDATE SET
    parent_id = EXCLUDED.parent_id,
    permission_name = EXCLUDED.permission_name,
    permission_type = EXCLUDED.permission_type,
    api_path = EXCLUDED.api_path,
    http_method = EXCLUDED.http_method,
    sort_order = EXCLUDED.sort_order,
    visible = TRUE,
    status = 1,
    tenant_id = EXCLUDED.tenant_id,
    permission_scope = EXCLUDED.permission_scope,
    risk_level = EXCLUDED.risk_level,
    update_time = NOW(),
    deleted = FALSE;

INSERT INTO sys_role_permission (id, role_id, permission_id, tenant_id, create_time)
SELECT uuid_generate_v5(uuid_ns_url(), 'scm:rp:' || r.role_code || ':' || p.permission_code),
       r.id, p.id, r.tenant_id, NOW()
FROM sys_role r
JOIN sys_permission p
  ON p.tenant_id = '019a0aee-0000-7000-8000-000000000002'
 AND p.status = 1
 AND NOT p.deleted
 AND p.permission_code IN (
     'system:user:import',
     'system:user:import:template',
     'system:user:export',
     'system:user:export:sensitive'
 )
WHERE r.tenant_id = '019a0aee-0000-7000-8000-000000000002'
  AND r.role_code IN ('ROLE_SUPER_ADMIN', 'ROLE_TENANT_ADMIN')
ON CONFLICT (role_id, permission_id) DO NOTHING;

COMMIT;

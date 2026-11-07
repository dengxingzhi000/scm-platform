-- ======================================================================
-- 用户管理企业级增强 — 批次 2（批量运维）权限登记
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
    ('system:user:batch:enable',   '批量启用用户', '/api/system/users/batch/enable', 'POST', 21, 3),
    ('system:user:batch:disable',  '批量禁用用户', '/api/system/users/batch/disable', 'POST', 22, 3),
    ('system:user:batch:delete',   '批量删除用户', '/api/system/users/batch/delete', 'POST', 23, 4),
    ('system:user:batch:transfer', '批量调整部门', '/api/system/users/batch/transfer-dept', 'POST', 24, 3),
    ('system:user:batch:grant',    '批量授予角色', '/api/system/users/batch/grant-roles', 'POST', 25, 4)
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
     'system:user:batch:enable',
     'system:user:batch:disable',
     'system:user:batch:delete',
     'system:user:batch:transfer',
     'system:user:batch:grant'
 )
WHERE r.tenant_id = '019a0aee-0000-7000-8000-000000000002'
  AND r.role_code IN ('ROLE_SUPER_ADMIN', 'ROLE_TENANT_ADMIN')
ON CONFLICT (role_id, permission_id) DO NOTHING;

COMMIT;

-- ======================================================================
-- 用户管理企业级增强 — 批次 1（P0 安全与正确性）权限登记
-- 目标库：db_permission
--
-- 目的：
--   1. 为 GET /api/system/users/{id}、统计、临时角色、锁定、强制下线等端点
--      登记 api_path + http_method（ApiAccessControlFilter 默认 DENY）。
--   2. 修正 002 中 edit/delete/reset/grant 的 NULL 路径，并新增服务层二次校验码。
--
-- 幂等：固定 UUID v5 + ON CONFLICT 更新；角色绑定 ON CONFLICT DO NOTHING。
-- 依赖：002_rbac_permission.sql（menu:system:user 菜单与默认租户角色）
-- ======================================================================

BEGIN;

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ----------------------------------------------------------------------
-- 1. 修正既有权限的 URL 登记（002 重跑也会写回正确值）
-- ----------------------------------------------------------------------
UPDATE sys_permission SET
    api_path = '/api/system/users/{id}', http_method = 'PUT',
    risk_level = 3, update_time = NOW(), deleted = FALSE
WHERE permission_code = 'system:user:edit';

UPDATE sys_permission SET
    api_path = '/api/system/users/{id}', http_method = 'DELETE',
    risk_level = 4, update_time = NOW(), deleted = FALSE
WHERE permission_code = 'system:user:delete';

UPDATE sys_permission SET
    api_path = '/api/system/users/{id}/reset-password', http_method = 'POST',
    risk_level = 4, update_time = NOW(), deleted = FALSE
WHERE permission_code = 'system:user:reset';

UPDATE sys_permission SET
    api_path = '/api/system/users/{id}/grant-roles', http_method = 'POST',
    risk_level = 4, update_time = NOW(), deleted = FALSE
WHERE permission_code = 'system:user:grant';

-- ----------------------------------------------------------------------
-- 2. 新增权限
-- ----------------------------------------------------------------------
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
    ('system:user:detail',                  '用户详情',           '/api/system/users/{id}', 'GET',  7, 1),
    ('system:user:view:sensitive',          '查看用户敏感字段',   NULL, NULL, 8, 2),
    ('system:user:reset:reveal',            '重置密码显示明文',   NULL, NULL, 9, 4),
    ('system:user:statistics',              '用户统计',           '/api/system/users/{id}/statistics', 'GET', 10, 1),
    ('system:user:temporary-role:list',     '临时角色查询',       '/api/system/users/{id}/temporary-roles', 'GET', 11, 1),
    ('system:user:temporary-role:grant',    '授予临时角色',       '/api/system/users/{id}/grant-temporary-roles', 'POST', 12, 4),
    ('system:user:temporary-role:extend',   '延长临时角色',       '/api/system/users/{userId}/extend-temporary-role/{roleId}', 'POST', 13, 3),
    ('system:user:temporary-role:terminate','终止临时角色',       '/api/system/users/{userId}/terminate-temporary-role/{roleId}', 'POST', 14, 3),
    ('system:user:lock',                    '锁定/解锁用户',      '/api/system/users/{id}/lock', 'POST', 15, 3),
    ('system:user:force-logout',            '强制用户下线',       '/api/system/users/{id}/force-logout', 'POST', 16, 3)
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

-- ----------------------------------------------------------------------
-- 3. 角色绑定：超级管理员/租户管理员获得全部用户管理权限
-- ----------------------------------------------------------------------
INSERT INTO sys_role_permission (id, role_id, permission_id, tenant_id, create_time)
SELECT uuid_generate_v5(uuid_ns_url(), 'scm:rp:' || r.role_code || ':' || p.permission_code),
       r.id, p.id, r.tenant_id, NOW()
FROM sys_role r
JOIN sys_permission p
  ON p.tenant_id = '019a0aee-0000-7000-8000-000000000002'
 AND p.status = 1
 AND NOT p.deleted
 AND p.permission_code IN (
     'system:user:detail',
     'system:user:view:sensitive',
     'system:user:reset:reveal',
     'system:user:statistics',
     'system:user:temporary-role:list',
     'system:user:temporary-role:grant',
     'system:user:temporary-role:extend',
     'system:user:temporary-role:terminate',
     'system:user:lock',
     'system:user:force-logout'
 )
WHERE r.tenant_id = '019a0aee-0000-7000-8000-000000000002'
  AND r.role_code IN ('ROLE_SUPER_ADMIN', 'ROLE_TENANT_ADMIN')
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- 部门主管保留“查看用户列表 + 查看详情”的最小可见范围
INSERT INTO sys_role_permission (id, role_id, permission_id, tenant_id, create_time)
SELECT uuid_generate_v5(uuid_ns_url(), 'scm:rp:' || r.role_code || ':' || p.permission_code),
       r.id, p.id, r.tenant_id, NOW()
FROM sys_role r
JOIN sys_permission p
  ON p.tenant_id = '019a0aee-0000-7000-8000-000000000002'
 AND p.status = 1
 AND NOT p.deleted
 AND p.permission_code = 'system:user:detail'
WHERE r.tenant_id = '019a0aee-0000-7000-8000-000000000002'
  AND r.role_code = 'ROLE_DEPARTMENT_MANAGER'
ON CONFLICT (role_id, permission_id) DO NOTHING;

COMMIT;

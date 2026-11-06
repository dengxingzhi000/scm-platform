-- ======================================================================
-- 财务批次 6 权限补充：坏账核销 — db_permission
--
-- 目的：为 /bad-debt-write-off 端点登记 URL 权限（ApiAccessControlFilter
-- 按 api_path + http_method 匹配，未登记端点默认 DENY）。
--
-- 坏账核销走独立权限码 finance:bad-debt:*，与对账差异的 write-off 权限
-- （finance:reconciliation-diff:write-off）互不替代。
--
-- 幂等：固定 UUID v5 + ON CONFLICT；角色绑定 ON CONFLICT DO NOTHING。
-- 依赖：002_rbac_permission.sql（menu:finance 菜单与默认租户角色）
-- ======================================================================

BEGIN;

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ----------------------------------------------------------------------
-- 1. API 权限
-- ----------------------------------------------------------------------
INSERT INTO sys_permission (
    id, parent_id, permission_code, permission_name, permission_type,
    api_path, http_method, sort_order, visible, status,
    tenant_id, permission_scope, permission_level, risk_level,
    need_approval, need_two_factor, create_time, update_time, deleted
)
SELECT uuid_generate_v5(uuid_ns_url(), 'scm:perm:' || v.code),
       (SELECT p.id FROM sys_permission p WHERE p.permission_code = v.menu LIMIT 1),
       v.code, v.name, 3, v.api, v.method, v.sort_order,
       TRUE, 1,
       '019a0aee-0000-7000-8000-000000000002', 'TENANT', 1, v.risk,
       FALSE, FALSE, NOW(), NOW(), FALSE
FROM (VALUES
    ('finance:bad-debt:apply', '坏账核销-提交申请', 'menu:finance:settlement',
     '/bad-debt-write-off', 'POST', 140, 3),
    ('finance:bad-debt:approve', '坏账核销-审批通过', 'menu:finance:settlement',
     '/bad-debt-write-off/{id}/approve', 'POST', 141, 4),
    ('finance:bad-debt:reject', '坏账核销-驳回', 'menu:finance:settlement',
     '/bad-debt-write-off/{id}/reject', 'POST', 142, 4),
    ('finance:bad-debt:write-off', '坏账核销-核销生效', 'menu:finance:settlement',
     '/bad-debt-write-off/{id}/write-off', 'POST', 143, 4),
    ('finance:bad-debt:page', '坏账核销-分页查询', 'menu:finance:settlement',
     '/bad-debt-write-off/page', 'GET', 144, 1),
    ('finance:bad-debt:view', '坏账核销-查看', 'menu:finance:settlement',
     '/bad-debt-write-off/{id}', 'GET', 145, 1)
) AS v(code, name, menu, api, method, sort_order, risk)
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
-- 2. 角色绑定：超级管理员与租户管理员
-- ----------------------------------------------------------------------
INSERT INTO sys_role_permission (id, role_id, permission_id, tenant_id, create_time)
SELECT uuid_generate_v5(uuid_ns_url(), 'scm:rp:' || r.role_code || ':' || p.permission_code),
       r.id, p.id, r.tenant_id, NOW()
FROM sys_role r
JOIN sys_permission p
  ON p.tenant_id = '019a0aee-0000-7000-8000-000000000002'
 AND p.status = 1
 AND NOT p.deleted
 AND p.permission_code LIKE 'finance:bad-debt:%'
WHERE r.tenant_id = '019a0aee-0000-7000-8000-000000000002'
  AND r.role_code IN ('ROLE_SUPER_ADMIN', 'ROLE_TENANT_ADMIN')
ON CONFLICT (role_id, permission_id) DO NOTHING;

COMMIT;

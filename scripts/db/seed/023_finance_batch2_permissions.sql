-- ======================================================================
-- 财务批次 2 权限补充 — db_permission
--
-- 目的：为新端点登记 URL 权限（ApiAccessControlFilter 按 api_path +
-- http_method 匹配，未登记端点默认 DENY）。
--
--   * 账期开/关/查
--   * 对账差异 发现/确认/驳回/核销/查
--   * 资金流水 登记
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
    ('finance:accounting-period:open', '账期-开账期', 'menu:finance:settlement',
     '/accounting-period', 'POST', 120, 3),
    ('finance:accounting-period:close', '账期-关账期', 'menu:finance:settlement',
     '/accounting-period/{id}/close', 'POST', 121, 3),
    ('finance:accounting-period:page', '账期-分页查询', 'menu:finance:settlement',
     '/accounting-period/page', 'GET', 122, 1),
    ('finance:accounting-period:view', '账期-查看', 'menu:finance:settlement',
     '/accounting-period/{id}', 'GET', 123, 1),
    ('finance:accounting-period:current', '账期-当前账期', 'menu:finance:settlement',
     '/accounting-period/current', 'GET', 124, 1),
    ('finance:accounting-period:closed', '账期-已关账期', 'menu:finance:settlement',
     '/accounting-period/closed', 'GET', 125, 1),
    ('finance:reconciliation-diff:create', '对账差异-登记', 'menu:finance:reconciliation',
     '/reconciliation-diff', 'POST', 120, 3),
    ('finance:reconciliation-diff:confirm', '对账差异-确认', 'menu:finance:reconciliation',
     '/reconciliation-diff/{id}/confirm', 'POST', 121, 3),
    ('finance:reconciliation-diff:reject', '对账差异-驳回', 'menu:finance:reconciliation',
     '/reconciliation-diff/{id}/reject', 'POST', 122, 3),
    ('finance:reconciliation-diff:write-off', '对账差异-核销', 'menu:finance:reconciliation',
     '/reconciliation-diff/{id}/write-off', 'POST', 123, 4),
    ('finance:reconciliation-diff:page', '对账差异-分页查询', 'menu:finance:reconciliation',
     '/reconciliation-diff/page', 'GET', 124, 1),
    ('finance:reconciliation-diff:view', '对账差异-查看', 'menu:finance:reconciliation',
     '/reconciliation-diff/{id}', 'GET', 125, 1),
    ('finance:fund-flow:create', '资金流水-登记', 'menu:finance:settlement',
     '/fund-flow', 'POST', 126, 4),
    ('finance:reconciliation:reject', '对账-驳回', 'menu:finance:reconciliation',
     '/reconciliation-record/{id}/reject', 'POST', 109, 3)
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
 AND (p.permission_code LIKE 'finance:accounting-period:%'
      OR p.permission_code LIKE 'finance:reconciliation-diff:%'
      OR p.permission_code LIKE 'finance:reconciliation:%'
      OR p.permission_code = 'finance:fund-flow:create')
WHERE r.tenant_id = '019a0aee-0000-7000-8000-000000000002'
  AND r.role_code IN ('ROLE_SUPER_ADMIN', 'ROLE_TENANT_ADMIN')
ON CONFLICT (role_id, permission_id) DO NOTHING;

COMMIT;

-- ======================================================================
-- 系统配置权限补充 — db_permission
--
-- 目的：
--   为 GET/PUT /api/system/config 登记 URL 权限（ApiAccessControlFilter 按
--   api_path + http_method 匹配，未登记端点默认 DENY）。
--   - system:config:view — GET  只读，风险 1
--   - system:config:edit — PUT  可写，风险 3
--
-- 层级约束不靠权限码实现：租户管理员可调用 PUT 修改本租户覆盖，但写
-- scope=PLATFORM 时由 SysConfigServiceImpl + PlatformAdminGuard 返回 403。
--
-- 幂等：固定 UUID v5 + ON CONFLICT 更新；角色绑定 ON CONFLICT DO NOTHING。
-- 依赖：002_rbac_permission.sql（menu:system 菜单与默认租户角色）
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
       (SELECT p.id FROM sys_permission p WHERE p.permission_code = 'menu:system' LIMIT 1),
       v.code, v.name, 3, v.api, v.method, v.sort_order,
       TRUE, 1,
       '019a0aee-0000-7000-8000-000000000002', 'TENANT', 1, v.risk,
       FALSE, FALSE, NOW(), NOW(), FALSE
FROM (VALUES
    ('system:config:view', '系统配置-查看', '/api/system/config', 'GET', 21, 1),
    ('system:config:edit', '系统配置-修改', '/api/system/config', 'PUT', 22, 3)
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
-- 2. 角色绑定：超级管理员与租户管理员均可查看/修改（租户覆盖）
-- ----------------------------------------------------------------------
INSERT INTO sys_role_permission (id, role_id, permission_id, tenant_id, create_time)
SELECT uuid_generate_v5(uuid_ns_url(), 'scm:rp:' || r.role_code || ':' || p.permission_code),
       r.id, p.id, r.tenant_id, NOW()
FROM sys_role r
JOIN sys_permission p
  ON p.tenant_id = '019a0aee-0000-7000-8000-000000000002'
 AND p.status = 1
 AND NOT p.deleted
 AND p.permission_code IN ('system:config:view', 'system:config:edit')
WHERE r.tenant_id = '019a0aee-0000-7000-8000-000000000002'
  AND r.role_code IN ('ROLE_SUPER_ADMIN', 'ROLE_TENANT_ADMIN')
ON CONFLICT (role_id, permission_id) DO NOTHING;

COMMIT;

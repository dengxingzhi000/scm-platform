-- ======================================================================
-- 023 — 库存权限码补齐（批次 4）
-- 目标库：db_permission
-- 依赖：002_rbac_permission.sql、019_api_permission_coverage.sql
--
-- 命名说明（重要）：
--   编码沿用仓库既有 `inventory:{模块}:{kebab-动作}` 风格。
--   需求里给出的示例码与仓库已落地的码对应关系如下（本脚本不重复造码）：
--     需求示例                        仓库已落地 / 本脚本新增
--     inventory:stock:view       -> inventory:inventory:get-inventory（已存在）
--     inventory:stock:adjust     -> inventory:inventory:adjust-inventory（已存在）
--     inventory:transfer:edit    -> inventory:inventory:transfer-inventory（已存在）
--     inventory:alert:scan       -> inventory:alert:scan（已存在，本脚本做幂等确保）
--     inventory:reservation:release -> 本脚本新增
--     预占列表/详情、流水列表（批次 4 前端缺口）：
--       inventory:reservation:view（POST /reservations/query）
--       inventory:reservation:get （GET  /reservations/{businessKey}）
--       inventory:log:view        （POST /logs/query）
--
--   ⚠ 以上编码（含新增的 inventory:reservation:release）**待 platform/system 权限规范最终确认**，
--     本轮先按仓库现状落地，规范定稿后如改名只需再次执行本脚本并清理旧码。
--
-- 关于 release-expired：
--   /api/v1/inventory/reservation/release-expired 是内部服务间接口，
--   不登记用户权限码；它由 InternalEndpointSignatureFilter 的
--   internalSignaturePaths（/api/v1/inventory/reservation/**）强制 HMAC 签名，
--   已登录用户无签名同样 403。用例见 ReleaseExpiredAuthorizationTest。
--
-- 幂等：固定 UUID v5 + ON CONFLICT，可重复执行。
-- ======================================================================

BEGIN;

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ----------------------------------------------------------------------
-- 1. 权限定义
-- ----------------------------------------------------------------------
INSERT INTO sys_permission (
    id, parent_id, permission_code, permission_name, permission_type,
    api_path, http_method, sort_order, visible, status,
    tenant_id, permission_scope, permission_level, risk_level,
    need_approval, need_two_factor, create_time, update_time, deleted
)
SELECT uuid_generate_v5(uuid_ns_url(), 'scm:perm:' || v.code),
       (SELECT p.id FROM sys_permission p WHERE p.permission_code = v.parent LIMIT 1),
       v.code, v.name, 3, v.api, v.method, v.sort_order,
       TRUE, 1,
       '019a0aee-0000-7000-8000-000000000002', 'TENANT', 1, v.risk,
       FALSE, FALSE, NOW(), NOW(), FALSE
FROM (VALUES
    ('inventory:reservation:release', '库存预占-释放', 'menu:inventory:list',
     '/api/v1/inventory/reservation/release/{businessKey}', 'DELETE', 120, 3),
    ('inventory:reservation:view', '库存预占-列表', 'menu:inventory:list',
     '/api/v1/inventory/reservations/query', 'POST', 121, 1),
    ('inventory:reservation:get', '库存预占-详情', 'menu:inventory:list',
     '/api/v1/inventory/reservations/{businessKey}', 'GET', 122, 1),
    ('inventory:log:view', '库存流水-列表', 'menu:inventory:list',
     '/api/v1/inventory/logs/query', 'POST', 123, 1),
    ('inventory:alert:scan', '库存预警-扫描', 'menu:inventory:alerts',
     '/api/v1/inventory/alerts/scan', 'POST', 102, 3)
) AS v(code, name, parent, api, method, sort_order, risk)
ON CONFLICT (permission_code) DO UPDATE SET
    parent_id = EXCLUDED.parent_id,
    permission_name = EXCLUDED.permission_name,
    permission_type = 3,
    api_path = EXCLUDED.api_path,
    http_method = EXCLUDED.http_method,
    sort_order = EXCLUDED.sort_order,
    risk_level = EXCLUDED.risk_level,
    visible = TRUE,
    status = 1,
    tenant_id = EXCLUDED.tenant_id,
    permission_scope = EXCLUDED.permission_scope,
    update_time = NOW(),
    deleted = FALSE;

-- ----------------------------------------------------------------------
-- 2. 授予默认角色（仅本脚本涉及的两个码，不扩大授权面）
-- ----------------------------------------------------------------------
INSERT INTO sys_role_permission (id, role_id, permission_id, tenant_id, create_time)
SELECT uuid_generate_v5(uuid_ns_url(), 'scm:rp:' || r.role_code || ':' || p.permission_code),
       r.id, p.id, r.tenant_id, NOW()
FROM sys_role r
JOIN sys_permission p
  ON p.tenant_id = '019a0aee-0000-7000-8000-000000000002'
 AND p.status = 1
 AND NOT p.deleted
WHERE r.tenant_id = '019a0aee-0000-7000-8000-000000000002'
  AND r.role_code IN ('ROLE_SUPER_ADMIN', 'ROLE_TENANT_ADMIN')
  AND p.permission_code IN ('inventory:reservation:release',
                            'inventory:reservation:view',
                            'inventory:reservation:get',
                            'inventory:log:view',
                            'inventory:alert:scan')
ON CONFLICT (role_id, permission_id) DO NOTHING;

COMMIT;

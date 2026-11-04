-- ======================================================================
-- 搜索索引管理权限（双身份端点：权限码 or 服务间签名）
-- 目标库：db_permission
-- 依赖：002_rbac_permission.sql、020_api_permission_coverage.sql
-- 版本：021
--
-- 背景：
--   /api/v1/search/index/** 既有 search-console 页面从浏览器直呼，
--   也有服务间 reindex 调用。因此不在豁免清单里，改为：
--   - 有效服务间 HMAC 签名 → 放行；
--   - 或登录用户持有 search:index:manage → 放行；
--   - 两者都无 → 403（见 InternalEndpointSignatureFilter）。
--
-- 授予：仅 ROLE_SUPER_ADMIN。
--   复核结论：products 索引是单索引共享（@Document(indexName="products")），
--   ProductDocument 没有 tenantId 字段，reindexAll 读整个索引再回写、
--   deleteProduct 只按 productId 删除，查询侧（searchProducts/similar/personalized）
--   也不带租户过滤 —— 该索引没有租户维度，reindex/删除会作用于全量（跨租户）。
--   因此不能授予租户管理员；租户维度改造列为 P0 风险（见 PR 说明）。
-- 幂等：固定 UUID v5 + ON CONFLICT；角色绑定 DO NOTHING。可重复执行。
-- ======================================================================

BEGIN;

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

INSERT INTO sys_permission (
    id, parent_id, permission_code, permission_name, permission_type,
    api_path, http_method, sort_order, visible, status,
    tenant_id, permission_scope, permission_level, risk_level,
    need_approval, need_two_factor, create_time, update_time, deleted
)
VALUES (
    uuid_generate_v5(uuid_ns_url(), 'scm:perm:search:index:manage'),
    (SELECT p.id FROM sys_permission p WHERE p.permission_code = 'menu:search' LIMIT 1),
    'search:index:manage', '搜索索引-管理', 3,
    '/api/v1/search/index/**', '*', 101, TRUE, 1,
    '019a0aee-0000-7000-8000-000000000002', 'TENANT', 1, 3,
    FALSE, FALSE, NOW(), NOW(), FALSE
)
ON CONFLICT (permission_code) DO UPDATE SET
    permission_name = EXCLUDED.permission_name,
    api_path = EXCLUDED.api_path,
    http_method = EXCLUDED.http_method,
    risk_level = EXCLUDED.risk_level,
    visible = TRUE,
    status = 1,
    tenant_id = EXCLUDED.tenant_id,
    update_time = NOW(),
    deleted = FALSE;

INSERT INTO sys_role_permission (id, role_id, permission_id, tenant_id, create_time)
SELECT uuid_generate_v5(uuid_ns_url(), 'scm:rp:' || r.role_code || ':search:index:manage'),
       r.id, p.id, r.tenant_id, NOW()
FROM sys_role r
JOIN sys_permission p
  ON p.permission_code = 'search:index:manage'
 AND p.status = 1 AND NOT p.deleted
WHERE r.tenant_id = '019a0aee-0000-7000-8000-000000000002'
  AND r.role_code IN ('ROLE_SUPER_ADMIN')
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- 收回曾授予租户管理员的绑定（幂等）：索引没有租户维度，reindex 会跨租户生效。
DELETE FROM sys_role_permission rp
USING sys_role r, sys_permission p
WHERE rp.role_id = r.id
  AND rp.permission_id = p.id
  AND p.permission_code = 'search:index:manage'
  AND r.role_code = 'ROLE_TENANT_ADMIN';

COMMIT;

-- 回读：SELECT permission_code, api_path, http_method FROM sys_permission
--        WHERE permission_code = 'search:index:manage';
-- ======================================================================

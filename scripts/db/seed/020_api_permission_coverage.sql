-- ======================================================================
-- API 权限登记补齐（第 2 批：业务端点 24 条）
-- 目标库：db_permission
-- 依赖：002_rbac_permission.sql、019_api_permission_coverage.sql
-- 版本：020（seed 目录，按 019 之后顺延）
--
-- 范围：
--   019 落地后仍未登记的 88 条 handler 中，属于“业务端点、应靠权限码”的 24 条
--   （权限覆盖矩阵扫描结果，按模块拆分）：
--     document 12、file 7、mall sellers 2、order cancel 1、order payments/refunds 2。
--
--   其余未登记 handler 不在本脚本内，按既定口径处理：
--   - 内部 16 条（审计写入、库存预占 6、通知审计 2、搜索索引 5、支付回调 2）
--     → PR-C 补服务间签名/白名单后进豁免清单；
--   - 自助 40 条（购物车、收货地址、积分、用户通知、优惠券/拼团、个性化推荐、本人支付单）
--     → PR-C 走“当前登录人 == 目标 userId/cartItemId”归属校验，不发权限码；
--   - 公开只读（会员注册、商品搜索/相似商品）；
--   - auth 登出/健康等 → PR-C 豁免清单；
--   - ecom payment 的创建/退款 → 待业务方确认归属（同 scm/order 资金端点口径）。
--
--   待业务方确认：scm/order 的 POST /api/v1/payments、POST /api/v1/refunds
--   本轮按“内部端点”登记并要求服务间签名（见 PR-C）；
--   若确认是前端直呼，需要改走前端权限码而不是内部签名。
--
-- 幂等：
--   permission_code 全局唯一，使用固定 UUID v5 + ON CONFLICT 更新；
--   角色绑定使用 ON CONFLICT (role_id, permission_id) DO NOTHING。可重复执行。
--
-- 执行：
--   psql -h localhost -U admin -d db_permission -v ON_ERROR_STOP=1 \
--        -f scripts/db/seed/020_api_permission_coverage.sql
-- ======================================================================

BEGIN;

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ----------------------------------------------------------------------
-- 1. 业务端点权限（24 条）
--    risk_level：1 只读、3 写、4 删除/资金
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
    -- document（12）
    ('document:document:view', '文档-查看', 'menu:document', '/api/v1/documents/{id}', 'GET', 106, 1),
    ('document:document:list-audits', '文档-审计记录', 'menu:document', '/api/v1/documents/{id}/audits', 'GET', 107, 1),
    ('document:document:list-versions', '文档-版本列表', 'menu:document', '/api/v1/documents/{id}/versions', 'GET', 108, 1),
    ('document:document:page', '文档-分页', 'menu:document', '/api/v1/documents/page', 'GET', 109, 1),
    ('document:file:view', '文档附件-查看', 'menu:document', '/api/v1/document-files/{id}', 'GET', 110, 1),
    ('document:file:url', '文档附件-下载地址', 'menu:document', '/api/v1/document-files/{id}/url', 'GET', 111, 1),
    ('document:file:page', '文档附件-分页', 'menu:document', '/api/v1/document-files/page', 'GET', 112, 1),
    ('document:template:view', '文档模板-查看', 'menu:document', '/api/v1/templates/{id}', 'GET', 113, 1),
    ('document:template:list-versions', '文档模板-版本列表', 'menu:document', '/api/v1/templates/{id}/versions', 'GET', 114, 1),
    ('document:template:get-schema', '文档模板-读取Schema', 'menu:document', '/api/v1/templates/{templateId}/schema', 'GET', 115, 1),
    ('document:template:page', '文档模板-分页', 'menu:document', '/api/v1/templates/page', 'GET', 116, 1),
    ('document:template:render', '文档模板-渲染', 'menu:document', '/api/v1/templates/render', 'POST', 117, 3),
    -- file（7）
    ('file:file:delete', '文件-删除', 'menu:file', '/api/v1/files/{id}', 'DELETE', 104, 4),
    ('file:file:update', '文件-修改', 'menu:file', '/api/v1/files/{id}', 'PUT', 105, 3),
    ('file:file:download', '文件-下载内容', 'menu:file', '/api/v1/files/{id}/content', 'GET', 106, 1),
    ('file:file:preview', '文件-预览', 'menu:file', '/api/v1/files/{id}/preview', 'GET', 107, 1),
    ('file:file:url', '文件-访问地址', 'menu:file', '/api/v1/files/{id}/url', 'GET', 108, 1),
    ('file:file:check', '文件-秒传校验', 'menu:file', '/api/v1/files/check/{md5}', 'GET', 109, 1),
    ('file:file:upload', '文件-上传', 'menu:file', '/api/v1/files/upload', 'POST', 110, 3),
    -- mall sellers（2）
    ('mall:seller:create', '商城-入驻申请', 'menu:mall', '/api/v1/mall/sellers', 'POST', 101, 3),
    ('mall:seller:view', '商城-店铺详情', 'menu:mall', '/api/v1/mall/sellers/{sellerId}', 'GET', 102, 1),
    -- order（3）
    ('order:order:cancel', '订单-取消', 'menu:order:list', '/api/v1/orders/{id}/cancel', 'PUT', 106, 3),
    ('order:payment:create', '订单支付-创建', 'menu:order:payment', '/api/v1/payments', 'POST', 105, 3),
    ('order:refund:create', '订单退款-创建', 'menu:order:refund', '/api/v1/refunds', 'POST', 108, 4)
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
-- 2. 角色绑定（与 019 口径一致）
--    SUPER_ADMIN：全部新增权限
--    TENANT_ADMIN：不含 tenant:*（本脚本 24 条均不带该前缀，实际全量绑定）
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
  AND r.role_code = 'ROLE_SUPER_ADMIN'
  AND p.permission_code ~ '^(document|file|mall|order):'
ON CONFLICT (role_id, permission_id) DO NOTHING;

INSERT INTO sys_role_permission (id, role_id, permission_id, tenant_id, create_time)
SELECT uuid_generate_v5(uuid_ns_url(), 'scm:rp:' || r.role_code || ':' || p.permission_code),
       r.id, p.id, r.tenant_id, NOW()
FROM sys_role r
JOIN sys_permission p
  ON p.tenant_id = '019a0aee-0000-7000-8000-000000000002'
 AND p.status = 1
 AND NOT p.deleted
WHERE r.tenant_id = '019a0aee-0000-7000-8000-000000000002'
  AND r.role_code = 'ROLE_TENANT_ADMIN'
  AND p.permission_code ~ '^(document|file|mall|order):'
  AND p.permission_code NOT LIKE 'tenant:%'
ON CONFLICT (role_id, permission_id) DO NOTHING;

COMMIT;

-- ======================================================================
-- 回读（执行后可手工核对）：
--   SELECT count(*) FROM sys_permission
--    WHERE permission_code ~ '^(document|file|mall):|^order:(order:cancel|payment:create|refund:create)'
--      AND status = 1 AND NOT deleted;              -- 期望 24
--
-- 覆盖矩阵复扫口径：88 条未登记中 24 条变为已登记；
--   剩余 64 条按上面的分类由 PR-C（豁免/签名/归属校验）与 PR-D（report-only 清单）处理。
-- 注意：scm/order 的 /api/v1/payments、/api/v1/refunds 虽已登记，
--   PR-C 仍会要求服务间签名（待业务方确认是否前端直呼）。
-- ======================================================================

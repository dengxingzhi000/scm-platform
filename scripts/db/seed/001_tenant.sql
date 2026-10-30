-- ======================================================================
-- 本地开发种子数据 001 — 默认租户
-- 目标库：db_tenant
--
-- 固定租户 ID 与 scm-web/.env.local 的 NEXT_PUBLIC_DEFAULT_TENANT_ID 保持一致。
-- 幂等：重复执行只更新名称/状态，不会产生重复租户。
-- ======================================================================
BEGIN;

INSERT INTO tenant (
    id, tenant_code, tenant_name, tenant_name_en, tenant_type,
    company_name, contact_name, contact_phone, contact_email,
    status, activated_at, create_time, update_time, deleted, remark
) VALUES (
    '019a0aee-0000-7000-8000-000000000002',
    'default',
    '默认租户',
    'Default Tenant',
    1,
    'SCM 演示有限公司',
    '张管理员',
    '13800000000',
    'admin@scm.local',
    1,
    NOW(),
    NOW(),
    NOW(),
    FALSE,
    '本地开发种子数据'
)
ON CONFLICT (id) DO UPDATE SET
    tenant_name = EXCLUDED.tenant_name,
    contact_name = EXCLUDED.contact_name,
    contact_phone = EXCLUDED.contact_phone,
    contact_email = EXCLUDED.contact_email,
    status = EXCLUDED.status,
    update_time = NOW(),
    deleted = FALSE;

COMMIT;

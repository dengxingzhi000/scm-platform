-- ======================================================================
-- 本地开发种子数据 003 — 组织结构
-- 目标库：db_org
--
-- 固定部门 ID 与 004_admin_user.sql 中的 admin 所属部门保持一致。
-- 幂等：固定 UUID + dept_code 唯一约束冲突更新。
-- ======================================================================
BEGIN;

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 总部
INSERT INTO sys_dept (
    id, tenant_id, parent_id, dept_code, dept_name, dept_type,
    dept_level, dept_path, isolation_level, sort_order, status,
    create_time, update_time, deleted
) VALUES (
    uuid_generate_v5(uuid_ns_url(), 'scm:dept:HQ'),
    '019a0aee-0000-7000-8000-000000000002',
    NULL, 'HQ', '总部', 1, 1, '/HQ', 1, 0, 1,
    NOW(), NOW(), FALSE
)
ON CONFLICT (dept_code) DO UPDATE SET
    dept_name = EXCLUDED.dept_name,
    dept_type = EXCLUDED.dept_type,
    dept_level = EXCLUDED.dept_level,
    dept_path = EXCLUDED.dept_path,
    isolation_level = EXCLUDED.isolation_level,
    sort_order = EXCLUDED.sort_order,
    status = EXCLUDED.status,
    update_time = NOW(),
    deleted = FALSE;

-- 二级部门
INSERT INTO sys_dept (
    id, tenant_id, parent_id, dept_code, dept_name, dept_type,
    dept_level, dept_path, isolation_level, sort_order, status,
    create_time, update_time, deleted, phone, email
)
SELECT uuid_generate_v5(uuid_ns_url(), 'scm:dept:' || v.code),
       '019a0aee-0000-7000-8000-000000000002',
       (SELECT p.id FROM sys_dept p WHERE p.dept_code = 'HQ'),
       v.code, v.name, 2, 2, '/HQ/' || v.code, 1, v.sort, 1,
       NOW(), NOW(), FALSE, v.phone, v.email
FROM (VALUES
    ('PURCHASE',  '采购部',   1, '13800000101', 'purchase@scm.local'),
    ('WAREHOUSE', '仓储部',   2, '13800000102', 'warehouse@scm.local'),
    ('LOGISTICS', '物流部',   3, '13800000103', 'logistics@scm.local'),
    ('FINANCE',   '财务部',   4, '13800000104', 'finance@scm.local'),
    ('IT',        '信息中心', 5, '13800000105', 'it@scm.local')
) AS v(code, name, sort, phone, email)
ON CONFLICT (dept_code) DO UPDATE SET
    parent_id = EXCLUDED.parent_id,
    dept_name = EXCLUDED.dept_name,
    dept_type = EXCLUDED.dept_type,
    dept_level = EXCLUDED.dept_level,
    dept_path = EXCLUDED.dept_path,
    isolation_level = EXCLUDED.isolation_level,
    sort_order = EXCLUDED.sort_order,
    status = EXCLUDED.status,
    phone = EXCLUDED.phone,
    email = EXCLUDED.email,
    update_time = NOW(),
    deleted = FALSE;

COMMIT;

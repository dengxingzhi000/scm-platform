-- ======================================================================
-- 本地开发种子数据 002 — 系统管理 RBAC 权限体系
-- 目标库：db_permission
--
-- 内容：
--   1. 租户级角色（超级管理员 / 租户管理员 / 部门主管 / 业务专员）
--   2. 全平台菜单权限树 + 系统管理按钮/接口权限
--   3. 服务层使用的兼容权限编码（user:* / role:* / dept:* / permission:*）
--   4. 角色-权限绑定
--   5. admin 用户绑定超级管理员角色
--   6. USER 账号状态字典与状态流转（锁定 / 解锁 / 禁用 / 启用）
--
-- 幂等：权限/角色/状态使用固定 UUID（uuid_generate_v5）与 ON CONFLICT 更新；
-- 角色-权限绑定采用先删后插；可重复执行。
-- ======================================================================
BEGIN;

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ----------------------------------------------------------------------
-- 1. 角色
-- ----------------------------------------------------------------------
INSERT INTO sys_role (
    id, tenant_id, role_code, role_name, role_desc,
    role_level, data_scope, role_type, role_category,
    status, sort_order, create_time, update_time, deleted
) VALUES
    (uuid_generate_v5(uuid_ns_url(), 'scm:role:super-admin'),
     '019a0aee-0000-7000-8000-000000000002', 'ROLE_SUPER_ADMIN', '超级管理员',
     '租户内最高权限，拥有全部菜单与操作权限', 9, 1, 'TENANT_ROLE', 'FUNCTIONAL', 1, 10, NOW(), NOW(), FALSE),
    (uuid_generate_v5(uuid_ns_url(), 'scm:role:tenant-admin'),
     '019a0aee-0000-7000-8000-000000000002', 'ROLE_TENANT_ADMIN', '租户管理员',
     '负责租户内的组织、用户、角色与业务配置', 7, 1, 'TENANT_ROLE', 'FUNCTIONAL', 1, 20, NOW(), NOW(), FALSE),
    (uuid_generate_v5(uuid_ns_url(), 'scm:role:dept-manager'),
     '019a0aee-0000-7000-8000-000000000002', 'ROLE_DEPARTMENT_MANAGER', '部门主管',
     '管理本部门及下级部门的业务数据', 4, 3, 'TENANT_ROLE', 'BUSINESS', 1, 30, NOW(), NOW(), FALSE),
    (uuid_generate_v5(uuid_ns_url(), 'scm:role:operator'),
     '019a0aee-0000-7000-8000-000000000002', 'ROLE_OPERATOR', '业务专员',
     '仅可查看和操作本人负责的业务数据', 2, 4, 'TENANT_ROLE', 'BUSINESS', 1, 40, NOW(), NOW(), FALSE)
ON CONFLICT (tenant_id, role_code) DO UPDATE SET
    role_name = EXCLUDED.role_name,
    role_desc = EXCLUDED.role_desc,
    role_level = EXCLUDED.role_level,
    data_scope = EXCLUDED.data_scope,
    role_type = EXCLUDED.role_type,
    role_category = EXCLUDED.role_category,
    status = EXCLUDED.status,
    sort_order = EXCLUDED.sort_order,
    update_time = NOW(),
    deleted = FALSE;

-- ----------------------------------------------------------------------
-- 2. 菜单权限（type 1=目录, 2=菜单）
-- ----------------------------------------------------------------------
INSERT INTO sys_permission (
    id, parent_id, permission_code, permission_name, permission_type,
    route_path, icon, sort_order, visible, status,
    tenant_id, permission_scope, permission_level, risk_level,
    need_approval, need_two_factor, create_time, update_time, deleted
)
SELECT uuid_generate_v5(uuid_ns_url(), 'scm:perm:' || v.code),
       NULL, v.code, v.name, v.ptype, v.route, v.icon, v.sort,
       TRUE, 1,
       '019a0aee-0000-7000-8000-000000000002', 'TENANT', 1, 1,
       FALSE, FALSE, NOW(), NOW(), FALSE
FROM (VALUES
    ('menu:dashboard',            '仪表盘',      2, '/dashboard',                  'dashboard',      10),
    ('menu:analytics',            '数据分析',    2, '/analytics',                  'bar-chart',      20),

    ('menu:product',              '商品管理',    1, '/product',                    'shop',           30),
    ('menu:product:list',         '商品列表',    2, '/product',                    NULL,              1),
    ('menu:product:category',     '商品分类',    2, '/product/category',           NULL,              2),
    ('menu:product:brand',        '品牌管理',    2, '/product/brand',              NULL,              3),
    ('menu:product:sku',          'SKU 管理',    2, '/product/sku',                NULL,              4),
    ('menu:product:attribute',    '属性模板',    2, '/product/attribute-template', NULL,              5),

    ('menu:order',                '订单管理',    1, '/order',                      'shopping-cart',  40),
    ('menu:order:list',           '订单列表',    2, '/order',                      NULL,              1),
    ('menu:order:refund',         '退款管理',    2, '/order/refund',               NULL,              2),
    ('menu:order:payment',        '支付管理',    2, '/order/payment',              NULL,              3),

    ('menu:inventory',            '库存管理',    1, '/inventory',                  'inbox',          50),
    ('menu:inventory:list',       '库存列表',    2, '/inventory',                  NULL,              1),
    ('menu:inventory:alerts',     '库存预警',    2, '/inventory/alerts',           NULL,              2),

    ('menu:warehouse',            '仓库管理',    1, '/warehouse',                  'apartment',      60),
    ('menu:warehouse:list',       '仓库列表',    2, '/warehouse',                  NULL,              1),
    ('menu:warehouse:inbound',    '入库管理',    2, '/warehouse/inbound',          NULL,              2),
    ('menu:warehouse:outbound',   '出库管理',    2, '/warehouse/outbound',         NULL,              3),
    ('menu:warehouse:wave',       '拣货波次',    2, '/warehouse/wave-picking',     NULL,              4),

    ('menu:purchase',             '采购管理',    1, '/purchase',                   'file-text',      70),
    ('menu:purchase:list',        '采购订单',    2, '/purchase',                   NULL,              1),
    ('menu:purchase:rfq',         '询价管理',    2, '/purchase/rfq',               NULL,              2),
    ('menu:purchase:quotation',   '报价管理',    2, '/purchase/quotation',         NULL,              3),

    ('menu:supplier',             '供应商管理',  2, '/supplier',                   'team',           80),

    ('menu:logistics',            '物流管理',    1, '/logistics',                  'car',            90),
    ('menu:logistics:list',       '运单管理',    2, '/logistics',                  NULL,              1),
    ('menu:logistics:tracking',   '物流跟踪',    2, '/logistics/tracking',         NULL,              2),
    ('menu:logistics:carrier',    '承运商管理',  2, '/logistics/carrier',          NULL,              3),

    ('menu:finance',              '财务管理',    1, '/finance',                    'book',          100),
    ('menu:finance:settlement',   '结算管理',    2, '/finance/settlement',         NULL,              1),
    ('menu:finance:invoice',      '发票管理',    2, '/finance/invoice',            NULL,              2),
    ('menu:finance:reconciliation','对账管理',   2, '/finance/reconciliation',     NULL,              3),

    ('menu:document',             '文档中心',    2, '/document',                   'folder-open',   110),
    ('menu:file',                 '文件中心',    2, '/file',                       'cloud-upload',  120),

    ('menu:tenant',               '租户管理',    1, '/tenant',                     'global',        130),
    ('menu:tenant:list',          '租户列表',    2, '/tenant',                     NULL,              1),
    ('menu:tenant:package',       '套餐管理',    2, '/tenant/package',             NULL,              2),

    ('menu:system',               '系统管理',    1, '/system',                     'setting',       140),
    ('menu:system:user',          '用户管理',    2, '/system/user',                'user',            1),
    ('menu:system:role',          '角色管理',    2, '/system/role',                'safety',          2),
    ('menu:system:permission',    '权限管理',    2, '/system/permission',          NULL,              3),
    ('menu:system:dept',          '部门管理',    2, '/system/dept',                NULL,              4),
    ('menu:system:dictionary',    '字典管理',    2, '/system/dictionary',          NULL,              5),
    ('menu:system:status',        '状态字典',    2, '/system/status',              NULL,              6),
    ('menu:system:decision',      '决策矩阵',    2, '/system/decision',            NULL,              7),

    ('menu:notification',         '通知管理',    2, '/notification',               'notification',  150),
    ('menu:approval',             '审批中心',    2, '/approval',                   'audit',         160),
    ('menu:audit',                '审计日志',    2, '/audit',                      'safety-certificate', 170)
) AS v(code, name, ptype, route, icon, sort)
ON CONFLICT (permission_code) DO UPDATE SET
    permission_name = EXCLUDED.permission_name,
    permission_type = EXCLUDED.permission_type,
    route_path = EXCLUDED.route_path,
    icon = EXCLUDED.icon,
    sort_order = EXCLUDED.sort_order,
    visible = TRUE,
    status = 1,
    tenant_id = EXCLUDED.tenant_id,
    permission_scope = EXCLUDED.permission_scope,
    update_time = NOW(),
    deleted = FALSE;

-- 菜单父子关系
WITH mapping(child_code, parent_code) AS (VALUES
    ('menu:product:list', 'menu:product'),
    ('menu:product:category', 'menu:product'),
    ('menu:product:brand', 'menu:product'),
    ('menu:product:sku', 'menu:product'),
    ('menu:product:attribute', 'menu:product'),
    ('menu:order:list', 'menu:order'),
    ('menu:order:refund', 'menu:order'),
    ('menu:order:payment', 'menu:order'),
    ('menu:inventory:list', 'menu:inventory'),
    ('menu:inventory:alerts', 'menu:inventory'),
    ('menu:warehouse:list', 'menu:warehouse'),
    ('menu:warehouse:inbound', 'menu:warehouse'),
    ('menu:warehouse:outbound', 'menu:warehouse'),
    ('menu:warehouse:wave', 'menu:warehouse'),
    ('menu:purchase:list', 'menu:purchase'),
    ('menu:purchase:rfq', 'menu:purchase'),
    ('menu:purchase:quotation', 'menu:purchase'),
    ('menu:logistics:list', 'menu:logistics'),
    ('menu:logistics:tracking', 'menu:logistics'),
    ('menu:logistics:carrier', 'menu:logistics'),
    ('menu:finance:settlement', 'menu:finance'),
    ('menu:finance:invoice', 'menu:finance'),
    ('menu:finance:reconciliation', 'menu:finance'),
    ('menu:tenant:list', 'menu:tenant'),
    ('menu:tenant:package', 'menu:tenant'),
    ('menu:system:user', 'menu:system'),
    ('menu:system:role', 'menu:system'),
    ('menu:system:permission', 'menu:system'),
    ('menu:system:dept', 'menu:system'),
    ('menu:system:dictionary', 'menu:system'),
    ('menu:system:status', 'menu:system'),
    ('menu:system:decision', 'menu:system')
)
UPDATE sys_permission c
SET parent_id = p.id, update_time = NOW()
FROM mapping m
JOIN sys_permission p ON p.permission_code = m.parent_code
WHERE c.permission_code = m.child_code;

-- ----------------------------------------------------------------------
-- 3. 操作权限（type 3=按钮/接口）
--    api_path/http_method 用于 ApiAccessControlFilter 的 URL 精确匹配，
--    permission_code 同时是 @PreAuthorize 与 PermissionChecker 使用的权限标识。
-- ----------------------------------------------------------------------
INSERT INTO sys_permission (
    id, parent_id, permission_code, permission_name, permission_type,
    api_path, http_method, sort_order, visible, status,
    tenant_id, permission_scope, permission_level, risk_level,
    need_approval, need_two_factor, create_time, update_time, deleted
)
SELECT uuid_generate_v5(uuid_ns_url(), 'scm:perm:' || v.code),
       (SELECT p.id FROM sys_permission p WHERE p.permission_code = v.parent),
       v.code, v.name, 3, v.api, v.method, v.sort,
       TRUE, 1,
       '019a0aee-0000-7000-8000-000000000002', 'TENANT', 1,
       v.risk, FALSE, v.twofa, NOW(), NOW(), FALSE
FROM (VALUES
    -- 用户管理
    ('system:user:list',   '查看用户',   'menu:system:user', '/api/system/users', 'GET',  1, 1, FALSE),
    ('system:user:add',    '新增用户',   'menu:system:user', '/api/system/users', 'POST', 2, 3, FALSE),
    ('system:user:edit',   '编辑用户',   'menu:system:user', NULL, NULL, 3, 3, FALSE),
    ('system:user:delete', '删除用户',   'menu:system:user', NULL, NULL, 4, 4, FALSE),
    ('system:user:reset',  '重置密码',   'menu:system:user', NULL, NULL, 5, 3, FALSE),
    ('system:user:grant',  '分配角色',   'menu:system:user', NULL, NULL, 6, 4, FALSE),
    ('user:add',           '用户新增(服务层)', 'menu:system:user', NULL, NULL, 91, 3, FALSE),
    ('user:update',        '用户修改(服务层)', 'menu:system:user', NULL, NULL, 92, 3, FALSE),
    ('user:delete',        '用户删除(服务层)', 'menu:system:user', NULL, NULL, 93, 4, FALSE),
    ('user:grant-role',    '用户授权(服务层)', 'menu:system:user', NULL, NULL, 94, 4, FALSE),

    -- 角色管理
    ('system:role:list',   '查看角色',   'menu:system:role', '/api/system/roles', 'GET',  1, 1, FALSE),
    ('system:role:add',    '新增角色',   'menu:system:role', '/api/system/roles', 'POST', 2, 3, FALSE),
    ('system:role:edit',   '编辑角色',   'menu:system:role', NULL, NULL, 3, 3, FALSE),
    ('system:role:delete', '删除角色',   'menu:system:role', NULL, NULL, 4, 4, FALSE),
    ('role:add',           '角色新增(服务层)', 'menu:system:role', NULL, NULL, 91, 3, FALSE),
    ('role:update',        '角色修改(服务层)', 'menu:system:role', NULL, NULL, 92, 3, FALSE),
    ('role:delete',        '角色删除(服务层)', 'menu:system:role', NULL, NULL, 93, 4, FALSE),
    ('role:grant-permission', '角色授权(服务层)', 'menu:system:role', NULL, NULL, 94, 4, FALSE),

    -- 部门管理
    ('system:dept:list',   '查看部门',   'menu:system:dept', '/api/system/depts/tree', 'GET', 1, 1, FALSE),
    ('system:dept:add',    '新增部门',   'menu:system:dept', '/api/system/depts', 'POST', 2, 2, FALSE),
    ('system:dept:edit',   '编辑部门',   'menu:system:dept', NULL, NULL, 3, 2, FALSE),
    ('system:dept:delete', '删除部门',   'menu:system:dept', NULL, NULL, 4, 3, FALSE),
    ('dept:add',           '部门新增(服务层)', 'menu:system:dept', NULL, NULL, 91, 2, FALSE),
    ('dept:update',        '部门修改(服务层)', 'menu:system:dept', NULL, NULL, 92, 2, FALSE),
    ('dept:delete',        '部门删除(服务层)', 'menu:system:dept', NULL, NULL, 93, 3, FALSE),

    -- 字典管理
    ('system:dict:list',   '查看字典',   'menu:system:dictionary', '/api/system/dicts/types', 'GET', 1, 1, FALSE),
    ('system:dict:add',    '新增字典',   'menu:system:dictionary', '/api/system/dicts/types', 'POST', 2, 2, FALSE),
    ('system:dict:edit',   '编辑字典',   'menu:system:dictionary', NULL, NULL, 3, 2, FALSE),
    ('system:dict:delete', '删除字典',   'menu:system:dictionary', NULL, NULL, 4, 3, FALSE),

    -- 权限管理
    ('system:permission:list',   '查看权限', 'menu:system:permission', '/api/system/permissions/tree', 'GET', 1, 1, FALSE),
    ('system:permission:add',    '新增权限', 'menu:system:permission', '/api/system/permissions', 'POST', 2, 4, FALSE),
    ('system:permission:edit',   '编辑权限', 'menu:system:permission', NULL, NULL, 3, 4, FALSE),
    ('system:permission:delete', '删除权限', 'menu:system:permission', NULL, NULL, 4, 4, FALSE),
    ('permission:add',           '权限新增(服务层)', 'menu:system:permission', NULL, NULL, 91, 4, FALSE),
    ('permission:update',        '权限修改(服务层)', 'menu:system:permission', NULL, NULL, 92, 4, FALSE),
    ('permission:delete',        '权限删除(服务层)', 'menu:system:permission', NULL, NULL, 93, 4, FALSE),

    -- 状态字典
    ('system:status:add',    '新增状态', 'menu:system:status', '/api/system/status/dict', 'POST', 1, 2, FALSE),
    ('system:status:edit',   '编辑状态', 'menu:system:status', NULL, NULL, 2, 2, FALSE),
    ('system:status:delete', '删除状态', 'menu:system:status', NULL, NULL, 3, 3, FALSE)
) AS v(code, name, parent, api, method, sort, risk, twofa)
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
-- 4. 角色-权限绑定（先清理再重建，保证与角色定义一致）
-- ----------------------------------------------------------------------
DELETE FROM sys_role_permission
WHERE role_id IN (
    SELECT id FROM sys_role
    WHERE tenant_id = '019a0aee-0000-7000-8000-000000000002'
      AND role_code IN ('ROLE_SUPER_ADMIN', 'ROLE_TENANT_ADMIN', 'ROLE_DEPARTMENT_MANAGER', 'ROLE_OPERATOR')
);

-- 超级管理员 / 租户管理员：全部权限
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
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- 部门主管：全部菜单 + 用户/部门查看
INSERT INTO sys_role_permission (id, role_id, permission_id, tenant_id, create_time)
SELECT uuid_generate_v5(uuid_ns_url(), 'scm:rp:' || r.role_code || ':' || p.permission_code),
       r.id, p.id, r.tenant_id, NOW()
FROM sys_role r
JOIN sys_permission p
  ON p.tenant_id = '019a0aee-0000-7000-8000-000000000002'
 AND p.status = 1
 AND NOT p.deleted
WHERE r.tenant_id = '019a0aee-0000-7000-8000-000000000002'
  AND r.role_code = 'ROLE_DEPARTMENT_MANAGER'
  AND (p.permission_code LIKE 'menu:%'
       OR p.permission_code IN ('system:user:list', 'system:dept:list'))
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- 业务专员：业务菜单（不含系统管理与租户管理）
INSERT INTO sys_role_permission (id, role_id, permission_id, tenant_id, create_time)
SELECT uuid_generate_v5(uuid_ns_url(), 'scm:rp:' || r.role_code || ':' || p.permission_code),
       r.id, p.id, r.tenant_id, NOW()
FROM sys_role r
JOIN sys_permission p
  ON p.tenant_id = '019a0aee-0000-7000-8000-000000000002'
 AND p.status = 1
 AND NOT p.deleted
WHERE r.tenant_id = '019a0aee-0000-7000-8000-000000000002'
  AND r.role_code = 'ROLE_OPERATOR'
  AND p.permission_code LIKE 'menu:%'
  AND p.permission_code NOT LIKE 'menu:system%'
  AND p.permission_code NOT LIKE 'menu:tenant%'
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- ----------------------------------------------------------------------
-- 5. admin 绑定超级管理员
-- ----------------------------------------------------------------------
INSERT INTO sys_user_role (
    id, user_id, role_id, approval_status, tenant_id,
    create_time, username, real_name, user_status
)
SELECT uuid_generate_v5(uuid_ns_url(), 'scm:user-role:admin:super-admin'),
       '019a0aee-0000-7000-8000-000000000001',
       r.id, 2, r.tenant_id, NOW(), 'admin', '管理员', 1
FROM sys_role r
WHERE r.tenant_id = '019a0aee-0000-7000-8000-000000000002'
  AND r.role_code = 'ROLE_SUPER_ADMIN'
ON CONFLICT (user_id, role_id) DO UPDATE SET
    approval_status = 2,
    effective_time = NULL,
    expire_time = NULL,
    tenant_id = EXCLUDED.tenant_id,
    username = EXCLUDED.username,
    real_name = EXCLUDED.real_name,
    user_status = EXCLUDED.user_status;

-- ----------------------------------------------------------------------
-- 6. USER 账号状态字典与流转规则
--    lockUser / unlockUser 依赖 StatusMachineDubboService 校验这些流转。
-- ----------------------------------------------------------------------
INSERT INTO sys_status_dict (
    id, tenant_id, biz_type, status_code, status_name, status_name_en,
    color, icon, sort_order, is_initial, is_terminal, is_cancellable,
    enabled, create_time, update_time, deleted, remark
) VALUES
    ('sd-user-01', '019a0aee-0000-7000-8000-000000000002', 'USER', 'ACTIVE',
     '启用', 'Active', '#52c41a', 'check-circle', 1, TRUE, FALSE, FALSE, TRUE, NOW(), NOW(), FALSE, NULL),
    ('sd-user-02', '019a0aee-0000-7000-8000-000000000002', 'USER', 'INACTIVE',
     '禁用', 'Disabled', '#8c8c8c', 'stop', 2, FALSE, FALSE, FALSE, TRUE, NOW(), NOW(), FALSE, NULL),
    ('sd-user-03', '019a0aee-0000-7000-8000-000000000002', 'USER', 'LOCKED',
     '锁定', 'Locked', '#faad14', 'lock', 3, FALSE, FALSE, FALSE, TRUE, NOW(), NOW(), FALSE, NULL)
ON CONFLICT (id) DO UPDATE SET
    status_name = EXCLUDED.status_name,
    status_name_en = EXCLUDED.status_name_en,
    color = EXCLUDED.color,
    icon = EXCLUDED.icon,
    sort_order = EXCLUDED.sort_order,
    is_initial = EXCLUDED.is_initial,
    is_terminal = EXCLUDED.is_terminal,
    is_cancellable = EXCLUDED.is_cancellable,
    enabled = TRUE,
    update_time = NOW(),
    deleted = FALSE;

INSERT INTO sys_status_transition (
    id, tenant_id, biz_type, from_status, to_status, action_code, action_name,
    action_name_en, need_approval, enabled, sort_order, create_time, update_time, deleted
) VALUES
    ('st-user-lock',     '019a0aee-0000-7000-8000-000000000002', 'USER', 'ACTIVE',   'LOCKED',   'LOCK',    '锁定账号', 'Lock',   FALSE, TRUE, 1, NOW(), NOW(), FALSE),
    ('st-user-unlock',   '019a0aee-0000-7000-8000-000000000002', 'USER', 'LOCKED',   'ACTIVE',   'UNLOCK',  '解锁账号', 'Unlock', FALSE, TRUE, 2, NOW(), NOW(), FALSE),
    ('st-user-disable',  '019a0aee-0000-7000-8000-000000000002', 'USER', 'ACTIVE',   'INACTIVE', 'DISABLE', '禁用账号', 'Disable', FALSE, TRUE, 3, NOW(), NOW(), FALSE),
    ('st-user-enable',   '019a0aee-0000-7000-8000-000000000002', 'USER', 'INACTIVE', 'ACTIVE',   'ENABLE',  '启用账号', 'Enable', FALSE, TRUE, 4, NOW(), NOW(), FALSE),
    ('st-user-lock-off', '019a0aee-0000-7000-8000-000000000002', 'USER', 'LOCKED',   'INACTIVE', 'DISABLE', '禁用账号', 'Disable', FALSE, TRUE, 5, NOW(), NOW(), FALSE)
ON CONFLICT (id) DO UPDATE SET
    action_name = EXCLUDED.action_name,
    action_name_en = EXCLUDED.action_name_en,
    enabled = TRUE,
    update_time = NOW(),
    deleted = FALSE;

COMMIT;

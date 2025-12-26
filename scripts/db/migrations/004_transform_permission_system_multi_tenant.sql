-- =====================================================
-- 权限系统多租户改造迁移脚本
-- =====================================================
-- 功能：将现有的 sys_user, sys_role, sys_permission 改造为支持多租户的RBAC系统
-- 版本：v1.0
-- 作者：Claude Code
-- 日期：2025-01-24
-- =====================================================

BEGIN;

-- =====================================================
-- 1. 用户表（sys_user）多租户改造
-- =====================================================

-- 1.1 添加 tenant_id 字段
ALTER TABLE IF EXISTS sys_user
    ADD COLUMN IF NOT EXISTS tenant_id UUID;

-- 1.2 添加用户类型字段（区分平台管理员和租户用户）
ALTER TABLE IF EXISTS sys_user
    ADD COLUMN IF NOT EXISTS user_type VARCHAR(20) DEFAULT 'TENANT_USER' CHECK (user_type IN ('PLATFORM_ADMIN', 'TENANT_ADMIN', 'TENANT_USER'));

COMMENT ON COLUMN sys_user.user_type IS '用户类型：PLATFORM_ADMIN=平台管理员, TENANT_ADMIN=租户管理员, TENANT_USER=租户普通用户';

-- 1.3 添加所属部门字段（租户内部组织架构）
ALTER TABLE IF EXISTS sys_user
    ADD COLUMN IF NOT EXISTS department_id UUID;

COMMENT ON COLUMN sys_user.department_id IS '所属部门ID（租户内部组织架构）';

-- 1.4 添加数据权限范围字段
ALTER TABLE IF EXISTS sys_user
    ADD COLUMN IF NOT EXISTS data_scope VARCHAR(20) DEFAULT 'SELF' CHECK (data_scope IN ('ALL', 'DEPT', 'DEPT_AND_SUB', 'SELF', 'CUSTOM'));

COMMENT ON COLUMN sys_user.data_scope IS '数据权限范围：ALL=全部, DEPT=本部门, DEPT_AND_SUB=本部门及下级, SELF=仅本人, CUSTOM=自定义';

-- 1.5 重建索引（添加 tenant_id）
DROP INDEX IF EXISTS idx_user_username;
CREATE UNIQUE INDEX idx_user_username ON sys_user(tenant_id, username) WHERE NOT deleted;

DROP INDEX IF EXISTS idx_user_email;
CREATE INDEX idx_user_email ON sys_user(tenant_id, email) WHERE NOT deleted AND email IS NOT NULL;

DROP INDEX IF EXISTS idx_user_phone;
CREATE INDEX idx_user_phone ON sys_user(tenant_id, phone) WHERE NOT deleted AND phone IS NOT NULL;

-- 1.6 添加租户+用户名唯一约束
ALTER TABLE sys_user DROP CONSTRAINT IF EXISTS sys_user_username_key;
ALTER TABLE sys_user ADD CONSTRAINT uk_tenant_username UNIQUE (tenant_id, username);

-- 1.7 添加外键约束
ALTER TABLE sys_user
    ADD CONSTRAINT fk_user_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE CASCADE;

-- 1.8 为平台管理员设置 tenant_id = NULL（平台级用户不属于任何租户）
-- 注意：需要手动标识哪些是平台管理员
-- UPDATE sys_user SET tenant_id = NULL, user_type = 'PLATFORM_ADMIN' WHERE username = 'admin';


-- =====================================================
-- 2. 角色表（sys_role）多租户改造
-- =====================================================

-- 2.1 添加 tenant_id 字段
ALTER TABLE IF EXISTS sys_role
    ADD COLUMN IF NOT EXISTS tenant_id UUID;

-- 2.2 添加角色类型字段（区分平台角色和租户角色）
ALTER TABLE IF EXISTS sys_role
    ADD COLUMN IF NOT EXISTS role_type VARCHAR(20) DEFAULT 'TENANT_ROLE' CHECK (role_type IN ('PLATFORM_ROLE', 'TENANT_ROLE'));

COMMENT ON COLUMN sys_role.role_type IS '角色类型：PLATFORM_ROLE=平台角色（跨租户）, TENANT_ROLE=租户角色（租户内）';

-- 2.3 添加角色分类字段
ALTER TABLE IF EXISTS sys_role
    ADD COLUMN IF NOT EXISTS role_category VARCHAR(50) DEFAULT 'BUSINESS';

COMMENT ON COLUMN sys_role.role_category IS '角色分类：BUSINESS=业务角色, FUNCTIONAL=职能角色, CUSTOM=自定义角色';

-- 2.4 添加数据权限范围字段（角色级别的数据权限）
ALTER TABLE IF EXISTS sys_role
    ADD COLUMN IF NOT EXISTS data_scope VARCHAR(20) DEFAULT 'SELF' CHECK (data_scope IN ('ALL', 'DEPT', 'DEPT_AND_SUB', 'SELF', 'CUSTOM'));

-- 2.5 添加自定义数据权限部门列表（JSONB数组）
ALTER TABLE IF EXISTS sys_role
    ADD COLUMN IF NOT EXISTS custom_dept_ids JSONB DEFAULT '[]';

COMMENT ON COLUMN sys_role.custom_dept_ids IS '自定义数据权限部门ID列表（当 data_scope=CUSTOM 时使用）';

-- 2.6 重建索引（添加 tenant_id）
DROP INDEX IF EXISTS idx_role_code;
CREATE UNIQUE INDEX idx_role_code ON sys_role(tenant_id, role_code) WHERE NOT deleted;

-- 2.7 添加租户+角色编码唯一约束
ALTER TABLE sys_role DROP CONSTRAINT IF EXISTS sys_role_role_code_key;
ALTER TABLE sys_role ADD CONSTRAINT uk_tenant_role_code UNIQUE (tenant_id, role_code);

-- 2.8 添加外键约束
ALTER TABLE sys_role
    ADD CONSTRAINT fk_role_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE CASCADE;


-- =====================================================
-- 3. 权限表（sys_permission）多租户改造
-- =====================================================

-- 3.1 添加 tenant_id 字段（NULL表示平台级权限）
ALTER TABLE IF EXISTS sys_permission
    ADD COLUMN IF NOT EXISTS tenant_id UUID;

COMMENT ON COLUMN sys_permission.tenant_id IS '租户ID（NULL=平台级权限，可被所有租户使用；非NULL=租户自定义权限）';

-- 3.2 添加权限类型字段
ALTER TABLE IF EXISTS sys_permission
    ADD COLUMN IF NOT EXISTS permission_type VARCHAR(20) DEFAULT 'MENU' CHECK (permission_type IN ('MENU', 'BUTTON', 'API', 'DATA'));

COMMENT ON COLUMN sys_permission.permission_type IS '权限类型：MENU=菜单, BUTTON=按钮, API=接口, DATA=数据权限';

-- 3.3 添加权限归属字段
ALTER TABLE IF EXISTS sys_permission
    ADD COLUMN IF NOT EXISTS permission_scope VARCHAR(20) DEFAULT 'PLATFORM' CHECK (permission_scope IN ('PLATFORM', 'TENANT'));

COMMENT ON COLUMN sys_permission.permission_scope IS '权限归属：PLATFORM=平台级权限（所有租户共享）, TENANT=租户级权限（租户自定义）';

-- 3.4 添加API路径和HTTP方法字段（用于API权限）
ALTER TABLE IF EXISTS sys_permission
    ADD COLUMN IF NOT EXISTS api_path VARCHAR(500),
    ADD COLUMN IF NOT EXISTS http_method VARCHAR(10) CHECK (http_method IN ('GET', 'POST', 'PUT', 'DELETE', 'PATCH', '*'));

COMMENT ON COLUMN sys_permission.api_path IS 'API路径（支持Ant风格通配符，如 /api/orders/**）';
COMMENT ON COLUMN sys_permission.http_method IS 'HTTP方法（*表示所有方法）';

-- 3.5 重建索引
DROP INDEX IF EXISTS idx_permission_code;
CREATE UNIQUE INDEX idx_permission_code ON sys_permission(COALESCE(tenant_id, '00000000-0000-0000-0000-000000000000'::UUID), permission_code) WHERE NOT deleted;

-- 3.6 添加外键约束（允许NULL，表示平台级权限）
ALTER TABLE sys_permission
    ADD CONSTRAINT fk_permission_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE CASCADE;


-- =====================================================
-- 4. 用户-角色关联表（sys_user_role）多租户改造
-- =====================================================

-- 4.1 添加 tenant_id 字段（冗余字段，用于查询优化）
ALTER TABLE IF EXISTS sys_user_role
    ADD COLUMN IF NOT EXISTS tenant_id UUID;

-- 4.2 重建索引
DROP INDEX IF EXISTS idx_user_role_user;
CREATE INDEX idx_user_role_user ON sys_user_role(tenant_id, user_id);

DROP INDEX IF EXISTS idx_user_role_role;
CREATE INDEX idx_user_role_role ON sys_user_role(tenant_id, role_id);

-- 4.3 修改唯一约束
ALTER TABLE sys_user_role DROP CONSTRAINT IF EXISTS sys_user_role_user_id_role_id_key;
ALTER TABLE sys_user_role ADD CONSTRAINT uk_tenant_user_role UNIQUE (tenant_id, user_id, role_id);


-- =====================================================
-- 5. 角色-权限关联表（sys_role_permission）多租户改造
-- =====================================================

-- 5.1 添加 tenant_id 字段（冗余字段）
ALTER TABLE IF EXISTS sys_role_permission
    ADD COLUMN IF NOT EXISTS tenant_id UUID;

-- 5.2 重建索引
DROP INDEX IF EXISTS idx_role_permission_role;
CREATE INDEX idx_role_permission_role ON sys_role_permission(tenant_id, role_id);

DROP INDEX IF EXISTS idx_role_permission_permission;
CREATE INDEX idx_role_permission_permission ON sys_role_permission(permission_id);

-- 5.3 修改唯一约束
ALTER TABLE sys_role_permission DROP CONSTRAINT IF EXISTS sys_role_permission_role_id_permission_id_key;
ALTER TABLE sys_role_permission ADD CONSTRAINT uk_tenant_role_permission UNIQUE (tenant_id, role_id, permission_id);


-- =====================================================
-- 6. 新增：部门表（sys_department）- 租户内部组织架构
-- =====================================================

CREATE TABLE IF NOT EXISTS sys_department (
    -- 主键
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- 租户ID
    tenant_id UUID NOT NULL,

    -- 部门信息
    dept_code VARCHAR(50) NOT NULL,
    dept_name VARCHAR(100) NOT NULL,
    parent_id UUID,                          -- 父部门ID（NULL表示根部门）
    dept_level INT DEFAULT 1,                -- 部门层级（1=一级部门）
    dept_path VARCHAR(500),                  -- 部门路径（如 /1/2/3）
    sort_order INT DEFAULT 0,                -- 排序号

    -- 部门负责人
    leader_id UUID,                          -- 部门负责人用户ID
    contact_phone VARCHAR(20),

    -- 状态
    status VARCHAR(20) DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE')),

    -- 审计字段
    create_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    create_by UUID,
    update_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    update_by UUID,
    deleted BOOLEAN DEFAULT FALSE,

    -- 约束
    CONSTRAINT fk_dept_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE CASCADE,
    CONSTRAINT fk_dept_parent FOREIGN KEY (parent_id) REFERENCES sys_department(id) ON DELETE CASCADE,
    CONSTRAINT fk_dept_leader FOREIGN KEY (leader_id) REFERENCES sys_user(id) ON DELETE SET NULL,
    CONSTRAINT uk_tenant_dept_code UNIQUE (tenant_id, dept_code)
);

-- 索引
CREATE INDEX idx_dept_tenant ON sys_department(tenant_id) WHERE NOT deleted;
CREATE INDEX idx_dept_parent ON sys_department(parent_id) WHERE NOT deleted;
CREATE INDEX idx_dept_path ON sys_department USING btree(dept_path) WHERE NOT deleted;

-- 注释
COMMENT ON TABLE sys_department IS '部门表（租户内部组织架构）';
COMMENT ON COLUMN sys_department.dept_path IS '部门路径（用于快速查询上下级部门）';


-- =====================================================
-- 7. 新增：数据权限规则表（sys_data_permission）
-- =====================================================

CREATE TABLE IF NOT EXISTS sys_data_permission (
    -- 主键
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- 租户ID
    tenant_id UUID NOT NULL,

    -- 规则信息
    rule_name VARCHAR(100) NOT NULL,
    rule_code VARCHAR(50) NOT NULL,
    resource_type VARCHAR(50) NOT NULL,      -- 资源类型（如 ORDER, PRODUCT, INVENTORY）
    rule_type VARCHAR(20) NOT NULL CHECK (rule_type IN ('DEPT', 'DEPT_AND_SUB', 'USER', 'CUSTOM_SQL')),

    -- 规则配置
    rule_config JSONB DEFAULT '{}',          -- 规则配置（JSON格式）
    custom_sql TEXT,                         -- 自定义SQL条件（如 user_id = #{userId}）

    -- 状态
    status VARCHAR(20) DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE')),
    priority INT DEFAULT 0,                  -- 优先级（数字越大优先级越高）

    -- 审计字段
    create_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    create_by UUID,
    update_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    update_by UUID,
    deleted BOOLEAN DEFAULT FALSE,

    -- 约束
    CONSTRAINT fk_data_perm_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE CASCADE,
    CONSTRAINT uk_tenant_rule_code UNIQUE (tenant_id, rule_code)
);

-- 索引
CREATE INDEX idx_data_perm_tenant ON sys_data_permission(tenant_id) WHERE NOT deleted;
CREATE INDEX idx_data_perm_resource ON sys_data_permission(resource_type) WHERE NOT deleted;

-- 注释
COMMENT ON TABLE sys_data_permission IS '数据权限规则表';
COMMENT ON COLUMN sys_data_permission.rule_config IS '规则配置（示例：{"deptIds": ["uuid1", "uuid2"], "userIds": ["uuid3"]}）';
COMMENT ON COLUMN sys_data_permission.custom_sql IS '自定义SQL条件（支持SpEL表达式）';


-- =====================================================
-- 8. 新增：角色-数据权限关联表
-- =====================================================

CREATE TABLE IF NOT EXISTS sys_role_data_permission (
    -- 主键
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- 租户ID
    tenant_id UUID NOT NULL,

    -- 关联
    role_id UUID NOT NULL,
    data_permission_id UUID NOT NULL,

    -- 审计字段
    create_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    create_by UUID,

    -- 约束
    CONSTRAINT fk_role_data_perm_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE CASCADE,
    CONSTRAINT fk_role_data_perm_role FOREIGN KEY (role_id) REFERENCES sys_role(id) ON DELETE CASCADE,
    CONSTRAINT fk_role_data_perm_data FOREIGN KEY (data_permission_id) REFERENCES sys_data_permission(id) ON DELETE CASCADE,
    CONSTRAINT uk_tenant_role_data_perm UNIQUE (tenant_id, role_id, data_permission_id)
);

-- 索引
CREATE INDEX idx_role_data_perm_role ON sys_role_data_permission(tenant_id, role_id);


-- =====================================================
-- 9. 创建视图：用户权限视图（优化查询性能）
-- =====================================================

CREATE OR REPLACE VIEW v_user_permissions AS
SELECT
    u.id AS user_id,
    u.tenant_id,
    u.username,
    u.user_type,
    u.department_id,
    u.data_scope AS user_data_scope,
    r.id AS role_id,
    r.role_code,
    r.role_name,
    r.data_scope AS role_data_scope,
    r.custom_dept_ids,
    p.id AS permission_id,
    p.permission_code,
    p.permission_name,
    p.permission_type,
    p.api_path,
    p.http_method
FROM sys_user u
         JOIN sys_user_role ur ON u.id = ur.user_id AND u.tenant_id = ur.tenant_id
         JOIN sys_role r ON ur.role_id = r.id AND ur.tenant_id = r.tenant_id
         JOIN sys_role_permission rp ON r.id = rp.role_id AND r.tenant_id = rp.tenant_id
         JOIN sys_permission p ON rp.permission_id = p.id
WHERE NOT u.deleted
  AND NOT r.deleted
  AND NOT p.deleted
  AND u.status = 'ACTIVE'
  AND r.status = 'ACTIVE';

COMMENT ON VIEW v_user_permissions IS '用户权限视图（用于快速查询用户拥有的所有权限）';


-- =====================================================
-- 10. 初始化平台级权限（示例）
-- =====================================================

-- 10.1 插入平台级菜单权限（所有租户共享）
INSERT INTO sys_permission (id, tenant_id, permission_code, permission_name, permission_type, permission_scope, parent_id, sort_order, deleted)
VALUES
    (gen_random_uuid(), NULL, 'MENU_DASHBOARD', '仪表盘', 'MENU', 'PLATFORM', NULL, 1, false),
    (gen_random_uuid(), NULL, 'MENU_ORDER', '订单管理', 'MENU', 'PLATFORM', NULL, 2, false),
    (gen_random_uuid(), NULL, 'MENU_PRODUCT', '商品管理', 'MENU', 'PLATFORM', NULL, 3, false),
    (gen_random_uuid(), NULL, 'MENU_INVENTORY', '库存管理', 'MENU', 'PLATFORM', NULL, 4, false),
    (gen_random_uuid(), NULL, 'MENU_WAREHOUSE', '仓库管理', 'MENU', 'PLATFORM', NULL, 5, false),
    (gen_random_uuid(), NULL, 'MENU_SUPPLIER', '供应商管理', 'MENU', 'PLATFORM', NULL, 6, false),
    (gen_random_uuid(), NULL, 'MENU_REPORT', '报表分析', 'MENU', 'PLATFORM', NULL, 7, false),
    (gen_random_uuid(), NULL, 'MENU_SYSTEM', '系统设置', 'MENU', 'PLATFORM', NULL, 8, false)
ON CONFLICT DO NOTHING;

-- 10.2 插入平台级按钮权限
INSERT INTO sys_permission (id, tenant_id, permission_code, permission_name, permission_type, permission_scope, deleted)
VALUES
    (gen_random_uuid(), NULL, 'BTN_ORDER_CREATE', '创建订单', 'BUTTON', 'PLATFORM', false),
    (gen_random_uuid(), NULL, 'BTN_ORDER_UPDATE', '编辑订单', 'BUTTON', 'PLATFORM', false),
    (gen_random_uuid(), NULL, 'BTN_ORDER_DELETE', '删除订单', 'BUTTON', 'PLATFORM', false),
    (gen_random_uuid(), NULL, 'BTN_ORDER_EXPORT', '导出订单', 'BUTTON', 'PLATFORM', false),
    (gen_random_uuid(), NULL, 'BTN_PRODUCT_CREATE', '创建商品', 'BUTTON', 'PLATFORM', false),
    (gen_random_uuid(), NULL, 'BTN_PRODUCT_UPDATE', '编辑商品', 'BUTTON', 'PLATFORM', false),
    (gen_random_uuid(), NULL, 'BTN_PRODUCT_DELETE', '删除商品', 'BUTTON', 'PLATFORM', false)
ON CONFLICT DO NOTHING;

-- 10.3 插入平台级API权限
INSERT INTO sys_permission (id, tenant_id, permission_code, permission_name, permission_type, permission_scope, api_path, http_method, deleted)
VALUES
    (gen_random_uuid(), NULL, 'API_ORDER_QUERY', '查询订单接口', 'API', 'PLATFORM', '/api/orders/**', 'GET', false),
    (gen_random_uuid(), NULL, 'API_ORDER_CREATE', '创建订单接口', 'API', 'PLATFORM', '/api/orders', 'POST', false),
    (gen_random_uuid(), NULL, 'API_ORDER_UPDATE', '更新订单接口', 'API', 'PLATFORM', '/api/orders/*', 'PUT', false),
    (gen_random_uuid(), NULL, 'API_ORDER_DELETE', '删除订单接口', 'API', 'PLATFORM', '/api/orders/*', 'DELETE', false),
    (gen_random_uuid(), NULL, 'API_PRODUCT_QUERY', '查询商品接口', 'API', 'PLATFORM', '/api/products/**', 'GET', false),
    (gen_random_uuid(), NULL, 'API_PRODUCT_CREATE', '创建商品接口', 'API', 'PLATFORM', '/api/products', 'POST', false)
ON CONFLICT DO NOTHING;


-- =====================================================
-- 11. 触发器：自动更新关联表的 tenant_id
-- =====================================================

-- 11.1 用户-角色关联表自动填充 tenant_id
CREATE OR REPLACE FUNCTION fn_sync_user_role_tenant_id()
RETURNS TRIGGER AS $$
BEGIN
    -- 从 sys_user 获取 tenant_id
    SELECT tenant_id INTO NEW.tenant_id FROM sys_user WHERE id = NEW.user_id;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_user_role_tenant_id
    BEFORE INSERT OR UPDATE ON sys_user_role
    FOR EACH ROW
EXECUTE FUNCTION fn_sync_user_role_tenant_id();

-- 11.2 角色-权限关联表自动填充 tenant_id
CREATE OR REPLACE FUNCTION fn_sync_role_permission_tenant_id()
RETURNS TRIGGER AS $$
BEGIN
    -- 从 sys_role 获取 tenant_id
    SELECT tenant_id INTO NEW.tenant_id FROM sys_role WHERE id = NEW.role_id;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_role_permission_tenant_id
    BEFORE INSERT OR UPDATE ON sys_role_permission
    FOR EACH ROW
EXECUTE FUNCTION fn_sync_role_permission_tenant_id();


-- =====================================================
-- 12. 数据迁移脚本（根据实际情况调整）
-- =====================================================

-- 12.1 为现有用户分配默认租户（需要手动调整）
-- UPDATE sys_user SET tenant_id = '<your-tenant-id>', user_type = 'TENANT_USER'
-- WHERE tenant_id IS NULL AND user_type = 'TENANT_USER';

-- 12.2 为现有角色分配租户
-- UPDATE sys_role SET tenant_id = '<your-tenant-id>', role_type = 'TENANT_ROLE'
-- WHERE tenant_id IS NULL;

-- 12.3 同步关联表的 tenant_id
-- UPDATE sys_user_role ur
-- SET tenant_id = u.tenant_id
-- FROM sys_user u
-- WHERE ur.user_id = u.id AND ur.tenant_id IS NULL;

-- UPDATE sys_role_permission rp
-- SET tenant_id = r.tenant_id
-- FROM sys_role r
-- WHERE rp.role_id = r.id AND rp.tenant_id IS NULL;

COMMIT;